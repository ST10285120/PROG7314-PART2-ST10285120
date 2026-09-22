package com.fitquest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.repository.WorkoutDetail
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(
    val detail: WorkoutDetail? = null,
    val isLoading: Boolean = true
)

class WorkoutDetailViewModel(private val workoutRepository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState

    fun load(workoutLocalId: String) {
        viewModelScope.launch {
            _uiState.value = WorkoutDetailUiState(isLoading = true)
            val detail = workoutRepository.getWorkoutDetail(workoutLocalId)
            _uiState.value = WorkoutDetailUiState(detail = detail, isLoading = false)
        }
    }
}
