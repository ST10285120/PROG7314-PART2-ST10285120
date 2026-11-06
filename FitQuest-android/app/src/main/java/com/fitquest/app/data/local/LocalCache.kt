package com.fitquest.app.data.local

import android.content.Context
import com.fitquest.app.data.remote.dto.BadgeDto
import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.data.remote.dto.PreferencesDto
import com.fitquest.app.data.remote.dto.ProgressResponse
import com.fitquest.app.data.remote.dto.RoutineDto
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

    /**
     * When records last started again, for the phone's PR estimate. A reinstalled phone
     * loses this, but the server still decides, so only the estimate is affected.
     */
    var recordsResetAt: Long
        get() = prefs.getLong(KEY_RECORDS_RESET_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_RECORDS_RESET_AT, value).apply()

    fun savePreferences(preferences: PreferencesDto) = putJson(KEY_PREFERENCES, preferences)

    fun getPreferences(): PreferencesDto? =
        readJson(KEY_PREFERENCES) { gson.fromJson(it, PreferencesDto::class.java) }

    /** True when routines changed offline and the server has not received them yet. */
    var hasPendingRoutines: Boolean
        get() = prefs.getBoolean(KEY_ROUTINES_PENDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ROUTINES_PENDING, value).apply()

    fun saveRoutines(routines: List<RoutineDto>) = putJson(KEY_ROUTINES, routines)

    fun getRoutines(): List<RoutineDto>? {
        val type = object : TypeToken<List<RoutineDto>>() {}.type
        return readJson(KEY_ROUTINES) { gson.fromJson<List<RoutineDto>>(it, type) }
    }

    fun saveBadges(badges: List<BadgeDto>) = putJson(KEY_BADGES, badges)

    fun getBadges(): List<BadgeDto>? {
        val type = object : TypeToken<List<BadgeDto>>() {}.type
        return readJson(KEY_BADGES) { gson.fromJson<List<BadgeDto>>(it, type) }
    }

    fun saveProgress(progress: ProgressResponse) = putJson(KEY_PROGRESS, progress)

    fun getProgress(): ProgressResponse? =
        readJson(KEY_PROGRESS) { gson.fromJson(it, ProgressResponse::class.java) }

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
            .remove(KEY_ROUTINES)
            .remove(KEY_ROUTINES_PENDING)
            .remove(KEY_BADGES)
            .remove(KEY_PROGRESS)
            .remove(KEY_RECORDS_RESET_AT)
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
        const val KEY_ROUTINES = "routines"
        const val KEY_ROUTINES_PENDING = "routines_pending"
        const val KEY_BADGES = "badges"
        const val KEY_PROGRESS = "progress"
        const val KEY_RECORDS_RESET_AT = "records_reset_at"
        const val KEY_PREFERENCES = "preferences"
        const val KEY_PREFERENCES_PENDING = "preferences_pending"
    }
}
