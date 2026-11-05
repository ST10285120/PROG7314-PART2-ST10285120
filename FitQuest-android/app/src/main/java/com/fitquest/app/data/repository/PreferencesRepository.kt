package com.fitquest.app.data.repository

import android.util.Log
import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.PreferencesDto

/**
 * Settings held on the server, with a local copy so units and theme stay correct
 * offline. A change made offline is kept and sent the next time the API is reachable.
 */
class PreferencesRepository(
    private val api: FitQuestApi,
    private val localCache: LocalCache
) {

    suspend fun getPreferences(): Result<PreferencesDto> = try {
        pushPendingChanges()
        val response = api.getPreferences()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            localCache.savePreferences(body)
            Result.success(body)
        } else {
            cachedOr("Failed to load preferences (HTTP ${response.code()})")
        }
    } catch (e: Exception) {
        cachedOr(e.message ?: "Network error")
    }

    private fun cachedOr(message: String): Result<PreferencesDto> =
        localCache.getPreferences()?.let { Result.success(it) } ?: Result.failure(Exception(message))

    private suspend fun pushPendingChanges() {
        if (!localCache.hasPendingPreferences) return
        val pending = localCache.getPreferences() ?: return
        if (api.updatePreferences(pending).isSuccessful) {
            localCache.hasPendingPreferences = false
            Log.i(TAG, "Offline settings changes uploaded")
        }
    }

    suspend fun updatePreferences(preferences: PreferencesDto): Result<PreferencesDto> {
        localCache.savePreferences(preferences)
        return try {
            val response = api.updatePreferences(preferences)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                localCache.savePreferences(body)
                localCache.hasPendingPreferences = false
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to update preferences (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Settings saved offline: ${e.message}")
            localCache.hasPendingPreferences = true
            Result.success(preferences)
        }
    }

    private companion object {
        const val TAG = "PreferencesRepository"
    }
}
