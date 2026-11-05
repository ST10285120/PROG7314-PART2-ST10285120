package com.fitquest.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitquest.app.R
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.LoadingBlock
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.QuestHeroCard
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.ui.components.StatusChip
import com.fitquest.app.ui.components.muscleGroupName
import com.fitquest.app.util.WeightUnits
import com.fitquest.app.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The dashboard: progress hero, suggested workout, start button and recent history. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    profile: UserDto?,
    pendingXp: Int,
    units: String,
    recentWorkouts: List<WorkoutEntity>,
    onStartWorkout: () -> Unit,
    onClickSuggestedWorkout: () -> Unit,
    onClickStreak: () -> Unit,
    onClickWorkout: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    val hasSuggestion = !uiState.isLoadingSuggestion && uiState.suggestedExercises.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md)
    ) {
        item {
            QuestHeroCard(
                displayName = profile?.username,
                level = profile?.level ?: 1,
                xpTotal = profile?.xpTotal ?: 0,
                pendingXp = pendingXp,
                streak = profile?.currentStreak ?: 0,
                longestStreak = profile?.longestStreak ?: 0,
                onClick = onClickStreak
            )
        }

        item {
            FitQuestCard(onClick = if (hasSuggestion) onClickSuggestedWorkout else null) {
                Text(
                    stringResource(R.string.suggested_heading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                when {
                    uiState.isLoadingSuggestion -> LoadingBlock()
                    !hasSuggestion -> Text(
                        stringResource(R.string.suggested_unavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> {
                        Text(
                            stringResource(R.string.suggested_workout_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.suggested_reason),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            uiState.focusMuscleGroups.forEach { group ->
                                StatusChip(text = muscleGroupName(group))
                            }
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            uiState.suggestedExercises.joinToString(" · ") { it.name },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            stringResource(R.string.suggested_start, uiState.suggestedExercises.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = stringResource(R.string.start_workout),
                onClick = onStartWorkout,
                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        item { SectionHeader(stringResource(R.string.recent_workouts)) }
        if (recentWorkouts.isEmpty()) {
            item {
                FitQuestCard {
                    Text(
                        stringResource(R.string.no_workouts),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentWorkouts.take(RECENT_WORKOUT_COUNT), key = { it.localId }) { workout ->
                FitQuestCard(onClick = { onClickWorkout(workout.localId) }) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(workout.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                dateFormat.format(Date(workout.startedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            WeightUnits.format(workout.totalVolumeKg, units),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

private const val RECENT_WORKOUT_COUNT = 5
