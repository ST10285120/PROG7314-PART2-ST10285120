package com.fitquest.app.auth

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps the native BiometricPrompt. BIOMETRIC_WEAK covers fingerprint and the face
 * unlock found on most phones, and DEVICE_CREDENTIAL lets a user fall back to their PIN
 * if a finger or face is not recognised. This pairing works on every supported API
 * level, whereas BIOMETRIC_STRONG with DEVICE_CREDENTIAL fails on Android 9 and 10.
 *
 * Reference: Google (2026) Show a biometric authentication dialog.
 * https://developer.android.com/identity/sign-in/biometric-auth
 */
object BiometricAuthenticator {

    private const val TAG = "BiometricAuthenticator"
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** True when the device can show the prompt: it has hardware or a screen lock set up. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Log.i(TAG, "Biometric verification succeeded")
                    onSuccess()
                }

                // Called for a cancel or lockout. A single unrecognised finger only calls
                // onAuthenticationFailed, and the system dialog stays open to try again.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.w(TAG, "Biometric prompt ended: $errorCode")
                    onFailure(errString.toString())
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}
