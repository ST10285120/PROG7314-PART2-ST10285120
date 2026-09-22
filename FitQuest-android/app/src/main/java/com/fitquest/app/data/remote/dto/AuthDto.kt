package com.fitquest.app.data.remote.dto

data class SsoRequest(
    val provider: String,
    val idToken: String? = null,
    val code: String? = null
)

data class SsoResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val xpTotal: Int,
    val level: Int,
    val currentStreak: Int
)
