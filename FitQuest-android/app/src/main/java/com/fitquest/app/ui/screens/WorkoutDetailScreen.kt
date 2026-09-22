package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.LoadingBlock
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.ui.components.StatusChip
import com.fitquest.app.viewmodel.WorkoutDetailUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutDetailScreen(uiState: WorkoutDetailUiState) {
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM · h:mm a", Locale.getDefault()) }

    if (uiState.isLoading) {
        LoadingBlock(modifier = Modifier.fillMaxSize())
        return
    }

    val detail = uiState.detail
    if (detail == null) {
        Box(modifier = Modifier.fillMaxSize().padding(Spacing.lg), contentAlignment = Alignment.Center) {
            Text("Couldn't find that workout.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            FitQuestCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    detail.workout.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    dateFormat.format(Date(detail.workout.startedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    Column {
                        Text(
                            "${"%.0f".format(detail.workout.totalVolumeKg)} kg",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Total volume",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            "${detail.workout.xpEarned}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "XP earned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        item { SectionHeader("Exercises") }

        items(detail.exercises) { exercise ->
            FitQuestCard {
                Text(exercise.exerciseName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                exercise.sets.forEach { set ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Set ${set.setNumber} — ${set.weightKg}kg × ${set.reps}")
                        if (set.isPersonalRecord) {
                            StatusChip(
                                text = "PR",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}
