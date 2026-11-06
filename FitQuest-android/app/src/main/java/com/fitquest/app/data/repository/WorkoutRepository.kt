package com.fitquest.app.data.repository

import android.util.Log
import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.local.dao.PersonalRecordRow
import com.fitquest.app.data.local.dao.SetEntryDao
import com.fitquest.app.data.local.dao.WorkoutDao
import com.fitquest.app.data.local.dao.WorkoutExerciseDao
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.UUID

/** What the summary screen shows. pendingSync marks the numbers as a local estimate. */
data class CompletionResult(
    val xpEarned: Int,
    val newLevel: Int,
    val streak: Int,
    val badgeNames: List<String>,
    val pendingSync: Boolean,
    val personalRecords: Int = 0,
    val leveledUp: Boolean = false
)

/** An exercise and its sets, for the workout detail screen. */
data class WorkoutExerciseDetail(
    val exerciseName: String,
    val sets: List<SetEntryEntity>
)

/** A finished workout with everything logged in it. */
data class WorkoutDetail(
    val workout: WorkoutEntity,
    val exercises: List<WorkoutExerciseDetail>
)

/** Result of pushing one workout to the API. */
private sealed class SyncOutcome {
    data class Synced(val completion: CompletionDto?) : SyncOutcome()
    data object Failed : SyncOutcome()
}

/**
 * Offline-first access to workouts.
 *
 * Every action is written to Room straight away, so logging never waits for the
 * network. The whole workout is then pushed to POST /api/workouts/sync. Because each
 * row has a UUID created on this phone, repeating a push cannot duplicate anything.
 * If a push fails, [onSyncDeferred] schedules WorkManager to retry once there is a
 * connection, even if the app has been closed.
 *
 * Reference: Google (2026) Build an offline-first app.
 * https://developer.android.com/topic/architecture/data-layer/offline-first
 */
class WorkoutRepository(
    private val api: FitQuestApi,
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val setEntryDao: SetEntryDao,
    private val localCache: LocalCache,
    private val profileRepository: ProfileRepository,
    private val onSyncDeferred: () -> Unit = {},
    private val syncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    /** One push at a time, so two rapid sets cannot race each other on the server. */
    private val syncMutex = Mutex()

    fun observeRecentCompleted(limit: Int = 50): Flow<List<WorkoutEntity>> =
        workoutDao.observeRecentCompleted(limit)

    fun observePendingXp(): Flow<Int> = workoutDao.observePendingXp()

    fun observePersonalRecords(): Flow<List<PersonalRecordRow>> =
        setEntryDao.observeAllPersonalRecords().map { records -> records.distinctBy { it.exerciseName } }

    fun observeExercisesForWorkout(workoutLocalId: String): Flow<List<WorkoutExerciseEntity>> =
        workoutExerciseDao.observeForWorkout(workoutLocalId)

    fun observeSetsForWorkoutExercise(weLocalId: String): Flow<List<SetEntryEntity>> =
        setEntryDao.observeForWorkoutExercise(weLocalId)

    fun observeActiveWorkout(): Flow<WorkoutEntity?> = workoutDao.observeActive()

    fun observeSetCount(workoutLocalId: String): Flow<Int> =
        setEntryDao.observeSetCount(workoutLocalId)

    suspend fun startWorkout(title: String): String {
        val localId = UUID.randomUUID().toString()
        workoutDao.upsert(
            WorkoutEntity(localId = localId, title = title, startedAt = System.currentTimeMillis(), synced = false)
        )
        Log.d(TAG, "Workout $localId started")
        syncInBackground(localId)
        return localId
    }

    suspend fun startWorkoutWithExercises(title: String, exercises: List<SuggestedWorkoutExerciseDto>): String {
        val workoutLocalId = startWorkout(title)
        exercises.forEachIndexed { index, exercise ->
            addExercise(workoutLocalId, exercise.exerciseId, exercise.name, index)
        }
        return workoutLocalId
    }

    suspend fun addExercise(workoutLocalId: String, exerciseId: String, exerciseName: String, orderIndex: Int): String {
        val localId = UUID.randomUUID().toString()
        workoutExerciseDao.upsert(
            WorkoutExerciseEntity(
                localId = localId,
                workoutLocalId = workoutLocalId,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                orderIndex = orderIndex
            )
        )
        workoutDao.markUnsynced(workoutLocalId)
        syncInBackground(workoutLocalId)
        return localId
    }

    /**
     * Saves a set locally. The PR flag here is an estimate from this phone's history,
     * and the server's decision replaces it on the next successful sync.
     */
    suspend fun logSet(
        workoutExerciseLocalId: String,
        exerciseId: String,
        setNumber: Int,
        reps: Int,
        weightKg: Double,
        rpe: Int?
    ): SetEntryEntity {
        val bestSoFar = setEntryDao.getBestWeightForExercise(exerciseId)
        val entry = SetEntryEntity(
            localId = UUID.randomUUID().toString(),
            workoutExerciseLocalId = workoutExerciseLocalId,
            setNumber = setNumber,
            reps = reps,
            weightKg = weightKg,
            rpe = rpe,
            isPersonalRecord = bestSoFar != null && weightKg > bestSoFar
        )
        setEntryDao.insert(entry)

        workoutExerciseDao.getByLocalId(workoutExerciseLocalId)?.let { workoutExercise ->
            workoutDao.markUnsynced(workoutExercise.workoutLocalId)
            syncInBackground(workoutExercise.workoutLocalId)
        }
        return entry
    }

    /** Removes a mistaken set. The next sync removes it from the server too. */
    suspend fun deleteSet(set: SetEntryEntity) {
        setEntryDao.delete(set.localId)
        workoutExerciseDao.getByLocalId(set.workoutExerciseLocalId)?.let { workoutExercise ->
            workoutDao.markUnsynced(workoutExercise.workoutLocalId)
            syncInBackground(workoutExercise.workoutLocalId)
        }
    }

    /** Removes an unused workout, for when the user cancels before logging a set. */
    suspend fun discardWorkout(workoutLocalId: String) {
        workoutDao.delete(workoutLocalId)
    }

    /**
     * Finishes a workout locally, then waits briefly for the server's real numbers. If
     * they do not arrive the summary shows a clearly marked estimate, which is replaced
     * after the background sync (Part 1, "Optimistic gamification").
     */
    suspend fun completeWorkout(workoutLocalId: String): CompletionResult {
        val workout = requireNotNull(workoutDao.getByLocalId(workoutLocalId))
        val previousLevel = profileRepository.profile.value?.level
        val estimatedXp = estimateXp(workoutLocalId)
        workoutDao.update(
            workout.copy(
                completedAt = System.currentTimeMillis(),
                totalVolumeKg = setEntryDao.getTotalVolumeForWorkout(workoutLocalId) ?: 0.0,
                xpEarned = estimatedXp,
                synced = false
            )
        )

        val outcome = withTimeoutOrNull(COMPLETION_WAIT_MS) { pushWorkout(workoutLocalId) }
        val completion = (outcome as? SyncOutcome.Synced)?.completion
        if (completion != null) {
            return CompletionResult(
                xpEarned = completion.xpEarned,
                newLevel = completion.newLevel,
                streak = completion.streak,
                badgeNames = completion.badgesEarned.map { it.name },
                pendingSync = false,
                personalRecords = setEntryDao.countPersonalRecords(workoutLocalId),
                leveledUp = previousLevel != null && completion.newLevel > previousLevel
            )
        }

        Log.i(TAG, "Completion for $workoutLocalId is pending sync")
        onSyncDeferred()
        val profile = profileRepository.profile.value
        return CompletionResult(
            xpEarned = estimatedXp,
            newLevel = profile?.level ?: 1,
            streak = profile?.currentStreak ?: 0,
            badgeNames = emptyList(),
            pendingSync = true,
            personalRecords = setEntryDao.countPersonalRecords(workoutLocalId)
        )
    }

    /** Uses the same formula as the API, purely for the "pending" number. */
    private suspend fun estimateXp(workoutLocalId: String): Int =
        XP_COMPLETION_BONUS +
            setEntryDao.countSets(workoutLocalId) * XP_PER_SET +
            setEntryDao.countPersonalRecords(workoutLocalId) * XP_PER_PERSONAL_RECORD

    /** Pushes every unsynced workout, oldest first. Returns true when none failed. */
    suspend fun syncPendingWorkouts(): Boolean {
        val pending = workoutDao.getUnsynced().sortedBy { it.startedAt }
        if (pending.isEmpty()) return true
        Log.i(TAG, "Syncing ${pending.size} pending workout(s)")

        var allSynced = true
        for (workout in pending) {
            if (pushWorkout(workout.localId) is SyncOutcome.Failed) allSynced = false
        }
        return allSynced
    }

    private fun syncInBackground(workoutLocalId: String) {
        syncScope.launch {
            if (pushWorkout(workoutLocalId) is SyncOutcome.Failed) onSyncDeferred()
        }
    }

    private suspend fun pushWorkout(workoutLocalId: String): SyncOutcome = syncMutex.withLock {
        val workout = workoutDao.getByLocalId(workoutLocalId) ?: return@withLock SyncOutcome.Failed
        val request = buildSyncRequest(workout)

        try {
            val response = api.syncWorkout(request)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                Log.w(TAG, "Sync of $workoutLocalId rejected with HTTP ${response.code()}")
                return@withLock SyncOutcome.Failed
            }

            body.sets.forEach { setEntryDao.updatePersonalRecord(it.clientId, it.isPersonalRecord) }

            // A set logged during the request is not on the server yet, so the row stays
            // unsynced unless what was sent still matches what is stored.
            val latest = workoutDao.getByLocalId(workoutLocalId)
            if (latest != null) {
                workoutDao.update(
                    latest.copy(
                        serverId = body.workoutId,
                        xpEarned = body.completion?.xpEarned ?: latest.xpEarned,
                        synced = buildSyncRequest(latest) == request
                    )
                )
            }
            if (body.completion != null) profileRepository.refresh()

            Log.d(TAG, "Workout $workoutLocalId synced")
            SyncOutcome.Synced(body.completion)
        } catch (e: Exception) {
            Log.d(TAG, "Sync of $workoutLocalId deferred: ${e.message}")
            SyncOutcome.Failed
        }
    }

    private suspend fun buildSyncRequest(workout: WorkoutEntity) = SyncWorkoutRequest(
        clientId = workout.localId,
        title = workout.title,
        startedAt = Instant.ofEpochMilli(workout.startedAt).toString(),
        completedAt = workout.completedAt?.let { Instant.ofEpochMilli(it).toString() },
        exercises = workoutExerciseDao.getForWorkout(workout.localId).map { exercise ->
            SyncExerciseDto(
                clientId = exercise.localId,
                exerciseId = exercise.exerciseId,
                orderIndex = exercise.orderIndex,
                sets = setEntryDao.getForWorkoutExercise(exercise.localId).map { set ->
                    SyncSetDto(set.localId, set.setNumber, set.reps, set.weightKg, set.rpe)
                }
            )
        }
    )

    /**
     * Rebuilds local history from the API, so a reinstall or new phone starts with the
     * user's workouts. Rows that already exist are skipped, as they may have unsynced edits.
     */
    suspend fun restoreHistory() {
        try {
            val response = api.getWorkoutHistory()
            val history = response.body()?.takeIf { response.isSuccessful } ?: return
            var restored = 0
            for (workout in history) {
                val localId = workout.clientId ?: workout.id
                if (workoutDao.getByLocalId(localId) != null) continue
                insertFromServer(localId, workout)
                restored++
            }
            if (restored > 0) Log.i(TAG, "Restored $restored workout(s) from the API")
        } catch (e: Exception) {
            Log.d(TAG, "History restore skipped: ${e.message}")
        }
    }

    private suspend fun insertFromServer(localId: String, workout: HistoryWorkoutDto) {
        workoutDao.upsert(
            WorkoutEntity(
                localId = localId,
                serverId = workout.id,
                title = workout.title,
                startedAt = Instant.parse(workout.startedAt).toEpochMilli(),
                completedAt = Instant.parse(workout.completedAt).toEpochMilli(),
                totalVolumeKg = workout.totalVolumeKg,
                xpEarned = workout.xpEarned,
                synced = true
            )
        )
        for (exercise in workout.exercises) {
            val exerciseLocalId = exercise.clientId ?: exercise.id
            workoutExerciseDao.upsert(
                WorkoutExerciseEntity(
                    localId = exerciseLocalId,
                    serverId = exercise.id,
                    workoutLocalId = localId,
                    exerciseId = exercise.exerciseId,
                    exerciseName = exercise.name,
                    orderIndex = exercise.orderIndex
                )
            )
            for (set in exercise.sets) {
                setEntryDao.insert(
                    SetEntryEntity(
                        localId = set.clientId ?: set.id,
                        workoutExerciseLocalId = exerciseLocalId,
                        setNumber = set.setNumber,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        rpe = set.rpe,
                        isPersonalRecord = set.isPersonalRecord
                    )
                )
            }
        }
    }

    suspend fun getWorkoutDetail(workoutLocalId: String): WorkoutDetail? {
        val workout = workoutDao.getByLocalId(workoutLocalId) ?: return null
        val exercises = workoutExerciseDao.getForWorkout(workoutLocalId).map { we ->
            WorkoutExerciseDetail(
                exerciseName = we.exerciseName,
                sets = setEntryDao.getForWorkoutExercise(we.localId)
            )
        }
        return WorkoutDetail(workout = workout, exercises = exercises)
    }

    /** Falls back to the last suggestion, so the home card still works offline. */
    suspend fun getSuggestedWorkout(): Result<SuggestedWorkoutResponse> = try {
        val res = api.getSuggestedWorkout()
        val body = res.body()
        if (res.isSuccessful && body != null) {
            localCache.saveSuggestion(body)
            Result.success(body)
        } else {
            cachedOr(localCache.getSuggestion(), "Failed to load suggestion (HTTP ${res.code()})")
        }
    } catch (e: Exception) {
        cachedOr(localCache.getSuggestion(), e.message ?: "Network error")
    }

    /** Falls back to the cached library, which is what lets a workout start offline. */
    suspend fun getExerciseLibrary(): Result<List<ExerciseDto>> = try {
        val res = api.listExercises()
        val body = res.body()
        if (res.isSuccessful && body != null) {
            localCache.saveExercises(body)
            Result.success(body)
        } else {
            cachedOr(localCache.getExercises(), "Failed to load exercises (HTTP ${res.code()})")
        }
    } catch (e: Exception) {
        cachedOr(localCache.getExercises(), e.message ?: "Network error")
    }

    private fun <T> cachedOr(cached: T?, message: String): Result<T> =
        if (cached != null) Result.success(cached) else Result.failure(Exception(message))

    suspend fun getBadgeCatalog(): Result<List<BadgeDto>> = try {
        val res = api.getBadgeCatalog()
        val body = res.body()
        if (res.isSuccessful && body != null) {
            localCache.saveBadges(body)
            Result.success(body)
        } else {
            cachedOr(localCache.getBadges(), "Failed to load badges (HTTP ${res.code()})")
        }
    } catch (e: Exception) {
        cachedOr(localCache.getBadges(), e.message ?: "Network error")
    }

    suspend fun getProgress(): Result<ProgressResponse> = try {
        val res = api.getProgress()
        val body = res.body()
        if (res.isSuccessful && body != null) {
            localCache.saveProgress(body)
            Result.success(body)
        } else {
            Result.failure(Exception("Failed to load progress (HTTP ${res.code()})"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** The last progress data seen, shown with a notice while offline. */
    fun getCachedProgress(): ProgressResponse? = localCache.getProgress()

    /** Wipes this phone after the account itself has been deleted. */
    suspend fun clearLocalData() {
        workoutDao.deleteAll()
    }

    /**
     * Clears this phone for the next account, but only if nothing would be lost. Rows
     * that could not be uploaded stay and sync after the next sign-in.
     */
    suspend fun clearLocalDataIfSynced() {
        syncPendingWorkouts()
        if (workoutDao.countUnsynced() == 0) workoutDao.deleteAll()
    }

    /** Used by the streak reminder to see if a workout has been logged. */
    suspend fun getLatestCompletedAt(): Long? = workoutDao.getLatestCompletedAt()

    private companion object {
        const val TAG = "WorkoutRepository"
        const val COMPLETION_WAIT_MS = 15_000L

        // These mirror gamificationService.js on the API, which stays authoritative.
        const val XP_COMPLETION_BONUS = 20
        const val XP_PER_SET = 5
        const val XP_PER_PERSONAL_RECORD = 25
    }
}
