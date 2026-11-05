package com.fitquest.app.data.remote.dto

/** Settings stored on the server so they follow the user to a new device. */
data class PreferencesDto(
    val units: String,
    val theme: String,
    val reminderTime: String,
    val restTimerDefaultSec: Int
)
