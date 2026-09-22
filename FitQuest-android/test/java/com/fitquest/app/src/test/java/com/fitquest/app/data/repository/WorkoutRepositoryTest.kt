package com.fitquest.app.data.repository

import com.fitquest.app.data.local.dao.SetEntryDao
import com.fitquest.app.data.local.dao.WorkoutDao
import com.fitquest.app.data.local.dao.WorkoutExerciseDao
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response

class WorkoutRepositoryTest {

    private lateinit var api: FitQuestApi
    private lateinit var workoutDao: WorkoutDao
    private lateinit var workoutExerciseDao: WorkoutExerciseDao
    private lateinit var setEntryDao: SetEntryDao
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        api = mock()
        workoutDao = mock()
        workoutExerciseDao = mock()
        setEntryDao = mock()
        repository = WorkoutRepository(api, workoutDao, workoutExerciseDao, setEntryDao)
    }

    @Test
    fun `startWorkout inserts locally then marks itself synced when the API call succeeds`() = runTest {
        whenever(api.createWorkout(any())).thenReturn(Response.success(CreateWorkoutResponse("server-1")))
        whenever(workoutDao.getByLocalId(any())).thenAnswer { invocation ->
            WorkoutEntity(localId = invocation.getArgument(0), title = "Push Day", startedAt = 0L, synced = false)
        }

        val returnedLocalId = repository.startWorkout("Push Day")

        assertTrue(returnedLocalId.isNotBlank())

        val upsertCaptor = argumentCaptor<WorkoutEntity>()
        verify(workoutDao).upsert(upsertCaptor.capture())
        assertEquals(returnedLocalId, upsertCaptor.firstValue.localId)
        assertFalse(upsertCaptor.firstValue.synced)

        val updateCaptor = argumentCaptor<WorkoutEntity>()
        verify(workoutDao).update(updateCaptor.capture())
        assertEquals("server-1", updateCaptor.firstValue.serverId)
        assertTrue(updateCaptor.firstValue.synced)
    }

    @Test
    fun `startWorkout leaves the workout unsynced when the API call throws`() = runTest {
        whenever(api.createWorkout(any())).thenThrow(RuntimeException("offline"))

        val localId = repository.startWorkout("Push Day")

        assertTrue(localId.isNotBlank())
        verify(workoutDao).upsert(any())
        verify(workoutDao, never()).update(any())
    }

    @Test
    fun `addExercise skips the API call when the parent workout has not synced`() = runTest {
        whenever(workoutDao.getByLocalId("w1")).thenReturn(
            WorkoutEntity(localId = "w1", title = "X", startedAt = 0L, serverId = null, synced = false)
        )

        repository.addExercise("w1", "ex1", "Bench Press", 0)

        verify(api, never()).addExercise(any(), any())
        verify(workoutExerciseDao, times(1)).upsert(any())
    }

    @Test
    fun `addExercise calls the API and stores the returned server id when the parent workout has synced`() = runTest {
        whenever(workoutDao.getByLocalId("w1")).thenReturn(
            WorkoutEntity(localId = "w1", title = "X", startedAt = 0L, serverId = "w1-server", synced = true)
        )
        whenever(api.addExercise(eq("w1-server"), any())).thenReturn(Response.success(AddExerciseResponse("we-server")))
        whenever(workoutExerciseDao.getByLocalId(any())).thenAnswer { invocation ->
            WorkoutExerciseEntity(
                localId = invocation.getArgument(0), workoutLocalId = "w1",
                exerciseId = "ex1", exerciseName = "Bench Press", orderIndex = 0
            )
        }

        repository.addExercise("w1", "ex1", "Bench Press", 0)

        verify(api).addExercise(eq("w1-server"), any())
        val upsertCaptor = argumentCaptor<WorkoutExerciseEntity>()
        verify(workoutExerciseDao, times(2)).upsert(upsertCaptor.capture())
        assertEquals("we-server", upsertCaptor.secondValue.serverId)
    }

    @Test
    fun `logSet does not mark a set as a personal record when there is no prior best to beat`() = runTest {
        whenever(setEntryDao.getBestWeightForExercise("ex1")).thenReturn(null)
        whenever(workoutExerciseDao.getByLocalId("we1")).thenReturn(null)

        val entry = repository.logSet("we1", "ex1", 1, 8, 60.0, null)

        assertFalse(entry.isPersonalRecord)
    }

    @Test
    fun `logSet marks a set as a personal record when it beats a real prior best`() = runTest {
        whenever(setEntryDao.getBestWeightForExercise("ex1")).thenReturn(50.0)
        whenever(workoutExerciseDao.getByLocalId("we1")).thenReturn(null)

        val entry = repository.logSet("we1", "ex1", 1, 8, 60.0, null)

        assertTrue(entry.isPersonalRecord)
    }

    @Test
    fun `logSet does not mark a set as a personal record when it does not beat the prior best`() = runTest {
        whenever(setEntryDao.getBestWeightForExercise("ex1")).thenReturn(100.0)
        whenever(workoutExerciseDao.getByLocalId("we1")).thenReturn(null)

        val entry = repository.logSet("we1", "ex1", 1, 8, 60.0, null)

        assertFalse(entry.isPersonalRecord)
    }

    @Test
    fun `logSet forwards to the API with the parent workout's server id once both have synced`() = runTest {
        whenever(setEntryDao.getBestWeightForExercise("ex1")).thenReturn(0.0)
        whenever(workoutExerciseDao.getByLocalId("we1")).thenReturn(
            WorkoutExerciseEntity(localId = "we1", serverId = "we1-server", workoutLocalId = "w1", exerciseId = "ex1", exerciseName = "Bench", orderIndex = 0)
        )
        whenever(workoutDao.getByLocalId("w1")).thenReturn(
            WorkoutEntity(localId = "w1", serverId = "w1-server", title = "X", startedAt = 0L, synced = true)
        )
        whenever(api.logSet(eq("w1-server"), eq("we1-server"), any())).thenReturn(Response.success(LogSetResponse("set-1", true)))

        repository.logSet("we1", "ex1", 1, 8, 60.0, null)

        verify(api).logSet(eq("w1-server"), eq("we1-server"), any())
    }

    @Test
    fun `logSet skips the API call when the workout exercise has not synced`() = runTest {
        whenever(setEntryDao.getBestWeightForExercise("ex1")).thenReturn(0.0)
        whenever(workoutExerciseDao.getByLocalId("we1")).thenReturn(
            WorkoutExerciseEntity(localId = "we1", serverId = null, workoutLocalId = "w1", exerciseId = "ex1", exerciseName = "Bench", orderIndex = 0)
        )

        repository.logSet("we1", "ex1", 1, 8, 60.0, null)

        verify(api, never()).logSet(any(), any(), any())
    }

    @Test
    fun `completeWorkout returns the server's authoritative result when the workout has synced`() = runTest {
        whenever(workoutDao.getByLocalId("w1")).thenReturn(
            WorkoutEntity(localId = "w1", serverId = "w1-server", title = "X", startedAt = 0L, synced = true)
        )
        whenever(api.completeWorkout(eq("w1-server"), any())).thenReturn(
            Response.success(
                CompleteWorkoutResponse(
                    xpEarned = 50, newLevel = 2, streak = 3,
                    badgesEarned = listOf(BadgeDto("b1", "7-Day Streak", "desc"))
                )
            )
        )

        val result = repository.completeWorkout("w1")

        assertEquals(50, result.xpEarned)
        assertEquals(2, result.newLevel)
        assertEquals(3, result.streak)
        assertEquals(listOf("7-Day Streak"), result.badgeNames)
        assertFalse(result.pendingSync)
    }

    @Test
    fun `completeWorkout returns a pending local estimate when the workout has not synced`() = runTest {
        whenever(workoutDao.getByLocalId("w1")).thenReturn(
            WorkoutEntity(localId = "w1", serverId = null, title = "X", startedAt = 0L, synced = false)
        )

        val result = repository.completeWorkout("w1")

        assertTrue(result.pendingSync)
        assertEquals(20, result.xpEarned)
        verify(api, never()).completeWorkout(any(), any())
    }

    @Test
    fun `completeWorkout falls back to a pending estimate when the synced API call fails`() = runTest {
        whenever(workoutDao.getByLocalId("w1")).thenReturn(
            WorkoutEntity(localId = "w1", serverId = "w1-server", title = "X", startedAt = 0L, synced = true)
        )
        whenever(api.completeWorkout(any(), any())).thenThrow(RuntimeException("offline"))

        val result = repository.completeWorkout("w1")

        assertTrue(result.pendingSync)
    }

    @Test
    fun `syncPendingWorkouts retries every unsynced workout`() = runTest {
        whenever(workoutDao.getUnsynced()).thenReturn(
            listOf(
                WorkoutEntity(localId = "a", title = "A", startedAt = 1L, synced = false),
                WorkoutEntity(localId = "b", title = "B", startedAt = 2L, synced = false)
            )
        )
        whenever(api.createWorkout(any())).thenReturn(Response.success(CreateWorkoutResponse("srv")))
        whenever(workoutDao.getByLocalId(any())).thenAnswer { invocation ->
            WorkoutEntity(localId = invocation.getArgument(0), title = "X", startedAt = 0L, synced = false)
        }

        repository.syncPendingWorkouts()

        verify(api, times(2)).createWorkout(any())
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
    fun `getExerciseLibrary returns failure when the call throws`() = runTest {
        whenever(api.listExercises(anyOrNull(), anyOrNull())).thenThrow(RuntimeException("offline"))

        val result = repository.getExerciseLibrary()

        assertTrue(result.isFailure)
    }
}
