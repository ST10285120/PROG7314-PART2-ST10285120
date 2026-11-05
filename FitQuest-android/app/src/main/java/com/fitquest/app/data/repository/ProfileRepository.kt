package com.fitquest.app.data.repository

import android.util.Log
import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.DeviceTokenRequest
import com.fitquest.app.data.remote.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * The single source of level, XP and streak for every screen. Values always come from
 * the API; the cache only provides the last known copy while offline.
 */
class ProfileRepository(
    private val api: FitQuestApi,
    private val localCache: LocalCache
) {

    private val _profile = MutableStateFlow(localCache.getProfile())
    val profile: StateFlow<UserDto?> = _profile

    fun setProfile(user: UserDto) {
        localCache.saveProfile(user)
        _profile.value = user
    }

    /** Failures are not reported: the cached profile is a correct offline answer. */
    suspend fun refresh() {
        try {
            val response = api.getProfile()
            response.body()?.takeIf { response.isSuccessful }?.let(::setProfile)
        } catch (e: Exception) {
            Log.d(TAG, "Profile refresh skipped: ${e.message}")
        }
    }

    suspend fun uploadAvatar(jpegBytes: ByteArray): Result<UserDto> = try {
        val response = api.uploadAvatar(jpegBytes.toRequestBody(JPEG))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            setProfile(body)
            Result.success(body)
        } else {
            Result.failure(Exception("Upload failed (HTTP ${response.code()})"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Avatar upload failed", e)
        Result.failure(e)
    }

    suspend fun registerDeviceToken(token: String) {
        try {
            api.registerDeviceToken(DeviceTokenRequest(token))
            Log.i(TAG, "Device token registered")
        } catch (e: Exception) {
            Log.w(TAG, "Device token registration failed: ${e.message}")
        }
    }

    suspend fun sendTestNotification(): Result<Int> = try {
        val response = api.sendTestNotification()
        val body = response.body()
        if (response.isSuccessful && body != null) Result.success(body.delivered)
        else Result.failure(Exception("HTTP ${response.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun clear() {
        localCache.clearUserData()
        _profile.value = null
    }

    private companion object {
        const val TAG = "ProfileRepository"
        val JPEG = "image/jpeg".toMediaType()
    }
}
