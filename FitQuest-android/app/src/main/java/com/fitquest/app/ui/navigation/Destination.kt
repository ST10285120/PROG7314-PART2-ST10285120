package com.fitquest.app.ui.navigation

sealed class Destination(val route: String) {
    data object Splash : Destination("splash")
    data object SsoSignIn : Destination("sso_sign_in")
    data object Home : Destination("home")
    data object ExercisePicker : Destination("exercise_picker")
    data object ActiveWorkout : Destination("active_workout")
    data object WorkoutSummary : Destination("workout_summary")
    data object Progress : Destination("progress")
    data object Badges : Destination("badges")
    data object Profile : Destination("profile")
    data object Settings : Destination("settings")

    data object WorkoutDetail : Destination("workout_detail/{workoutLocalId}") {
        const val ARG_WORKOUT_LOCAL_ID = "workoutLocalId"
        fun createRoute(workoutLocalId: String) = "workout_detail/$workoutLocalId"
    }
}
