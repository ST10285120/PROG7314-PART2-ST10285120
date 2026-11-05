package com.fitquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A workout on this phone. localId is a UUID created before any network call and is
 * sent as the clientId, so synced = false simply means "the server may be behind".
 */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val localId: String,
    val serverId: String? = null,
    val title: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val totalVolumeKg: Double = 0.0,
    val xpEarned: Int = 0,
    val synced: Boolean = false
)
