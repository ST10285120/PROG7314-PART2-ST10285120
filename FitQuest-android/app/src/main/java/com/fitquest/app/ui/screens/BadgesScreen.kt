package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.LoadingBlock
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.viewmodel.BadgesUiState

@Composable
fun BadgesScreen(uiState: BadgesUiState, currentStreak: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            FitQuestCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            "$currentStreak day streak",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Log a workout today to keep it alive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        item { SectionHeader("Earned Badges") }

        if (uiState.isLoading) {
            item { LoadingBlock() }
        } else if (uiState.errorMessage != null) {
            item { ErrorBanner(uiState.errorMessage) }
        } else if (uiState.badges.isEmpty()) {
            item {
                FitQuestCard {
                    Text(
                        "No badges yet — complete workouts and build a streak to start earning them.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.badges) { badge ->
                FitQuestCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Column {
                            Text(badge.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                badge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}
