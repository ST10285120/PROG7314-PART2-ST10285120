package com.fitquest.app.di

import android.content.Context
import com.fitquest.app.BuildConfig
import com.fitquest.app.auth.GitHubOAuthManager
import com.fitquest.app.auth.GoogleSignInManager
import com.fitquest.app.data.local.FitQuestDatabase
import com.fitquest.app.data.local.TokenStore
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.PreferencesRepository
import com.fitquest.app.data.repository.WorkoutRepository

class AppContainer(context: Context) {

    val tokenStore: TokenStore by lazy { TokenStore(context) }

    val api: FitQuestApi by lazy { RetrofitClient.create(BuildConfig.API_BASE_URL, tokenStore) }

    private val database: FitQuestDatabase by lazy { FitQuestDatabase.getInstance(context) }

    val googleSignInManager: GoogleSignInManager by lazy { GoogleSignInManager(context) }
    val gitHubOAuthManager: GitHubOAuthManager by lazy { GitHubOAuthManager(context) }

    val authRepository: AuthRepository by lazy { AuthRepository(api, tokenStore) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(api) }
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(api, database.workoutDao(), database.workoutExerciseDao(), database.setEntryDao())
    }
}
