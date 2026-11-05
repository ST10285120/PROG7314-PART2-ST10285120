package com.fitquest.app.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.fitquest.app.BuildConfig
import com.fitquest.app.data.local.TokenStore
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Starts GitHub sign-in in a Custom Tab. A random state value travels through the
 * browser and must match when the app is reopened, which stops another app or page
 * from injecting its own authorization code (RFC 6749, section 10.12).
 */
class GitHubOAuthManager(
    private val context: Context,
    private val tokenStore: TokenStore
) {

    private fun generateState(): String {
        val bytes = ByteArray(STATE_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildAuthorizeUri(state: String): Uri {
        return Uri.parse("https://github.com/login/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
            .appendQueryParameter("redirect_uri", GITHUB_REDIRECT_URI)
            .appendQueryParameter("scope", "read:user user:email")
            .appendQueryParameter("state", state)
            .build()
    }

    /** Opens the browser. Returns false if the device has no browser to open. */
    fun launch(activityContext: Context): Boolean {
        val state = generateState()
        tokenStore.saveOAuthState(state)
        return try {
            CustomTabsIntent.Builder().build().launchUrl(activityContext, buildAuthorizeUri(state))
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No browser available for GitHub sign-in", e)
            false
        }
    }

    /** Checks the returned state in constant time. A saved value can only be used once. */
    fun isValidState(returnedState: String?): Boolean {
        val expected = tokenStore.consumeOAuthState()
        if (expected == null || returnedState == null) return false
        return MessageDigest.isEqual(expected.toByteArray(), returnedState.toByteArray())
    }

    companion object {
        private const val TAG = "GitHubOAuthManager"
        private const val STATE_BYTES = 16
        const val GITHUB_REDIRECT_URI = "com.fitquest.app://oauth-callback"
    }
}
