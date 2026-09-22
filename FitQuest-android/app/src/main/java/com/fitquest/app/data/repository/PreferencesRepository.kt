package com.fitquest.app.data.repository

import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.PreferencesDto

class PreferencesRepository(private val api: FitQuestApi) {

    suspend fun getPreferences(): Result<PreferencesDto> = try {
        val response = api.getPreferences()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to load preferences (HTTP ${response.code()})"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updatePreferences(preferences: PreferencesDto): Result<PreferencesDto> = try {
        val response = api.updatePreferences(preferences)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to update preferences (HTTP ${response.code()})"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
