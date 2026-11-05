package com.fitquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** An exercise within a workout. The name is stored so history displays offline. */
@Entity(
    tableName = "workout_exercises",
    indices = [Index("workoutLocalId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["localId"],
            childColumns = ["workoutLocalId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkoutExerciseEntity(
    @PrimaryKey val localId: String,
    val serverId: String? = null,
    val workoutLocalId: String,
    val exerciseId: String,
    val exerciseName: String,
    val orderIndex: Int
)

/** A logged set, always in kg. isPersonalRecord is an estimate until the server confirms it. */
@Entity(
    tableName = "set_entries",
    indices = [Index("workoutExerciseLocalId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["localId"],
            childColumns = ["workoutExerciseLocalId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SetEntryEntity(
    @PrimaryKey val localId: String,
    val workoutExerciseLocalId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Int? = null,
    val isPersonalRecord: Boolean = false
)
