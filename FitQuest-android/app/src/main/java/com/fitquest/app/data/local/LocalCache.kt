package com.fitquest.app.data.local

import android.content.Context
import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.data.remote.dto.PreferencesDto
import com.fitquest.app.data.remote.dto.SuggestedWorkoutResponse
import com.fitquest.app.data.remote.dto.UserDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Small non-secret cache for what the app needs with no connection: the exercise
 * library (without it a workout could not start offline), the last suggestion and the
 * last known profile. Also holds on-device settings.
 */
class LocalCache(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /** True when settings changed offline and the server has not received them yet. */
    var hasPendingPreferences: Boolean
        get() = prefs.getBoolean(KEY_PREFERENCES_PENDING, false)
        set(value) = prefs.edit().putBoolean(KEY_PREFERENCES_PENDING, value).apply()

    fun savePreferences(preferences: PreferencesDto) = putJson(KEY_PREFERENCES, preferences)

    fun getPreferences(): PreferencesDto? =
        readJson(KEY_PREFERENCES) { gson.fromJson(it, PreferencesDto::class.java) }

    fun saveExercises(exercises: List<ExerciseDto>) = putJson(KEY_EXERCISES, exercises)

    fun getExercises(): List<ExerciseDto>? {
        val type = object : TypeToken<List<ExerciseDto>>() {}.type
        return readJson(KEY_EXERCISES) { gson.fromJson<List<ExerciseDto>>(it, type) }
    }

    fun saveSuggestion(suggestion: SuggestedWorkoutResponse) = putJson(KEY_SUGGESTION, suggestion)

    fun getSuggestion(): SuggestedWorkoutResponse? =
        readJson(KEY_SUGGESTION) { gson.fromJson(it, SuggestedWorkoutResponse::class.java) }

    fun saveProfile(profile: UserDto) = putJson(KEY_PROFILE, profile)

    fun getProfile(): UserDto? = readJson(KEY_PROFILE) { gson.fromJson(it, UserDto::class.java) }

    /** Removes user data on sign-out. Device settings stay. */
    fun clearUserData() {
        prefs.edit()
            .remove(KEY_EXERCISES)
            .remove(KEY_SUGGESTION)
            .remove(KEY_PROFILE)
            .remove(KEY_PREFERENCES)
            .remove(KEY_PREFERENCES_PENDING)
            .apply()
    }

    private fun putJson(key: String, value: Any) {
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }

    /** A cache must never crash the app, so unreadable data counts as a miss. */
    private fun <T> readJson(key: String, parse: (String) -> T?): T? {
        val json = prefs.getString(key, null) ?: return null
        return runCatching { parse(json) }.getOrNull()
    }

    private companion object {
        const val FILE_NAME = "fitquest_cache"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_EXERCISES = "exercises"
        const val KEY_SUGGESTION = "suggestion"
        const val KEY_PROFILE = "profile"
        const val KEY_PREFERENCES = "preferences"
        const val KEY_PREFERENCES_PENDING = "preferences_pending"
    }
}
