package com.fitquest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.auth.SsoResult
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.AuthResult
import com.fitquest.app.auth.GoogleSignInManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object SignedOut : AuthUiState()
    data object Loading : AuthUiState()
    data class SignedIn(val user: UserDto) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(
        if (authRepository.isSignedIn()) AuthUiState.Loading else AuthUiState.SignedOut
    )
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        if (authRepository.isSignedIn()) continueWithCachedSession()
    }

    fun continueWithCachedSession() {
        _uiState.value = AuthUiState.SignedIn(
            UserDto(id = "", username = "", email = "", xpTotal = 0, level = 1, currentStreak = 0)
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

    private suspend fun completeSignIn(call: suspend () -> AuthResult) {
        when (val result = call()) {
            is AuthResult.Success -> _uiState.value = AuthUiState.SignedIn(result.user)
            is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.SignedOut
    }
}
