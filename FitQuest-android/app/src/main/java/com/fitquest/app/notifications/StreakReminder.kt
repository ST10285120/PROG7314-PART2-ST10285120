package com.fitquest.app.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fitquest.app.FitQuestApp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

/** Runs once a day at the chosen time and reminds the user if they have not trained (FR8). */
class StreakReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FitQuestApp).container
        if (!container.tokenStore.isSignedIn()) return Result.success()

        val latest = container.workoutRepository.getLatestCompletedAt()
        val trainedToday = latest != null &&
            Instant.ofEpochMilli(latest).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()

        if (!trainedToday) {
            Log.i(TAG, "No workout today; sending the streak reminder")
            NotificationHelper.showStreakReminder(applicationContext)
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "StreakReminderWorker"
    }
}

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val WORK_NAME = "fitquest-streak-reminder"
    private const val HOURS_PER_DAY = 24L

    /** Replaces the schedule, so changing the time in Settings takes effect at once. */
    fun schedule(context: Context, reminderTime: String) {
        val time = try {
            LocalTime.parse(reminderTime)
        } catch (e: DateTimeParseException) {
            Log.w(TAG, "Ignoring invalid reminder time")
            return
        }

        val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(HOURS_PER_DAY, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(time).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        Log.d(TAG, "Streak reminder scheduled for $reminderTime")
    }

    private fun delayUntil(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
