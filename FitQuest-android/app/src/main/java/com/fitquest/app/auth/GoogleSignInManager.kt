package com.fitquest.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.fitquest.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed class SsoResult {
    data class Success(val idToken: String) : SsoResult()
    data class Failure(val message: String) : SsoResult()
}

class GoogleSignInManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activityContext: Context): SsoResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                SsoResult.Success(googleIdTokenCredential.idToken)
            } else {
                SsoResult.Failure("Unexpected credential type returned by Credential Manager")
            }
        } catch (e: GetCredentialException) {
            SsoResult.Failure(e.message ?: "Google sign-in was cancelled or failed")
        } catch (e: GoogleIdTokenParsingException) {
            SsoResult.Failure("Could not parse Google ID token: ${e.message}")
        }
    }
}
