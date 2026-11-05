package com.fitquest.app.notifications

import android.util.Log
import com.fitquest.app.data.repository.ProfileRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Sends this device's FCM token to the API after sign-in. */
object PushTokenRegistrar {

    private const val TAG = "PushTokenRegistrar"

    fun register(scope: CoroutineScope, profileRepository: ProfileRepository) {
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                scope.launch { profileRepository.registerDeviceToken(token) }
            }
        } catch (e: IllegalStateException) {
            // Firebase is not set up when the app is built without google-services.json.
            Log.w(TAG, "Firebase is not configured; push is disabled")
        }
    }
}
