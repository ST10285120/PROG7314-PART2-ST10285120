package com.fitquest.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.auth.GoogleSignInManager
import com.fitquest.app.auth.SsoResult
import com.fitquest.app.data.remote.SessionEvents
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.AuthResult
import com.fitquest.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object SignedOut : AuthUiState()
    data object Loading : AuthUiState()
    data object SessionExpired : AuthUiState()
    data class SignedIn(val user: UserDto) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/** Drives SSO sign-in and returns the user to sign-in when the session expires. */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleSignInManager: GoogleSignInManager,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(
        if (authRepository.isSignedIn()) AuthUiState.Loading else AuthUiState.SignedOut
    )
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        if (authRepository.isSignedIn()) continueWithCachedSession()
        viewModelScope.launch {
            SessionEvents.expired.collect {
                Log.i(TAG, "Session expired")
                _uiState.value = AuthUiState.SessionExpired
            }
        }
    }

    /** A cached session opens straight away, including offline. */
    fun continueWithCachedSession() {
        val cached = profileRepository.profile.value
        _uiState.value = AuthUiState.SignedIn(
            cached ?: UserDto(id = "", username = "", email = "", xpTotal = 0, level = 1, currentStreak = 0)
        )
    }

    fun signInWithGoogle(activityContext: android.content.Context) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = googleSignInManager.signIn(activityContext)) {
                is SsoResult.Success -> completeSignIn { authRepository.signInWithGoogle(result.idToken) }
                is SsoResult.Failure -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun signInWithGitHubCode(code: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            completeSignIn { authRepository.signInWithGitHub(code) }
        }
    }

    /** Called when the state check fails. The code is discarded without being used. */
    fun rejectGitHubCallback() {
        Log.w(TAG, "GitHub callback rejected: state mismatch")
        _uiState.value = AuthUiState.Error("GitHub sign-in could not be verified")
    }

    fun reportSignInError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    private suspend fun completeSignIn(call: suspend () -> AuthResult) {
        when (val result = call()) {
            is AuthResult.Success -> {
                profileRepository.setProfile(result.user)
                _uiState.value = AuthUiState.SignedIn(result.user)
            }
            is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.SignedOut
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
