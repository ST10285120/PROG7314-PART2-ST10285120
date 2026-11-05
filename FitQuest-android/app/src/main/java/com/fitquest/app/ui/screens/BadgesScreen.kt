package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitquest.app.R
import com.fitquest.app.data.remote.dto.BadgeDto
import com.fitquest.app.ui.components.BadgeMedal
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.LoadingBlock
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.util.BadgeText
import com.fitquest.app.viewmodel.BadgesUiState

/** The streak plus a trophy cabinet of earned medals and locked goals with progress. */
@Composable
fun BadgesScreen(uiState: BadgesUiState, currentStreak: Int, longestStreak: Int) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FitQuestCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            stringResource(R.string.streak_days, currentStreak),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            stringResource(R.string.streak_best, longestStreak),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            stringResource(R.string.streak_keep_alive),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        when {
            uiState.isLoading && uiState.badges.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { LoadingBlock() }
            uiState.errorMessage != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorBanner(stringResource(R.string.badges_error))
            }
            else -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        stringResource(
                            R.string.earned_badges,
                            uiState.badges.count { it.earnedAt != null },
                            uiState.badges.size
                        )
                    )
                }
                items(uiState.badges, key = { it.badgeId }) { badge -> BadgeTile(badge) }
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: BadgeDto) {
    val earned = badge.earnedAt != null
    FitQuestCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            BadgeMedal(earned = earned, size = 64.dp)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                BadgeText.nameRes(badge.name)?.let { stringResource(it) } ?: badge.name,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            BadgeText.descriptionRes(badge.name)?.let { descriptionRes ->
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (!earned && badge.target > 0) {
                Spacer(Modifier.height(Spacing.xs))
                LinearProgressIndicator(
                    progress = { badge.progress / badge.target.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.badge_progress, badge.progress, badge.target),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private const val GRID_COLUMNS = 2
