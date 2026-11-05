package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitquest.app.R
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.ui.components.ExerciseSelectionList
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.RestTimerRing
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.ui.components.StatusChip
import com.fitquest.app.ui.components.WorkoutHero
import com.fitquest.app.ui.components.exerciseName
import com.fitquest.app.util.WeightUnits
import kotlinx.coroutines.delay

private const val MIN_REPS = 1
private const val MAX_REPS = 1000
private const val MAX_WEIGHT_KG = 1000.0
private const val WEIGHT_STEP_KG = 2.5
private const val WEIGHT_STEP_LB = 5.0

/**
 * The live logging screen, designed for one thumb between sets: large steppers, a
 * rest timer that starts itself, and validation that blocks impossible values.
 */
@Composable
fun ActiveWorkoutScreen(
    exerciseLibrary: List<ExerciseDto>,
    currentExercise: WorkoutExerciseEntity?,
    exercisePosition: Int,
    exerciseCount: Int,
    nextExerciseName: String?,
    setsForCurrentExercise: List<SetEntryEntity>,
    totalSets: Int,
    workoutStartedAt: Long?,
    units: String,
    restTimerDefaultSec: Int = 90,
    onPickExercises: (List<ExerciseDto>) -> Unit,
    onLogSet: (reps: Int, weightKg: Double, rpe: Int?) -> Unit,
    onDeleteSet: (SetEntryEntity) -> Unit,
    onNextExercise: () -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit
) {
    var reps by rememberSaveable { mutableStateOf("8") }
    var weight by rememberSaveable(units) { mutableStateOf(if (units == WeightUnits.POUNDS) "45" else "20") }
    var showPicker by remember { mutableStateOf(currentExercise == null) }
    LaunchedEffect(currentExercise?.localId) {
        if (currentExercise != null) showPicker = false
    }

    val haptics = LocalHapticFeedback.current
    var restTimerTrigger by remember { mutableIntStateOf(0) }
    var remainingRestSeconds by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(restTimerTrigger) {
        if (restTimerTrigger == 0) return@LaunchedEffect
        // Counting to a fixed end time keeps the timer right if the app is backgrounded.
        val endsAt = System.currentTimeMillis() + restTimerDefaultSec * 1000L
        remainingRestSeconds = restTimerDefaultSec
        while ((remainingRestSeconds ?: 0) > 0) {
            delay(250)
            if ((remainingRestSeconds ?: 0) > 0) {
                remainingRestSeconds = ((endsAt - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
            }
        }
        remainingRestSeconds = null
        // The phone may be in a pocket or on a bench, so rest ending can be felt.
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(workoutStartedAt) {
        if (workoutStartedAt == null) return@LaunchedEffect
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - workoutStartedAt) / 1000).toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    val repsValue = reps.toIntOrNull()
    val weightValue = WeightUnits.parse(weight)
    val weightKg = weightValue?.let { WeightUnits.toKg(it, units) }
    val isSetValid = repsValue != null && repsValue in MIN_REPS..MAX_REPS &&
        weightKg != null && weightKg in 0.0..MAX_WEIGHT_KG
    val unitLabel = WeightUnits.label(units)
    val weightStep = if (units == WeightUnits.POUNDS) WEIGHT_STEP_LB else WEIGHT_STEP_KG

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
        val elapsedText = "%d:%02d:%02d".format(elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60, elapsedSeconds % 60)
        if (workoutStartedAt != null && (showPicker || currentExercise == null)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(elapsedText, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (showPicker || currentExercise == null) {
            ExercisePicker(
                exerciseLibrary = exerciseLibrary,
                modifier = Modifier.weight(1f),
                onCancel = if (currentExercise != null) ({ showPicker = false }) else null,
                onConfirm = { selected ->
                    onPickExercises(selected)
                    showPicker = false
                }
            )
        } else {
            WorkoutHero(
                exerciseName = exerciseName(currentExercise.exerciseName),
                position = exercisePosition,
                total = exerciseCount,
                setsLogged = setsForCurrentExercise.size,
                elapsed = elapsedText
            )
            Spacer(Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StepperField(
                    value = reps,
                    label = stringResource(R.string.reps),
                    keyboardType = KeyboardType.Number,
                    isError = repsValue == null || repsValue !in MIN_REPS..MAX_REPS,
                    modifier = Modifier.weight(1f),
                    onValueChange = { reps = it.take(4) },
                    onDecrease = { reps = ((repsValue ?: MIN_REPS) - 1).coerceAtLeast(MIN_REPS).toString() },
                    onIncrease = { reps = ((repsValue ?: 0) + 1).coerceAtMost(MAX_REPS).toString() }
                )
                StepperField(
                    value = weight,
                    label = stringResource(R.string.weight_label, unitLabel),
                    keyboardType = KeyboardType.Decimal,
                    isError = weightKg == null || weightKg !in 0.0..MAX_WEIGHT_KG,
                    modifier = Modifier.weight(1f),
                    onValueChange = { weight = it.take(7) },
                    onDecrease = { weight = stepLabel(((weightValue ?: 0.0) - weightStep).coerceAtLeast(0.0)) },
                    onIncrease = { weight = stepLabel((weightValue ?: 0.0) + weightStep) }
                )
            }

            setsForCurrentExercise.lastOrNull()?.let { last ->
                TextButton(
                    onClick = {
                        reps = last.reps.toString()
                        weight = WeightUnits.number(last.weightKg, units)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.same_as_last)) }
            }

            if (!isSetValid) {
                Text(
                    stringResource(R.string.error_invalid_set),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(text = stringResource(R.string.log_set), enabled = isSetValid) {
                if (repsValue != null && weightKg != null) {
                    onLogSet(repsValue, weightKg, null)
                    restTimerTrigger += 1
                }
            }

            remainingRestSeconds?.let { seconds ->
                Spacer(Modifier.height(Spacing.sm))
                RestTimerRing(
                    seconds = seconds,
                    totalSeconds = restTimerDefaultSec,
                    onSkip = { remainingRestSeconds = 0 }
                )
            }

            if (nextExerciseName != null) {
                Spacer(Modifier.height(Spacing.sm))
                Button(
                    onClick = onNextExercise,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.next_exercise))
                    Spacer(Modifier.width(Spacing.sm))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    stringResource(R.string.up_next, exerciseName(nextExerciseName)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            SectionHeader(stringResource(R.string.logged_sets))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                items(setsForCurrentExercise, key = { it.localId }) { set ->
                    FitQuestCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(Spacing.sm))
                                Text(
                                    stringResource(
                                        R.string.set_line,
                                        set.setNumber,
                                        WeightUnits.format(set.weightKg, units),
                                        set.reps
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (set.isPersonalRecord) {
                                    StatusChip(
                                        text = stringResource(R.string.pr),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                IconButton(onClick = { onDeleteSet(set) }, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.delete_set),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = { showPicker = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.add_more_exercises)) }
        }

        Spacer(Modifier.height(Spacing.sm))
        if (totalSets > 0) {
            Button(
                onClick = onFinishWorkout,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(stringResource(R.string.finish_workout), style = MaterialTheme.typography.labelLarge) }
        } else {
            OutlinedButton(
                onClick = onCancelWorkout,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(stringResource(R.string.cancel_workout), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Shows a stepped weight without a trailing ".0". */
private fun stepLabel(value: Double): String =
    if (value == Math.floor(value)) value.toLong().toString() else value.toString()

/** A number field with large minus and plus buttons, which suit sweaty hands in a gym. */
@Composable
private fun StepperField(
    value: String,
    label: String,
    keyboardType: KeyboardType,
    isError: Boolean,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            FilledTonalIconButton(
                onClick = onDecrease,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease)) }
            FilledTonalIconButton(
                onClick = onIncrease,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase)) }
        }
    }
}

@Composable
private fun ExercisePicker(
    exerciseLibrary: List<ExerciseDto>,
    modifier: Modifier,
    onCancel: (() -> Unit)?,
    onConfirm: (List<ExerciseDto>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.choose_exercises))
        ExerciseSelectionList(
            exerciseLibrary = exerciseLibrary,
            selectedIds = selectedIds,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.height(Spacing.sm))
        PrimaryButton(
            text = if (selectedIds.isEmpty()) stringResource(R.string.select_to_continue)
            else stringResource(R.string.add_to_workout, selectedIds.size),
            enabled = selectedIds.isNotEmpty(),
            onClick = {
                val libraryById = exerciseLibrary.associateBy { it._id }
                onConfirm(selectedIds.mapNotNull { libraryById[it] })
            }
        )
        if (onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
