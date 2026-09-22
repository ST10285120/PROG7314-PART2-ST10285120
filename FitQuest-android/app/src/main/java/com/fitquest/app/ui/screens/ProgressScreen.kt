package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitquest.app.data.local.dao.PersonalRecordRow
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.repository.CompletionResult
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.ui.components.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    recentWorkouts: List<WorkoutEntity>,
    personalRecords: List<PersonalRecordRow>,
    onClickWorkout: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FitQuestCard(modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("${recentWorkouts.size}", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Workouts logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FitQuestCard(modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(4.dp))
                    val totalXp = recentWorkouts.sumOf { it.xpEarned }
                    Text("$totalXp", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Total XP earned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { SectionHeader("Workout History") }
        if (recentWorkouts.isEmpty()) {
            item {
                FitQuestCard {
                    Text(
                        "No completed workouts yet — finish one to see it here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
                    if (!workout.synced) {
                        StatusChip(
                            text = "Pending sync",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "${"%.0f".format(workout.totalVolumeKg)} kg volume  ·  ${workout.xpEarned} XP",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item { Spacer(Modifier.height(Spacing.md)) }

        item { SectionHeader("Personal Records") }
        if (personalRecords.isEmpty()) {
            item {
                FitQuestCard {
                    Text(
                        "No personal records yet — your first set on any exercise becomes one automatically the next time you beat it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(personalRecords) { pr ->
                FitQuestCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(pr.exerciseName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                dateFormat.format(Date(pr.achievedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusChip(
                            text = "${pr.weightKg}kg × ${pr.reps}",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(Spacing.md)) }
    }
}

@Composable
fun WorkoutSummaryScreen(completion: CompletionResult?, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(Spacing.sm))
        Text("Workout Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.lg))

        if (completion == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Saving…")
            }
        } else {
            FitQuestCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("XP Earned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${completion.xpEarned}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${completion.streak} days", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                if (completion.pendingSync) {
                    Spacer(Modifier.height(Spacing.md))
                    ErrorBanner("Pending sync — these numbers are a local estimate and will update once you're back online.")
                }
                if (completion.badgeNames.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.md))
                    Text("New badges", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(Spacing.xs))
                    completion.badgeNames.forEach { name ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text(name)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(text = "Done", onClick = onDone)
    }
}
