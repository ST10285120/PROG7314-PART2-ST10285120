package com.fitquest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.remote.dto.BadgeDto
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BadgesUiState(
    val badges: List<BadgeDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class BadgesViewModel(private val workoutRepository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            workoutRepository.getBadges().fold(
                onSuccess = { badges -> _uiState.value = BadgesUiState(badges = badges, isLoading = false) },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Couldn't load badges"
                    )
                }
            )
        }
    }
}
