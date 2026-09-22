package com.fitquest.app.data.remote.dto

data class PreferencesDto(
    val units: String,
    val theme: String,
    val reminderTime: String,
    val restTimerDefaultSec: Int
)
