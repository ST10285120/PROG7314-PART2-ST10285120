package com.fitquest.app

import android.app.Application
import com.fitquest.app.di.AppContainer

class FitQuestApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
