package com.fitquest.app.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fitquest.app.FitQuestApp
import java.util.concurrent.TimeUnit

/**
 * Uploads pending workouts. WorkManager holds this job until the phone has a
 * connection, and runs it even if the app has been closed in the meantime.
 *
 * Reference: Google (2026) Schedule tasks with WorkManager.
 * https://developer.android.com/topic/libraries/architecture/workmanager
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FitQuestApp).container
        // After sign-out or an expired session, retrying cannot work. The data stays in
        // Room and is uploaded after the next sign-in.
        if (!container.tokenStore.isSignedIn()) return Result.success()

        val allSynced = container.workoutRepository.syncPendingWorkouts()
        Log.i(TAG, "Background sync finished, allSynced=$allSynced")
        return if (allSynced) Result.success() else Result.retry()
    }

    private companion object {
        const val TAG = "SyncWorker"
    }
}

/** Queues [SyncWorker] to run when the phone has a connection. */
object SyncScheduler {

    private const val WORK_NAME = "fitquest-sync"
    private const val BACKOFF_SECONDS = 30L

    /** KEEP means many failed pushes in a row still queue only one job. */
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
