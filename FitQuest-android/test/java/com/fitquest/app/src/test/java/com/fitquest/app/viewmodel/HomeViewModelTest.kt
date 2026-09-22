package com.fitquest.app.viewmodel

import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.remote.dto.SuggestedWorkoutExerciseDto
import com.fitquest.app.data.remote.dto.SuggestedWorkoutResponse
import com.fitquest.app.data.repository.WorkoutRepository
import com.fitquest.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun mockRepository(recent: kotlinx.coroutines.flow.Flow<List<WorkoutEntity>> = flowOf(emptyList())): WorkoutRepository {
        val repo: WorkoutRepository = mock()
        whenever(repo.observeRecentCompleted()).thenReturn(recent)
        return repo
    }

    @Test
    fun `loads the suggested workout successfully on init`() = runTest {
        val repo = mockRepository()
        whenever(repo.getSuggestedWorkout()).thenReturn(
            Result.success(
                SuggestedWorkoutResponse(
                    title = "Leg Day",
                    exercises = listOf(SuggestedWorkoutExerciseDto("ex1", "Squat", "LEGS"))
                )
            )
        )

        val viewModel = HomeViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals("Leg Day", state.suggestedTitle)
        assertEquals(1, state.suggestedExercises.size)
        assertEquals(false, state.isLoadingSuggestion)
        assertNull(state.errorMessage)
    }

    @Test
    fun `sets an error message when the suggestion fails to load`() = runTest {
        val repo = mockRepository()
        whenever(repo.getSuggestedWorkout()).thenReturn(Result.failure(Exception("no network")))

        val viewModel = HomeViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals("no network", state.errorMessage)
        assertEquals(false, state.isLoadingSuggestion)
    }

    @Test
    fun `recentWorkouts reflects the repository's local workout history`() = runTest {
        val workouts = listOf(
            WorkoutEntity(localId = "w1", title = "Push Day", startedAt = 0L, completedAt = 100L, xpEarned = 50, synced = true)
        )
        val repo = mockRepository(recent = MutableStateFlow(workouts))
        whenever(repo.getSuggestedWorkout()).thenReturn(Result.success(SuggestedWorkoutResponse("X", emptyList())))

        val viewModel = HomeViewModel(repo)

        assertEquals(workouts, viewModel.recentWorkouts.value)
    }
}
