package com.fitquest.app.data.remote

import android.util.Log
import com.fitquest.app.BuildConfig
import com.fitquest.app.data.local.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Tells the UI when the server has rejected the saved JWT. */
object SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}

/**
 * Adds the JWT to each request. A 401 on a request that sent a token means it has
 * expired, so the token is cleared and the UI returns to sign-in instead of leaving
 * the user looking signed in while every call fails. Room data is not touched, so
 * unsynced workouts survive and upload after the next sign-in.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStore.getToken()
        val request = if (token != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }
        val response = chain.proceed(request)

        if (response.code == 401 && token != null && !original.url.encodedPath.endsWith("auth/sso")) {
            Log.w(TAG, "Server rejected the saved token; ending the session")
            tokenStore.clearToken()
            SessionEvents.notifyExpired()
        }
        return response
    }

    private companion object {
        const val TAG = "AuthInterceptor"
    }
}

/** Builds the shared HTTP client and the Retrofit API. */
object RetrofitClient {

    /** The API can take close to a minute to wake on Render's free tier. */
    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 60L

    /**
     * Shared HTTP client, also used by Coil so that private photos are requested with
     * the JWT. Logging is limited to debug builds and never prints the Authorization
     * header, because logcat must not contain tokens.
     */
    fun createHttpClient(tokenStore: TokenStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .build()
    }

    fun create(baseUrl: String, client: OkHttpClient): FitQuestApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FitQuestApi::class.java)
    }
}
