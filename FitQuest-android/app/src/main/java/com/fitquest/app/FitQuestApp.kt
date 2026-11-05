package com.fitquest.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.fitquest.app.di.AppContainer
import com.fitquest.app.notifications.NotificationHelper

/** Application entry point. Creates the dependency container and notification channels. */
class FitQuestApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application started, version ${BuildConfig.VERSION_NAME}")
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
    }

    /** Coil shares the API's HTTP client so private photos are requested with the JWT. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(container.httpClient)
            .crossfade(true)
            .build()

    private companion object {
        const val TAG = "FitQuestApp"
    }
}
