package com.fitquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
