package com.fitquest.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.remote.dto.SuggestedWorkoutExerciseDto
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.data.repository.ProfileRepository
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val suggestedTitle: String = "",
    val suggestedExercises: List<SuggestedWorkoutExerciseDto> = emptyList(),
    val focusMuscleGroups: List<String> = emptyList(),
    val isLoadingSuggestion: Boolean = true,
    val errorMessage: String? = null
)

/** Home dashboard state. Level and streak always come from the API profile. */
class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val profile: StateFlow<UserDto?> = profileRepository.profile

    val recentWorkouts: StateFlow<List<WorkoutEntity>> = workoutRepository
        .observeRecentCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** XP from finished workouts still waiting to reach the server. */
    val pendingXp: StateFlow<Int> = workoutRepository
        .observePendingXp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        Log.d(TAG, "Created")
        refresh()
    }

    /** Runs at start-up and after reconnecting. Each step copes with having no network. */
    fun refresh() {
        loadSuggestion()
        viewModelScope.launch {
            workoutRepository.syncPendingWorkouts()
            workoutRepository.restoreHistory()
            profileRepository.refresh()
            workoutRepository.getExerciseLibrary()
        }
    }

    fun refreshProfile() {
        viewModelScope.launch { profileRepository.refresh() }
    }

    fun loadSuggestion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSuggestion = true, errorMessage = null)
            workoutRepository.getSuggestedWorkout().fold(
                onSuccess = { suggestion ->
                    _uiState.value = _uiState.value.copy(
                        suggestedTitle = suggestion.title,
                        suggestedExercises = suggestion.exercises,
                        focusMuscleGroups = suggestion.focusMuscleGroups,
                        isLoadingSuggestion = false
                    )
                },
                onFailure = { err ->
                    Log.w(TAG, "Suggestion unavailable: ${err.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoadingSuggestion = false,
                        errorMessage = err.message ?: "Couldn't load a suggestion right now"
                    )
                }
            )
        }
    }

    override fun onCleared() {
        Log.d(TAG, "Cleared")
        super.onCleared()
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
