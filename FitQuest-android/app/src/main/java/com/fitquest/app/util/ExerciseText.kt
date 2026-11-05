package com.fitquest.app.util

import com.fitquest.app.R

/**
 * The exercise library arrives from the API in English. This maps each seeded exercise
 * and equipment type to a string resource for display. Stored data keeps the English
 * name, so language changes never alter a workout. Unknown names use the API value.
 */
object ExerciseText {

    fun nameRes(apiName: String): Int? = when (apiName) {
        "Barbell Bench Press" -> R.string.exercise_bench_press
        "Incline Dumbbell Press" -> R.string.exercise_incline_press
        "Push-Up" -> R.string.exercise_push_up
        "Barbell Back Squat" -> R.string.exercise_back_squat
        "Romanian Deadlift" -> R.string.exercise_romanian_deadlift
        "Walking Lunge" -> R.string.exercise_walking_lunge
        "Deadlift" -> R.string.exercise_deadlift
        "Pull-Up" -> R.string.exercise_pull_up
        "Bent-Over Row" -> R.string.exercise_bent_over_row
        "Overhead Press" -> R.string.exercise_overhead_press
        "Lateral Raise" -> R.string.exercise_lateral_raise
        "Barbell Curl" -> R.string.exercise_barbell_curl
        "Tricep Dip" -> R.string.exercise_tricep_dip
        "Plank" -> R.string.exercise_plank
        "Hanging Leg Raise" -> R.string.exercise_hanging_leg_raise
        else -> null
    }

    fun equipmentRes(apiName: String): Int? = when (apiName) {
        "Barbell" -> R.string.equipment_barbell
        "Dumbbell" -> R.string.equipment_dumbbell
        "Bodyweight" -> R.string.equipment_bodyweight
        else -> null
    }
}
