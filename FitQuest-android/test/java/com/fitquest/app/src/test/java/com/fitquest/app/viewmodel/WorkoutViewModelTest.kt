package com.fitquest.app.viewmodel

import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.data.repository.CompletionResult
import com.fitquest.app.data.repository.WorkoutRepository
import com.fitquest.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: WorkoutRepository
    private lateinit var viewModel: WorkoutViewModel

    private val benchPress = ExerciseDto("ex1", "Bench Press", "CHEST", "Barbell", "STRENGTH")

    @Before
    fun setUp() {
        repo = mock()
        whenever(repo.observeExercisesForWorkout(any())).thenReturn(flowOf(emptyList()))
        whenever(repo.observeSetsForWorkoutExercise(any())).thenReturn(flowOf(emptyList()))
        viewModel = WorkoutViewModel(repo)
    }

    @Test
    fun `startWorkout stores the returned id and loads the exercise library`() = runTest {
        whenever(repo.startWorkout("Push Day")).thenReturn("w1")
        whenever(repo.getExerciseLibrary()).thenReturn(Result.success(listOf(benchPress)))

        viewModel.startWorkout("Push Day")

        val state = viewModel.uiState.value
        assertEquals("w1", state.workoutLocalId)
        assertEquals(listOf(benchPress), state.exerciseLibrary)
    }

    @Test
    fun `addExercise does nothing when no workout has been started yet`() = runTest {
        viewModel.addExercise(benchPress, 0)

        verify(repo, never()).addExercise(any(), any(), any(), any())
    }

    @Test
    fun `addExercise stores the returned workout-exercise id as current`() = runTest {
        whenever(repo.startWorkout(any())).thenReturn("w1")
        whenever(repo.getExerciseLibrary()).thenReturn(Result.success(emptyList()))
        whenever(repo.addExercise("w1", "ex1", "Bench Press", 0)).thenReturn("we1")

        viewModel.startWorkout("Push Day")
        viewModel.addExercise(benchPress, 0)

        assertEquals("we1", viewModel.uiState.value.currentExerciseLocalId)
    }

    @Test
    fun `logSet does nothing when no exercise is currently selected`() = runTest {
        viewModel.logSet("ex1", 1, 8, 60.0, null)

        verify(repo, never()).logSet(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `logSet forwards to the repository using the current exercise's local id`() = runTest {
        whenever(repo.startWorkout(any())).thenReturn("w1")
        whenever(repo.getExerciseLibrary()).thenReturn(Result.success(emptyList()))
        whenever(repo.addExercise(any(), any(), any(), any())).thenReturn("we1")

        viewModel.startWorkout("Push Day")
        viewModel.addExercise(benchPress, 0)
        viewModel.logSet("ex1", 1, 8, 60.0, null)

        verify(repo).logSet("we1", "ex1", 1, 8, 60.0, null)
    }

    @Test
    fun `finishWorkout stores the completion result from the repository`() = runTest {
        whenever(repo.startWorkout(any())).thenReturn("w1")
        whenever(repo.getExerciseLibrary()).thenReturn(Result.success(emptyList()))
        val completion = CompletionResult(xpEarned = 50, newLevel = 2, streak = 3, badgeNames = listOf("7-Day Streak"), pendingSync = false)
        whenever(repo.completeWorkout("w1")).thenReturn(completion)

        viewModel.startWorkout("Push Day")
        viewModel.finishWorkout()

        assertEquals(completion, viewModel.uiState.value.completion)
    }

    @Test
    fun `resetForNewWorkout clears state back to its defaults`() = runTest {
        whenever(repo.startWorkout(any())).thenReturn("w1")
        whenever(repo.getExerciseLibrary()).thenReturn(Result.success(emptyList()))
        viewModel.startWorkout("Push Day")

        viewModel.resetForNewWorkout()

        val state = viewModel.uiState.value
        assertNull(state.workoutLocalId)
        assertNull(state.currentExerciseLocalId)
        assertNull(state.completion)
    }
}
