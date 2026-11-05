package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fitquest.app.BuildConfig
import com.fitquest.app.R
import com.fitquest.app.data.local.dao.PersonalRecordRow
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.remote.dto.ProgressPhotoDto
import com.fitquest.app.data.repository.CompletionResult
import com.fitquest.app.ui.components.ConfettiOverlay
import com.fitquest.app.ui.components.ConfirmDialog
import com.fitquest.app.ui.components.EmptyState
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.LoadingBlock
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.ui.components.SplitBar
import com.fitquest.app.ui.components.StatsHero
import com.fitquest.app.ui.components.StatusChip
import com.fitquest.app.ui.components.exerciseName
import com.fitquest.app.ui.components.VolumeLineChart
import com.fitquest.app.ui.components.muscleGroupName
import com.fitquest.app.util.BadgeText
import com.fitquest.app.util.WeightUnits
import com.fitquest.app.viewmodel.ProgressUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WEEKS_SHOWN = 8

/**
 * The dashboard from the Part 1 design. Charts and records come from the API, so they
 * cover all devices; workout history comes from Room, which the API repopulates.
 */
@Composable
fun ProgressScreen(
    uiState: ProgressUiState,
    recentWorkouts: List<WorkoutEntity>,
    localRecords: List<PersonalRecordRow>,
    units: String,
    onClickWorkout: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    val progress = uiState.progress
    val totalVolumeKg = progress?.volumeByWeek?.values?.sum() ?: recentWorkouts.sumOf { it.totalVolumeKg }
    val serverRecords = progress?.prsByExercise.orEmpty().entries.sortedByDescending { it.value }
    val records = serverRecords.map { it.key to it.value }
        .ifEmpty { localRecords.map { it.exerciseName to it.weightKg } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (uiState.isOfflineData) {
            item { ErrorBanner(stringResource(R.string.offline_progress)) }
        }

        item {
            StatsHero(
                workouts = progress?.totalWorkouts?.takeIf { it > 0 } ?: recentWorkouts.size,
                volume = WeightUnits.format(totalVolumeKg, units),
                records = records.size
            )
        }

        if (uiState.isLoading) item { LoadingBlock() }

        item { SectionHeader(stringResource(R.string.volume_chart)) }
        item {
            FitQuestCard {
                val weeklyKg = progress?.volumeByWeek.orEmpty().toSortedMap().values.toList().takeLast(WEEKS_SHOWN)
                if (weeklyKg.isEmpty()) {
                    Text(stringResource(R.string.no_chart_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        stringResource(R.string.chart_peak, WeightUnits.format(weeklyKg.max(), units)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    VolumeLineChart(values = weeklyKg.map { WeightUnits.fromKg(it, units) })
                }
            }
        }

        item { SectionHeader(stringResource(R.string.muscle_split)) }
        item {
            FitQuestCard {
                val split = progress?.muscleGroupSplit.orEmpty().entries.sortedByDescending { it.value }
                val total = split.sumOf { it.value }
                if (total <= 0.0) {
                    Text(stringResource(R.string.no_chart_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    split.forEach { (group, volume) ->
                        SplitBar(label = muscleGroupName(group), fraction = (volume / total).toFloat())
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.personal_records)) }
        if (records.isEmpty()) {
            item {
                EmptyState(icon = Icons.Filled.EmojiEvents, message = stringResource(R.string.no_records))
            }
        } else {
            items(records, key = { it.first }) { (apiName, weightKg) ->
                FitQuestCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(exerciseName(apiName), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        StatusChip(
                            text = WeightUnits.format(weightKg, units),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(stringResource(R.string.progress_photos))
                TextButton(onClick = onAddPhoto, enabled = !uiState.isUploadingPhoto) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        stringResource(
                            if (uiState.isUploadingPhoto) R.string.uploading else R.string.add_photo
                        )
                    )
                }
            }
        }
        if (uiState.photoError) {
            item { ErrorBanner(stringResource(R.string.photo_error)) }
        }
        item {
            if (uiState.photos.isEmpty()) {
                EmptyState(icon = Icons.Filled.AddAPhoto, message = stringResource(R.string.no_photos))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(uiState.photos, key = { it.id }) { photo ->
                        PhotoTile(photo = photo, onDelete = { onDeletePhoto(photo.id) })
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.workout_history)) }
        if (recentWorkouts.isEmpty()) {
            item {
                EmptyState(icon = Icons.Filled.FitnessCenter, message = stringResource(R.string.no_history))
            }
        }
        items(recentWorkouts, key = { it.localId }) { workout ->
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
                            text = stringResource(R.string.pending_sync),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    stringResource(
                        R.string.history_line,
                        WeightUnits.format(workout.totalVolumeKg, units),
                        workout.xpEarned
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PhotoTile(photo: ProgressPhotoDto, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_photo),
            message = stringResource(R.string.delete_photo_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
    Box(modifier = Modifier.size(136.dp).clip(MaterialTheme.shapes.medium)) {
        AsyncImage(
            model = BuildConfig.API_BASE_URL + photo.path,
            contentDescription = stringResource(R.string.progress_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        FilledTonalIconButton(
            onClick = { confirmDelete = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp)
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete_photo),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Shown after finishing. If the numbers are not confirmed yet, it says they are an estimate. */
@Composable
fun WorkoutSummaryScreen(completion: CompletionResult?, onDone: () -> Unit) {
    val animatedXp by animateIntAsState(completion?.xpEarned ?: 0, tween(1000))

    // A short pop gives the end of a session the feel of completing a quest.
    val trophyScale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        trophyScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Box(modifier = Modifier.fillMaxSize()) {
    if (completion != null && !completion.pendingSync) ConfettiOverlay()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(72.dp).scale(trophyScale.value)
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(stringResource(R.string.workout_complete), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.lg))

        if (completion == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.saving))
            }
        } else {
            FitQuestCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            stringResource(R.string.xp_earned),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "+$animatedXp",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.streak),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.days_count, completion.streak),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            stringResource(R.string.level),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("${completion.newLevel}", style = MaterialTheme.typography.headlineSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.personal_records),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("${completion.personalRecords}", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                if (completion.leveledUp) {
                    Spacer(Modifier.height(Spacing.sm))
                    StatusChip(
                        text = stringResource(R.string.notification_level_title),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (completion.pendingSync) {
                    Spacer(Modifier.height(Spacing.md))
                    ErrorBanner(stringResource(R.string.estimate_notice))
                }
                if (completion.badgeNames.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.md))
                    Text(stringResource(R.string.new_badges), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(Spacing.xs))
                    completion.badgeNames.forEach { name ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(BadgeText.nameRes(name)?.let { stringResource(it) } ?: name)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(text = stringResource(R.string.done), onClick = onDone)
    }
    }
}
