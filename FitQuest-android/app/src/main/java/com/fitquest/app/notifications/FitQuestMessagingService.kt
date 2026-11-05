package com.fitquest.app.notifications

import android.util.Log
import com.fitquest.app.FitQuestApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives real-time messages from the API through FCM. Data-only messages reach
 * onMessageReceived whether the app is open or not, so the text can be localised here.
 *
 * Reference: Google (2026) Receive messages in an Android app.
 * https://firebase.google.com/docs/cloud-messaging/android/receive-messages
 */
class FitQuestMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM token refreshed")
        val container = (application as FitQuestApp).container
        if (!container.tokenStore.isSignedIn()) return
        scope.launch { container.profileRepository.registerDeviceToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]
        Log.i(TAG, "Push received: $type")
        when (type) {
            "BADGE" -> message.data["badgeName"]?.let { NotificationHelper.showBadgeEarned(this, it) }
            "LEVEL_UP" -> message.data["level"]?.let { NotificationHelper.showLevelUp(this, it) }
            "TEST" -> NotificationHelper.showTest(this)
            else -> Log.w(TAG, "Unknown push type ignored")
        }
    }

    private companion object {
        const val TAG = "FitQuestMessaging"
    }
}
