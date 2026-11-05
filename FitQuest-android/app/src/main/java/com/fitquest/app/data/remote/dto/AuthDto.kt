package com.fitquest.app.data.remote.dto

/** Body of POST /api/auth/sso. Google sends idToken; GitHub sends an authorization code. */
data class SsoRequest(
    val provider: String,
    val idToken: String? = null,
    val code: String? = null
)

/** A FitQuest JWT plus the signed-in user. */
data class SsoResponse(
    val token: String,
    val user: UserDto
)

/** The user's server-authoritative profile. Level, XP and streak are never calculated on the phone. */
data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val xpTotal: Int,
    val level: Int,
    val currentStreak: Int,
    val longestStreak: Int = 0,
    val lastWorkoutDate: String? = null,
    val avatarPath: String? = null
)

/** Registers this device's FCM token for real-time notifications. */
data class DeviceTokenRequest(val token: String)

/** How many devices accepted a test notification. */
data class TestNotificationResponse(val delivered: Int)
