package com.fitquest.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keeps the JWT and the pending OAuth state in encrypted storage, so that a signed-in
 * user can keep logging workouts offline (Part 1, "Session persistence").
 *
 * Reference: Google (2026) Work with data more securely.
 * https://developer.android.com/privacy-and-security/cryptography
 */
class TokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "fitquest_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun isSignedIn(): Boolean = getToken() != null

    /** Remembers the OAuth state across the browser round trip and any process death. */
    fun saveOAuthState(state: String) {
        prefs.edit().putString(KEY_OAUTH_STATE, state).apply()
    }

    /** Returns the saved state and removes it, so each value can only be used once. */
    fun consumeOAuthState(): String? {
        val state = prefs.getString(KEY_OAUTH_STATE, null)
        prefs.edit().remove(KEY_OAUTH_STATE).apply()
        return state
    }

    companion object {
        private const val KEY_TOKEN = "jwt"
        private const val KEY_OAUTH_STATE = "oauth_state"
    }
}
