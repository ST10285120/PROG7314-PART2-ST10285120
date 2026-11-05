package com.fitquest.app.data.repository

import android.util.Log
import com.fitquest.app.data.local.TokenStore
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.SsoRequest
import com.fitquest.app.data.remote.dto.UserDto

sealed class AuthResult {
    data class Success(val user: UserDto) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/** Exchanges a provider credential for a FitQuest JWT and owns the saved session. */
class AuthRepository(
    private val api: FitQuestApi,
    private val tokenStore: TokenStore
) {

    fun isSignedIn(): Boolean = tokenStore.isSignedIn()

    suspend fun signInWithGoogle(idToken: String): AuthResult =
        signIn(SsoRequest(provider = "GOOGLE", idToken = idToken))

    suspend fun signInWithGitHub(code: String): AuthResult =
        signIn(SsoRequest(provider = "GITHUB", code = code))

    private suspend fun signIn(request: SsoRequest): AuthResult {
        return try {
            val response = api.ssoSignIn(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                tokenStore.saveToken(body.token)
                Log.i(TAG, "Signed in with ${request.provider}")
                AuthResult.Success(body.user)
            } else {
                Log.w(TAG, "Sign-in rejected with HTTP ${response.code()}")
                AuthResult.Error("Sign-in failed (HTTP ${response.code()})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in request failed", e)
            AuthResult.Error(e.message ?: "Network error during sign-in")
        }
    }

    fun signOut() {
        tokenStore.clearToken()
        Log.i(TAG, "Signed out")
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
