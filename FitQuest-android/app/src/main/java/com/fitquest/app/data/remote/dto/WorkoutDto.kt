package com.fitquest.app.data.remote.dto

data class ExerciseDto(
    val _id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val type: String
)

data class CreateWorkoutRequest(val title: String, val startedAt: String? = null)
data class CreateWorkoutResponse(val workoutId: String)

data class AddExerciseRequest(val exerciseId: String, val orderIndex: Int)
data class AddExerciseResponse(val workoutExerciseId: String)

data class LogSetRequest(
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Int? = null
)
data class LogSetResponse(val setId: String, val isPersonalRecord: Boolean)

data class CompleteWorkoutRequest(val completedAt: String? = null)
data class CompleteWorkoutResponse(
    val xpEarned: Int,
    val newLevel: Int,
    val streak: Int,
    val badgesEarned: List<BadgeDto>
)

data class BadgeDto(
    val badgeId: String,
    val name: String,
    val description: String,
    val earnedAt: String? = null
)

data class SuggestedWorkoutExerciseDto(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String
)
data class SuggestedWorkoutResponse(
    val title: String,
    val exercises: List<SuggestedWorkoutExerciseDto>
)

data class ProgressResponse(
    val volumeByWeek: Map<String, Double>,
    val prsByExercise: Map<String, Double>,
    val muscleGroupSplit: Map<String, Double>
)
