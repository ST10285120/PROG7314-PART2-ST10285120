package com.fitquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.LoadingBlock
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    recentWorkouts: List<WorkoutEntity>,
    currentStreak: Int,
    level: Int,
    xpTotal: Int,
    onStartWorkout: () -> Unit,
    onClickSuggestedWorkout: () -> Unit,
    onClickStreak: () -> Unit,
    onClickWorkout: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md)
    ) {
        item {
            FitQuestCard(containerColor = MaterialTheme.colorScheme.primaryContainer, onClick = onClickStreak) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(Spacing.sm))
                        Column {
                            Text(
                                "$currentStreak day streak",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Keep it going",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "Lv. $level",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                val progressWithinLevel = (xpTotal % 200) / 200f
                LinearProgressIndicator(
                    progress = { progressWithinLevel },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraSmall),
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "${xpTotal % 200} / 200 XP to next level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        item {
            FitQuestCard(onClick = if (uiState.isLoadingSuggestion || uiState.errorMessage != null) null else onClickSuggestedWorkout) {
                Text(
                    "TODAY'S SUGGESTED WORKOUT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                if (uiState.isLoadingSuggestion) {
                    LoadingBlock()
                } else if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    ErrorBanner(uiState.errorMessage)
                } else {
                    Text(uiState.suggestedTitle, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${uiState.suggestedExercises.size} exercises · Tap to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            PrimaryButton(
                text = "Start Workout",
                onClick = onStartWorkout,
                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        item { SectionHeader("Recent Workouts") }
        if (recentWorkouts.isEmpty()) {
            item {
                FitQuestCard {
                    Text(
                        "No workouts yet — start one above to see it here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentWorkouts) { workout ->
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
                            "${"%.0f".format(workout.totalVolumeKg)} kg",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}
