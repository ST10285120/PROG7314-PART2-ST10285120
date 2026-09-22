package com.fitquest.app.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.fitquest.app.BuildConfig
import java.security.SecureRandom

class GitHubOAuthManager(private val context: Context) {

    fun generateState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun buildAuthorizeUri(state: String): Uri {
        return Uri.parse("https://github.com/login/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
            .appendQueryParameter("redirect_uri", GITHUB_REDIRECT_URI)
            .appendQueryParameter("scope", "read:user user:email")
            .appendQueryParameter("state", state)
            .build()
    }

    fun launch(state: String) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(context, buildAuthorizeUri(state))
    }

    companion object {
        const val GITHUB_REDIRECT_URI = "com.fitquest.app://oauth-callback"
    }
}
