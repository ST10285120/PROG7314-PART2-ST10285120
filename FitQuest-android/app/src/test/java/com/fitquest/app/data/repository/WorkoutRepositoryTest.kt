package com.fitquest.app.data.repository

import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.local.dao.SetEntryDao
import com.fitquest.app.data.local.dao.WorkoutDao
import com.fitquest.app.data.local.dao.WorkoutExerciseDao
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepositoryTest {

    private lateinit var api: FitQuestApi
    private lateinit var workoutDao: WorkoutDao
    private lateinit var workoutExerciseDao: WorkoutExerciseDao
    private lateinit var setEntryDao: SetEntryDao
    private lateinit var localCache: LocalCache
    private lateinit var profileRepository: ProfileRepository
    private lateinit var repository: WorkoutRepository
    private var deferredSyncs = 0

    private val workout = WorkoutEntity(localId = "w1", title = "Push Day", startedAt = 0L, synced = false)
    private val exercise = WorkoutExerciseEntity(
        localId = "we1", workoutLocalId = "w1", exerciseId = "ex1", exerciseName = "Bench", orderIndex = 0
    )
    private val set = SetEntryEntity(localId = "s1", workoutExerciseLocalId = "we1", setNumber = 1, reps = 8, weightKg = 60.0)

    @Before
    fun setUp() {
        api = mock()
        workoutDao = mock()
        workoutExerciseDao = mock()
        setEntryDao = mock()
        localCache = mock()
        profileRepository = mock()
        deferredSyncs = 0
        whenever(profileRepository.profile).thenReturn(MutableStateFlow(UserDto("u", "n", "e", 500, 3, 4)))
        repository = WorkoutRepository(
            api, workoutDao, workoutExerciseDao, setEntryDao, localCache, profileRepository,
            onSyncDeferred = { deferredSyncs++ },
            syncScope = CoroutineScope(UnconfinedTestDispatcher())
        )
    }

    private suspend fun givenStoredWorkout(stored: WorkoutEntity = workout) {
        whenever(workoutDao.getByLocalId("w1")).thenReturn(stored)
        whenever(workoutExerciseDao.getForWorkout("w1")).thenReturn(listOf(exercise))
        whenever(workoutExerciseDao.getByLocalId("we1")).thenReturn(exercise)
        whenever(setEntryDao.getForWorkoutExercise("we1")).thenReturn(listOf(set))
        whenever(setEntryDao.countSets("w1")).thenReturn(1)
        whenever(setEntryDao.countPersonalRecords("w1")).thenReturn(0)
    }

    private fun syncResponse(completion: CompletionDto? = null, setIsPr: Boolean = false) =
        Response.success(SyncWorkoutResponse("server-1", completion, listOf(SyncedSetDto("s1", setIsPr))))

    @Test
    fun `startWorkout saves locally and sends the local id as the clientId`() = runTest {
        whenever(api.syncWorkout(any())).thenReturn(syncResponse())
        whenever(workoutDao.getByLocalId(any())).thenAnswer { invocation ->
            WorkoutEntity(localId = invocation.getArgument(0), title = "Push Day", startedAt = 0L)
        }
        whenever(workoutExerciseDao.getForWorkout(any())).thenReturn(emptyList())

        val localId = repository.startWorkout("Push Day")

        val saved = argumentCaptor<WorkoutEntity>()
        verify(workoutDao).upsert(saved.capture())
        assertEquals(localId, saved.firstValue.localId)
        assertFalse(saved.firstValue.synced)

        val sent = argumentCaptor<SyncWorkoutRequest>()
        verify(api).syncWorkout(sent.capture())
        assertEquals(localId, sent.firstValue.clientId)
    }

    @Test
    fun `a failed sync keeps the workout unsynced and schedules a retry`() = runTest {
        whenever(api.syncWorkout(any())).thenThrow(RuntimeException("offline"))
        whenever(workoutDao.getByLocalId(any())).thenAnswer { invocation ->
            WorkoutEntity(localId = invocation.getArgument(0), title = "Push Day", startedAt = 0L)
        }
        whenever(workoutExerciseDao.getForWorkout(any())).thenReturn(emptyList())

        repository.startWorkout("Push Day")

        verify(workoutDao, never()).update(any())
        assertEquals(1, deferredSyncs)
    }

    @Test
    fun `logSet marks the workout unsynced before pushing it`() = runTest {
        givenStoredWorkout()
        whenever(api.syncWorkout(any())).thenReturn(syncResponse())

        repository.logSet("we1", "ex1", 1, 8, 60.0, null)

        val order = inOrder(setEntryDao, workoutDao, api)
        order.verify(setEntryDao).insert(any())
        order.verify(workoutDao).markUnsynced("w1")
        order.verify(api).syncWorkout(any())
    }

    @Test
    fun `deleteSet removes it and marks the workout unsynced`() = runTest {
        givenStoredWorkout()
        whenever(api.syncWorkout(any())).thenReturn(syncResponse())

        repository.deleteSet(set)

        verify(setEntryDao).delete("s1")
        verify(workoutDao).markUnsynced("w1")
    }

    @Test
    fun `logSet does not estimate a PR when there is no prior best`() = runTest {
        whenever(setEntryDao.getBestWeightForExercise("ex1", 0L)).thenReturn(null)

        assertFalse(repository.logSet("we1", "ex1", 1, 8, 60.0, null).isPersonalRecord)
    }

    @Test
    fun `logSet estimates a PR when it beats the prior best`() = runTest {
        whenever(setEntryDao.getBestWeightForExercise("ex1", 0L)).thenReturn(50.0)

        assertTrue(repository.logSet("we1", "ex1", 1, 8, 60.0, null).isPersonalRecord)
    }

    @Test
    fun `resetting records clears local flags and sets the new start`() = runTest {
        whenever(workoutDao.getUnsynced()).thenReturn(emptyList())
        whenever(api.resetPersonalRecords()).thenReturn(
            Response.success(ResetRecordsResponse("2026-03-01T08:00:00Z"))
        )

        assertTrue(repository.resetPersonalRecords().isSuccess)

        verify(localCache).recordsResetAt = 1772352000000L
        verify(setEntryDao).clearPersonalRecords()
    }

    @Test
    fun `records are kept when queued workouts cannot be sent first`() = runTest {
        givenStoredWorkout()
        whenever(workoutDao.getUnsynced()).thenReturn(listOf(workout))
        whenever(api.syncWorkout(any())).thenThrow(RuntimeException("offline"))

        assertTrue(repository.resetPersonalRecords().isFailure)

        verify(api, never()).resetPersonalRecords()
        verify(setEntryDao, never()).clearPersonalRecords()
    }

    @Test
    fun `the server's PR decision replaces the local estimate`() = runTest {
        givenStoredWorkout()
        whenever(workoutDao.getUnsynced()).thenReturn(listOf(workout))
        whenever(api.syncWorkout(any())).thenReturn(syncResponse(setIsPr = true))

        repository.syncPendingWorkouts()

        verify(setEntryDao).updatePersonalRecord("s1", true)
    }

    @Test
    fun `a successful sync stores the server id and marks the workout synced`() = runTest {
        givenStoredWorkout()
        whenever(workoutDao.getUnsynced()).thenReturn(listOf(workout))
        whenever(api.syncWorkout(any())).thenReturn(syncResponse())

        assertTrue(repository.syncPendingWorkouts())

        val updated = argumentCaptor<WorkoutEntity>()
        verify(workoutDao).update(updated.capture())
        assertEquals("server-1", updated.firstValue.serverId)
        assertTrue(updated.firstValue.synced)
    }

    @Test
    fun `a set logged during a sync keeps the workout unsynced`() = runTest {
        givenStoredWorkout()
        whenever(workoutDao.getUnsynced()).thenReturn(listOf(workout))
        whenever(api.syncWorkout(any())).thenReturn(syncResponse())
        val newSet = set.copy(localId = "s2", setNumber = 2)
        whenever(setEntryDao.getForWorkoutExercise("we1"))
            .thenReturn(listOf(set))
            .thenReturn(listOf(set, newSet))

        repository.syncPendingWorkouts()

        val updated = argumentCaptor<WorkoutEntity>()
        verify(workoutDao).update(updated.capture())
        assertFalse(updated.firstValue.synced)
    }

    @Test
    fun `syncPendingWorkouts sends oldest first and reports a failure`() = runTest {
        val newer = workout.copy(localId = "b", startedAt = 2L)
        val older = workout.copy(localId = "a", startedAt = 1L)
        whenever(workoutDao.getUnsynced()).thenReturn(listOf(newer, older))
        whenever(workoutDao.getByLocalId("a")).thenReturn(older)
        whenever(workoutDao.getByLocalId("b")).thenReturn(newer)
        whenever(workoutExerciseDao.getForWorkout(any())).thenReturn(emptyList())
        whenever(api.syncWorkout(any())).thenThrow(RuntimeException("offline"))

        assertFalse(repository.syncPendingWorkouts())

        val sent = argumentCaptor<SyncWorkoutRequest>()
        verify(api, times(2)).syncWorkout(sent.capture())
        assertEquals(listOf("a", "b"), sent.allValues.map { it.clientId })
    }

    @Test
    fun `completeWorkout returns the server's result when online`() = runTest {
        givenStoredWorkout()
        whenever(api.syncWorkout(any())).thenReturn(
            syncResponse(CompletionDto(50, 2, 3, listOf(BadgeDto("b1", "7-Day Streak", "desc"))))
        )

        val result = repository.completeWorkout("w1")

        assertEquals(50, result.xpEarned)
        assertEquals(2, result.newLevel)
        assertEquals(3, result.streak)
        assertEquals(listOf("7-Day Streak"), result.badgeNames)
        assertFalse(result.pendingSync)
        verify(profileRepository).refresh()
    }

    @Test
    fun `completeWorkout offline gives an estimate and keeps the cached level and streak`() = runTest {
        givenStoredWorkout()
        whenever(setEntryDao.countSets("w1")).thenReturn(2)
        whenever(setEntryDao.countPersonalRecords("w1")).thenReturn(1)
        whenever(api.syncWorkout(any())).thenThrow(RuntimeException("offline"))

        val result = repository.completeWorkout("w1")

        assertTrue(result.pendingSync)
        assertEquals(55, result.xpEarned)
        assertEquals(3, result.newLevel)
        assertEquals(4, result.streak)
        assertEquals(1, deferredSyncs)
    }

    @Test
    fun `restoreHistory inserts missing workouts and skips existing ones`() = runTest {
        val history = listOf(
            HistoryWorkoutDto(
                id = "s1", clientId = "known", title = "A", startedAt = "2026-01-01T08:00:00Z",
                completedAt = "2026-01-01T09:00:00Z", totalVolumeKg = 100.0, xpEarned = 25,
                exercises = emptyList()
            ),
            HistoryWorkoutDto(
                id = "s2", clientId = null, title = "B", startedAt = "2026-01-02T08:00:00Z",
                completedAt = "2026-01-02T09:00:00Z", totalVolumeKg = 200.0, xpEarned = 30,
                exercises = listOf(
                    HistoryExerciseDto(
                        "e1", null, "ex1", "Squat", 0,
                        listOf(HistorySetDto("set1", null, 1, 5, 100.0, null, true))
                    )
                )
            )
        )
        whenever(api.getWorkoutHistory()).thenReturn(Response.success(history))
        whenever(workoutDao.getByLocalId("known")).thenReturn(workout)

        repository.restoreHistory()

        val inserted = argumentCaptor<WorkoutEntity>()
        verify(workoutDao, times(1)).upsert(inserted.capture())
        assertEquals("s2", inserted.firstValue.localId)
        assertTrue(inserted.firstValue.synced)
        verify(setEntryDao).insert(any())
    }

    @Test
    fun `getExerciseLibrary caches a fresh list`() = runTest {
        val exercises = listOf(ExerciseDto("ex1", "Squat", "LEGS", "Barbell", "STRENGTH"))
        whenever(api.listExercises(anyOrNull(), anyOrNull())).thenReturn(Response.success(exercises))

        assertEquals(exercises, repository.getExerciseLibrary().getOrNull())
        verify(localCache).saveExercises(exercises)
    }

    @Test
    fun `getExerciseLibrary falls back to the cache when offline`() = runTest {
        val cached = listOf(ExerciseDto("ex1", "Squat", "LEGS", "Barbell", "STRENGTH"))
        whenever(api.listExercises(anyOrNull(), anyOrNull())).thenThrow(RuntimeException("offline"))
        whenever(localCache.getExercises()).thenReturn(cached)

        assertEquals(cached, repository.getExerciseLibrary().getOrNull())
    }

    @Test
    fun `getExerciseLibrary fails when offline with an empty cache`() = runTest {
        whenever(localCache.getExercises()).thenReturn(null)
        whenever(api.listExercises(anyOrNull(), anyOrNull())).thenThrow(RuntimeException("offline"))

        assertTrue(repository.getExerciseLibrary().isFailure)
    }

    @Test
    fun `getSuggestedWorkout returns success on 2xx`() = runTest {
        whenever(api.getSuggestedWorkout(any())).thenReturn(
            Response.success(SuggestedWorkoutResponse("Leg Day", emptyList()))
        )

        val result = repository.getSuggestedWorkout()

        assertTrue(result.isSuccess)
        assertEquals("Leg Day", result.getOrNull()?.title)
    }

    @Test
    fun `clearLocalDataIfSynced keeps data that could not be uploaded`() = runTest {
        whenever(workoutDao.getUnsynced()).thenReturn(emptyList())
        whenever(workoutDao.countUnsynced()).thenReturn(1)

        repository.clearLocalDataIfSynced()

        verify(workoutDao, never()).deleteAll()
    }

    @Test
    fun `clearLocalDataIfSynced clears the phone when everything is uploaded`() = runTest {
        whenever(workoutDao.getUnsynced()).thenReturn(emptyList())
        whenever(workoutDao.countUnsynced()).thenReturn(0)

        repository.clearLocalDataIfSynced()

        verify(workoutDao).deleteAll()
    }
}
