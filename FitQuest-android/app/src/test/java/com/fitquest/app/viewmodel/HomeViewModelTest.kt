package com.fitquest.app.viewmodel

import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.remote.dto.SuggestedWorkoutExerciseDto
import com.fitquest.app.data.remote.dto.SuggestedWorkoutResponse
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.data.repository.ProfileRepository
import com.fitquest.app.data.repository.WorkoutRepository
import com.fitquest.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileRepository: ProfileRepository = mock()

    private fun mockRepository(recent: Flow<List<WorkoutEntity>> = flowOf(emptyList())): WorkoutRepository {
        val repo: WorkoutRepository = mock()
        whenever(repo.observeRecentCompleted()).thenReturn(recent)
        whenever(repo.observePendingXp()).thenReturn(flowOf(0))
        whenever(repo.observeActiveWorkout()).thenReturn(flowOf(null))
        whenever(profileRepository.profile).thenReturn(MutableStateFlow(null))
        return repo
    }

    @Test
    fun `loads the suggested workout successfully on init`() = runTest {
        val repo = mockRepository()
        whenever(repo.getSuggestedWorkout()).thenReturn(
            Result.success(
                SuggestedWorkoutResponse(
                    title = "Leg Day",
                    exercises = listOf(SuggestedWorkoutExerciseDto("ex1", "Squat", "LEGS")),
                    focusMuscleGroups = listOf("LEGS")
                )
            )
        )

        val viewModel = HomeViewModel(repo, profileRepository)

        val state = viewModel.uiState.value
        assertEquals("Leg Day", state.suggestedTitle)
        assertEquals(1, state.suggestedExercises.size)
        assertEquals(listOf("LEGS"), state.focusMuscleGroups)
        assertEquals(false, state.isLoadingSuggestion)
        assertNull(state.errorMessage)
    }

    @Test
    fun `sets an error message when the suggestion fails to load`() = runTest {
        val repo = mockRepository()
        whenever(repo.getSuggestedWorkout()).thenReturn(Result.failure(Exception("no network")))

        val viewModel = HomeViewModel(repo, profileRepository)

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

        val viewModel = HomeViewModel(repo, profileRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.recentWorkouts.collect {} }

        assertEquals(workouts, viewModel.recentWorkouts.value)
    }

    @Test
    fun `refresh syncs, restores history and reloads the profile`() = runTest {
        val repo = mockRepository()
        whenever(repo.getSuggestedWorkout()).thenReturn(Result.success(SuggestedWorkoutResponse("X", emptyList())))

        HomeViewModel(repo, profileRepository)

        verify(repo, atLeastOnce()).syncPendingWorkouts()
        verify(repo, atLeastOnce()).restoreHistory()
        verify(profileRepository, atLeastOnce()).refresh()
    }

    @Test
    fun `ensureLoaded does nothing until the session has ended`() = runTest {
        val repo = mockRepository()
        whenever(repo.getSuggestedWorkout()).thenReturn(Result.success(SuggestedWorkoutResponse("X", emptyList())))
        val viewModel = HomeViewModel(repo, profileRepository)

        viewModel.ensureLoaded()
        verify(repo, times(1)).getSuggestedWorkout()

        viewModel.markSessionEnded()
        viewModel.ensureLoaded()
        verify(repo, times(2)).getSuggestedWorkout()
    }

    @Test
    fun `profile comes from the API-backed repository`() = runTest {
        val repo = mockRepository()
        val user = UserDto("u1", "Sam", "sam@example.com", 420, 3, 6)
        whenever(profileRepository.profile).thenReturn(MutableStateFlow(user))
        whenever(repo.getSuggestedWorkout()).thenReturn(Result.success(SuggestedWorkoutResponse("X", emptyList())))

        val viewModel = HomeViewModel(repo, profileRepository)

        assertEquals(3, viewModel.profile.value?.level)
        assertEquals(6, viewModel.profile.value?.currentStreak)
    }
}
