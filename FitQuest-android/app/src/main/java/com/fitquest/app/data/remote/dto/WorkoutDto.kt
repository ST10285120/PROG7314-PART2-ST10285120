package com.fitquest.app.data.remote.dto

/** An entry in the shared exercise library. */
data class ExerciseDto(
    val _id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val type: String
)

/**
 * A whole workout sent to POST /api/workouts/sync. The clientIds are the UUIDs created
 * on this phone, which is what makes a replayed request safe (Part 1, "Idempotent sync").
 */
data class SyncWorkoutRequest(
    val clientId: String,
    val title: String,
    val startedAt: String,
    val completedAt: String?,
    val exercises: List<SyncExerciseDto>
)

data class SyncExerciseDto(
    val clientId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val sets: List<SyncSetDto>
)

data class SyncSetDto(
    val clientId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Int?
)

/** The server's view after a sync. completion is null until the workout is finished. */
data class SyncWorkoutResponse(
    val workoutId: String,
    val completion: CompletionDto?,
    val sets: List<SyncedSetDto>
)

/** The authoritative PR decision for one set. */
data class SyncedSetDto(val clientId: String, val isPersonalRecord: Boolean)

/** Gamification results calculated by the API. */
data class CompletionDto(
    val xpEarned: Int,
    val newLevel: Int,
    val streak: Int,
    val badgesEarned: List<BadgeDto>
)

/** A badge. target and progress are filled by the catalog, not the completion response. */
data class BadgeDto(
    val badgeId: String,
    val name: String,
    val description: String,
    val earnedAt: String? = null,
    val target: Int = 0,
    val progress: Int = 0
)

data class SuggestedWorkoutExerciseDto(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String
)

/** A rules-based suggestion built around the most rested muscle groups. */
data class SuggestedWorkoutResponse(
    val title: String,
    val exercises: List<SuggestedWorkoutExerciseDto>,
    val focusMuscleGroups: List<String> = emptyList()
)

/** Dashboard data from GET /api/users/me/progress, covering every device. */
data class ProgressResponse(
    val volumeByWeek: Map<String, Double>,
    val prsByExercise: Map<String, Double>,
    val muscleGroupSplit: Map<String, Double>,
    val totalWorkouts: Int = 0
)

/** A completed workout from the server, used to rebuild history on a new phone. */
data class HistoryWorkoutDto(
    val id: String,
    val clientId: String?,
    val title: String,
    val startedAt: String,
    val completedAt: String,
    val totalVolumeKg: Double,
    val xpEarned: Int,
    val exercises: List<HistoryExerciseDto>
)

data class HistoryExerciseDto(
    val id: String,
    val clientId: String?,
    val exerciseId: String,
    val name: String,
    val orderIndex: Int,
    val sets: List<HistorySetDto>
)

data class HistorySetDto(
    val id: String,
    val clientId: String?,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Int?,
    val isPersonalRecord: Boolean
)

/** A progress photo. path is relative to the API base URL. */
data class ProgressPhotoDto(
    val id: String,
    val path: String,
    val takenAt: String
)
