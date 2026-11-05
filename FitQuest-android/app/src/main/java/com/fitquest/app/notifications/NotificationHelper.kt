package com.fitquest.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fitquest.app.MainActivity
import com.fitquest.app.R
import com.fitquest.app.util.BadgeText

/**
 * Builds every notification on the phone rather than on the server, so text always
 * uses the language chosen in the app.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ACHIEVEMENTS = "achievements"
    private const val CHANNEL_REMINDERS = "reminders"
    private const val REMINDER_NOTIFICATION_ID = 1001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                context.getString(R.string.channel_achievements),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    /** Background work has no Activity, so apply the app's chosen language here. */
    private fun localized(context: Context): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        val locale = locales[0] ?: return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    fun showAchievement(context: Context, title: String, body: String) {
        show(context, CHANNEL_ACHIEVEMENTS, System.currentTimeMillis().toInt(), title, body)
    }

    fun showBadgeEarned(context: Context, badgeName: String) {
        val text = localized(context)
        val localizedName = BadgeText.nameRes(badgeName)?.let { text.getString(it) } ?: badgeName
        showAchievement(context, text.getString(R.string.notification_badge_title), localizedName)
    }

    fun showLevelUp(context: Context, level: String) {
        val text = localized(context)
        showAchievement(
            context,
            text.getString(R.string.notification_level_title),
            text.getString(R.string.notification_level_body, level)
        )
    }

    fun showTest(context: Context) {
        val text = localized(context)
        showAchievement(
            context,
            text.getString(R.string.notification_test_title),
            text.getString(R.string.notification_test_body)
        )
    }

    fun showStreakReminder(context: Context) {
        val text = localized(context)
        show(
            context,
            CHANNEL_REMINDERS,
            REMINDER_NOTIFICATION_ID,
            text.getString(R.string.notification_reminder_title),
            text.getString(R.string.notification_reminder_body)
        )
    }

    private fun show(context: Context, channel: String, id: Int, title: String, body: String) {
        val allowed = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        // The permission only exists from Android 13; older versions report it as denied.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !allowed) {
            Log.d(TAG, "Notification skipped: permission not granted")
            return
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification blocked: ${e.message}")
        }
    }
}
