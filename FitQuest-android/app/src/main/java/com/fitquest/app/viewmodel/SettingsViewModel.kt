package com.fitquest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.remote.dto.PreferencesDto
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: PreferencesDto = PreferencesDto("KG", "SYSTEM", "18:00", 90),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val signedOut: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            preferencesRepository.getPreferences().fold(
                onSuccess = { prefs -> _uiState.value = _uiState.value.copy(preferences = prefs, isLoading = false) },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Couldn't load settings"
                    )
                }
            )
        }
    }

    fun updatePreferences(update: (PreferencesDto) -> PreferencesDto) {
        val newPrefs = update(_uiState.value.preferences)
        _uiState.value = _uiState.value.copy(preferences = newPrefs, isSaving = true)
        viewModelScope.launch {
            preferencesRepository.updatePreferences(newPrefs).fold(
                onSuccess = { saved -> _uiState.value = _uiState.value.copy(preferences = saved, isSaving = false) },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = err.message ?: "Couldn't save settings"
                    )
                }
            )
        }
    }

    fun saveReminderAndRestTimer(reminderTime: String, restTimerDefaultSec: Int) {
        updatePreferences { it.copy(reminderTime = reminderTime, restTimerDefaultSec = restTimerDefaultSec) }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = _uiState.value.copy(signedOut = true)
    }
}
