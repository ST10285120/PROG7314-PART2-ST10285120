package com.fitquest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.local.dao.PersonalRecordRow
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.remote.dto.SuggestedWorkoutExerciseDto
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val suggestedTitle: String = "Loading suggestion…",
    val suggestedExercises: List<SuggestedWorkoutExerciseDto> = emptyList(),
    val isLoadingSuggestion: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(private val workoutRepository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val recentWorkouts: StateFlow<List<WorkoutEntity>> = workoutRepository
        .observeRecentCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalXp: StateFlow<Int> = workoutRepository
        .observeTotalXp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val personalRecords: StateFlow<List<PersonalRecordRow>> = workoutRepository
        .observePersonalRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSuggestion()
        syncPending()
    }

    fun loadSuggestion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSuggestion = true, errorMessage = null)
            workoutRepository.getSuggestedWorkout().fold(
                onSuccess = { suggestion ->
                    _uiState.value = _uiState.value.copy(
                        suggestedTitle = suggestion.title,
                        suggestedExercises = suggestion.exercises,
                        isLoadingSuggestion = false
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSuggestion = false,
                        errorMessage = err.message ?: "Couldn't load a suggestion right now"
                    )
                }
            )
        }
    }

    private fun syncPending() {
        viewModelScope.launch { workoutRepository.syncPendingWorkouts() }
    }
}
