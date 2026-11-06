package com.fitquest.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.remote.dto.PreferencesDto
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.PreferencesRepository
import com.fitquest.app.data.repository.ProfileRepository
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the Settings screen displays. */
data class SettingsUiState(
    val preferences: PreferencesDto = PreferencesDto("KG", "SYSTEM", "18:00", 90),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val signedOut: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val avatarError: Boolean = false,
    val testNotificationResult: Int? = null,
    val deleteAccountError: Boolean = false
)

/** Settings, account card, profile picture and sign-out. */
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    val profile: StateFlow<UserDto?> = profileRepository.profile

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            preferencesRepository.getPreferences().fold(
                onSuccess = { prefs -> _uiState.value = _uiState.value.copy(preferences = prefs, isLoading = false, errorMessage = null) },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Couldn't load settings"
                    )
                }
            )
        }
    }

    /** This ViewModel outlives a sign-out, so the flag must not carry into the next session. */
    fun consumeSignedOut() {
        _uiState.value = SettingsUiState()
    }

    fun updatePreferences(update: (PreferencesDto) -> PreferencesDto) {
        val newPrefs = update(_uiState.value.preferences)
        _uiState.value = _uiState.value.copy(preferences = newPrefs, isSaving = true, errorMessage = null)
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

    fun uploadAvatar(jpegBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingAvatar = true, avatarError = false)
            val result = profileRepository.uploadAvatar(jpegBytes)
            _uiState.value = _uiState.value.copy(isUploadingAvatar = false, avatarError = result.isFailure)
        }
    }

    fun reportAvatarError() {
        _uiState.value = _uiState.value.copy(avatarError = true)
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val delivered = profileRepository.sendTestNotification().getOrDefault(0)
            _uiState.value = _uiState.value.copy(testNotificationResult = delivered)
        }
    }

    /** Uploads what it can first, so a sign-out never silently loses a workout. */
    fun signOut() {
        viewModelScope.launch {
            workoutRepository.clearLocalDataIfSynced()
            authRepository.signOut()
            profileRepository.clear()
            _uiState.value = _uiState.value.copy(signedOut = true)
            Log.i(TAG, "Signed out")
        }
    }

    /** Only clears the phone once the server confirms that the account is gone. */
    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deleteAccountError = false)
            profileRepository.deleteAccount().fold(
                onSuccess = {
                    workoutRepository.clearLocalData()
                    authRepository.signOut()
                    profileRepository.clear()
                    _uiState.value = _uiState.value.copy(signedOut = true)
                },
                onFailure = { _uiState.value = _uiState.value.copy(deleteAccountError = true) }
            )
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
