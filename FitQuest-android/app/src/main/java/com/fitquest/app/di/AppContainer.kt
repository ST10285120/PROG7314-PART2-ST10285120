package com.fitquest.app.di

import android.content.Context
import com.fitquest.app.BuildConfig
import com.fitquest.app.auth.GitHubOAuthManager
import com.fitquest.app.auth.GoogleSignInManager
import com.fitquest.app.data.local.FitQuestDatabase
import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.local.TokenStore
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.PhotoRepository
import com.fitquest.app.data.repository.PreferencesRepository
import com.fitquest.app.data.repository.ProfileRepository
import com.fitquest.app.data.repository.WorkoutRepository
import com.fitquest.app.sync.ConnectivityObserver
import com.fitquest.app.sync.SyncScheduler
import okhttp3.OkHttpClient

/** Manual dependency injection. Each object is created once, the first time it is used. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val tokenStore: TokenStore by lazy { TokenStore(appContext) }
    val localCache: LocalCache by lazy { LocalCache(appContext) }

    val httpClient: OkHttpClient by lazy { RetrofitClient.createHttpClient(tokenStore) }
    val api: FitQuestApi by lazy { RetrofitClient.create(BuildConfig.API_BASE_URL, httpClient) }

    private val database: FitQuestDatabase by lazy { FitQuestDatabase.getInstance(appContext) }

    val googleSignInManager: GoogleSignInManager by lazy { GoogleSignInManager(appContext) }
    val gitHubOAuthManager: GitHubOAuthManager by lazy { GitHubOAuthManager(appContext, tokenStore) }
    val connectivityObserver: ConnectivityObserver by lazy { ConnectivityObserver(appContext) }

    val authRepository: AuthRepository by lazy { AuthRepository(api, tokenStore) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(api) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(api, localCache) }
    val photoRepository: PhotoRepository by lazy { PhotoRepository(api) }
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(
            api = api,
            workoutDao = database.workoutDao(),
            workoutExerciseDao = database.workoutExerciseDao(),
            setEntryDao = database.setEntryDao(),
            localCache = localCache,
            profileRepository = profileRepository,
            onSyncDeferred = { SyncScheduler.enqueue(appContext) }
        )
    }
}
