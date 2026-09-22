package com.fitquest.app.data.repository

import com.fitquest.app.data.local.TokenStore
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.SsoRequest
import com.fitquest.app.data.remote.dto.UserDto

sealed class AuthResult {
    data class Success(val user: UserDto) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

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
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.saveToken(body.token)
                AuthResult.Success(body.user)
            } else {
                AuthResult.Error("Sign-in failed (HTTP ${response.code()})")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error during sign-in")
        }
    }

    fun signOut() {
        tokenStore.clearToken()
    }
}
