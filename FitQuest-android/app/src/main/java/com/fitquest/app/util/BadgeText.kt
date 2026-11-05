package com.fitquest.app.util

import com.fitquest.app.R

/**
 * Badge names arrive from the API in English. This maps each seeded badge to a string
 * resource so it can be shown in the user's language. Unknown badges use the API name.
 */
object BadgeText {
    fun nameRes(apiName: String): Int? = when (apiName) {
        "3-Day Streak" -> R.string.badge_streak_3
        "7-Day Streak" -> R.string.badge_streak_7
        "30-Day Streak" -> R.string.badge_streak_30
        "First Steps" -> R.string.badge_first_steps
        "10 Workouts" -> R.string.badge_workouts_10
        "50 Workouts" -> R.string.badge_workouts_50
        "First PR" -> R.string.badge_first_pr
        "10 PRs" -> R.string.badge_prs_10
        else -> null
    }

    fun descriptionRes(apiName: String): Int? = when (apiName) {
        "3-Day Streak" -> R.string.badge_desc_streak_3
        "7-Day Streak" -> R.string.badge_desc_streak_7
        "30-Day Streak" -> R.string.badge_desc_streak_30
        "First Steps" -> R.string.badge_desc_first_steps
        "10 Workouts" -> R.string.badge_desc_workouts_10
        "50 Workouts" -> R.string.badge_desc_workouts_50
        "First PR" -> R.string.badge_desc_first_pr
        "10 PRs" -> R.string.badge_desc_prs_10
        else -> null
    }
}
