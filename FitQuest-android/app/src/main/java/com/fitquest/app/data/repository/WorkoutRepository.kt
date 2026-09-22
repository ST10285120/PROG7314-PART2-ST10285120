package com.fitquest.app.data.repository

import com.fitquest.app.data.local.dao.SetEntryDao
import com.fitquest.app.data.local.dao.WorkoutDao
import com.fitquest.app.data.local.dao.WorkoutExerciseDao
import com.fitquest.app.data.local.dao.PersonalRecordRow
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class CompletionResult(
    val xpEarned: Int,
    val newLevel: Int,
    val streak: Int,
    val badgeNames: List<String>,
    val pendingSync: Boolean
)

data class WorkoutExerciseDetail(
    val exerciseName: String,
    val sets: List<SetEntryEntity>
)

data class WorkoutDetail(
    val workout: WorkoutEntity,
    val exercises: List<WorkoutExerciseDetail>
)

class WorkoutRepository(
    private val api: FitQuestApi,
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val setEntryDao: SetEntryDao
) {

    fun observeRecentCompleted(limit: Int = 10): Flow<List<WorkoutEntity>> =
        workoutDao.observeRecentCompleted(limit)

    fun observeTotalXp(): Flow<Int> = workoutDao.observeTotalXp()

    fun observePersonalRecords(): Flow<List<PersonalRecordRow>> =
        setEntryDao.observeAllPersonalRecords().map { records -> records.distinctBy { it.exerciseName } }

    fun observeExercisesForWorkout(workoutLocalId: String): Flow<List<WorkoutExerciseEntity>> =
        workoutExerciseDao.observeForWorkout(workoutLocalId)

    fun observeSetsForWorkoutExercise(weLocalId: String): Flow<List<SetEntryEntity>> =
        setEntryDao.observeForWorkoutExercise(weLocalId)

    suspend fun startWorkout(title: String): String {
        val localId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()
        workoutDao.upsert(
            WorkoutEntity(localId = localId, title = title, startedAt = startedAt, synced = false)
        )
        trySyncNewWorkout(localId, title, startedAt)
        return localId
    }

    suspend fun startWorkoutWithExerciseSelection(title: String, exercises: List<ExerciseDto>): String {
        val workoutLocalId = startWorkout(title)
        exercises.forEachIndexed { index, exercise ->
            addExercise(workoutLocalId, exercise._id, exercise.name, index)
        }
        return workoutLocalId
    }

    private suspend fun trySyncNewWorkout(localId: String, title: String, startedAt: Long) {
        try {
            val response = api.createWorkout(CreateWorkoutRequest(title = title))
            if (response.isSuccessful && response.body() != null) {
                val local = workoutDao.getByLocalId(localId)
                if (local != null) {
                    workoutDao.update(local.copy(serverId = response.body()!!.workoutId, synced = true))
                }
            }
        } catch (_: Exception) {
        }
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
        val workout = workoutDao.getByLocalId(workoutLocalId)
        if (workout?.serverId != null) {
            try {
                val res = api.addExercise(workout.serverId, AddExerciseRequest(exerciseId, orderIndex))
                if (res.isSuccessful) {
                    val we = workoutExerciseDao.getByLocalId(localId)
                    if (we != null) workoutExerciseDao.upsert(we.copy(serverId = res.body()?.workoutExerciseId))
                }
            } catch (_: Exception) { }
        }
        return localId
    }

    suspend fun logSet(
        workoutExerciseLocalId: String,
        exerciseId: String,
        setNumber: Int,
        reps: Int,
        weightKg: Double,
        rpe: Int?
    ): SetEntryEntity {
        val bestSoFar = setEntryDao.getBestWeightForExercise(exerciseId)
        val isPr = bestSoFar != null && weightKg > bestSoFar
        val entry = SetEntryEntity(
            localId = UUID.randomUUID().toString(),
            workoutExerciseLocalId = workoutExerciseLocalId,
            setNumber = setNumber,
            reps = reps,
            weightKg = weightKg,
            rpe = rpe,
            isPersonalRecord = isPr
        )
        setEntryDao.insert(entry)

        val we = workoutExerciseDao.getByLocalId(workoutExerciseLocalId)
        if (we?.serverId != null) {
            try {
                api.logSet(
                    workoutId = requireNotNull(workoutDao.getByLocalId(we.workoutLocalId)?.serverId),
                    workoutExerciseId = we.serverId,
                    request = LogSetRequest(setNumber, reps, weightKg, rpe)
                )
            } catch (_: Exception) { }
        }
        return entry
    }

    suspend fun completeWorkout(workoutLocalId: String): CompletionResult {
        val workout = requireNotNull(workoutDao.getByLocalId(workoutLocalId))
        val completedAt = System.currentTimeMillis()
        val totalVolume = setEntryDao.getTotalVolumeForWorkout(workoutLocalId) ?: 0.0
        workoutDao.update(workout.copy(completedAt = completedAt, totalVolumeKg = totalVolume))

        if (workout.serverId != null) {
            try {
                val res = api.completeWorkout(workout.serverId, CompleteWorkoutRequest())
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    workoutDao.update(
                        workout.copy(
                            completedAt = completedAt,
                            totalVolumeKg = totalVolume,
                            xpEarned = body.xpEarned,
                            synced = true
                        )
                    )
                    return CompletionResult(
                        xpEarned = body.xpEarned,
                        newLevel = body.newLevel,
                        streak = body.streak,
                        badgeNames = body.badgesEarned.map { it.name },
                        pendingSync = false
                    )
                }
            } catch (_: Exception) { }
        }

        return CompletionResult(
            xpEarned = 20,
            newLevel = 0,
            streak = 0,
            badgeNames = emptyList(),
            pendingSync = true
        )
    }

    suspend fun syncPendingWorkouts() {
        val unsynced = workoutDao.getUnsynced()
        for (workout in unsynced) {
            trySyncNewWorkout(workout.localId, workout.title, workout.startedAt)
        }
    }

    suspend fun startWorkoutWithExercises(title: String, exercises: List<SuggestedWorkoutExerciseDto>): String {
        val workoutLocalId = startWorkout(title)
        exercises.forEachIndexed { index, exercise ->
            addExercise(workoutLocalId, exercise.exerciseId, exercise.name, index)
        }
        return workoutLocalId
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

    suspend fun getSuggestedWorkout(): Result<SuggestedWorkoutResponse> = try {
        val res = api.getSuggestedWorkout()
        if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
        else Result.failure(Exception("Failed to load suggestion (HTTP ${res.code()})"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getExerciseLibrary(): Result<List<ExerciseDto>> = try {
        val res = api.listExercises()
        if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
        else Result.failure(Exception("Failed to load exercises (HTTP ${res.code()})"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getBadges(): Result<List<BadgeDto>> = try {
        val res = api.getBadges()
        if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
        else Result.failure(Exception("Failed to load badges (HTTP ${res.code()})"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
