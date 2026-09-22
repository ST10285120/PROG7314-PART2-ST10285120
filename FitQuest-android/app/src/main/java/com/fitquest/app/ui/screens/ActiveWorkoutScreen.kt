package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.ui.components.StatusChip
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    exerciseLibrary: List<ExerciseDto>,
    exercisesInWorkout: List<WorkoutExerciseEntity>,
    currentExercise: WorkoutExerciseEntity?,
    setsForCurrentExercise: List<SetEntryEntity>,
    workoutStartedAt: Long?,
    hasNextExercise: Boolean,
    restTimerDefaultSec: Int = 90,
    onPickExercises: (List<ExerciseDto>) -> Unit,
    onLogSet: (reps: Int, weightKg: Double, rpe: Int?) -> Unit,
    onNextExercise: () -> Unit,
    onFinishWorkout: () -> Unit
) {
    var reps by remember { mutableStateOf("8") }
    var weight by remember { mutableStateOf("20") }
    var showPicker by remember { mutableStateOf(currentExercise == null) }
    LaunchedEffect(currentExercise?.localId) {
        if (currentExercise != null) showPicker = false
    }

    var restTimerTrigger by remember { mutableIntStateOf(0) }
    var remainingRestSeconds by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(restTimerTrigger) {
        if (restTimerTrigger == 0) return@LaunchedEffect
        remainingRestSeconds = restTimerDefaultSec
        while ((remainingRestSeconds ?: 0) > 0) {
            delay(1000)
            remainingRestSeconds = (remainingRestSeconds ?: 1) - 1
        }
        remainingRestSeconds = null
    }

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(workoutStartedAt) {
        if (workoutStartedAt == null) return@LaunchedEffect
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - workoutStartedAt) / 1000).toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
        if (workoutStartedAt != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "%d:%02d:%02d".format(elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60, elapsedSeconds % 60),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (showPicker || currentExercise == null) {
            ExercisePicker(
                exerciseLibrary = exerciseLibrary,
                onConfirm = { selected ->
                    onPickExercises(selected)
                    showPicker = false
                }
            )
        } else {
            FitQuestCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "CURRENT EXERCISE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    currentExercise.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(Spacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Kg") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(text = "Log Set") {
                val repsInt = reps.toIntOrNull() ?: return@PrimaryButton
                val weightDouble = weight.toDoubleOrNull() ?: return@PrimaryButton
                onLogSet(repsInt, weightDouble, null)
                restTimerTrigger += 1
            }

            remainingRestSeconds?.let { seconds ->
                Spacer(Modifier.height(Spacing.sm))
                FitQuestCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            "Rest: %d:%02d".format(seconds / 60, seconds % 60),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (hasNextExercise) {
                Spacer(Modifier.height(Spacing.sm))
                Button(
                    onClick = onNextExercise,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Next Exercise")
                    Spacer(Modifier.width(Spacing.sm))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(Spacing.md))
            SectionHeader("Logged sets")
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                items(setsForCurrentExercise) { set ->
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
                                Text("Set ${set.setNumber} — ${set.weightKg}kg × ${set.reps}")
                            }
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

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = { showPicker = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add more exercises") }
        }

        Spacer(Modifier.height(Spacing.sm))
        Button(
            onClick = onFinishWorkout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Finish Workout", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun ExercisePicker(
    exerciseLibrary: List<ExerciseDto>,
    onConfirm: (List<ExerciseDto>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader("Choose exercises")
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(exerciseLibrary) { exercise ->
                val isSelected = selectedIds.contains(exercise._id)
                FitQuestCard(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    onClick = {
                        if (isSelected) selectedIds.remove(exercise._id) else selectedIds.add(exercise._id)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedIds.add(exercise._id) else selectedIds.remove(exercise._id)
                            }
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Column {
                            Text(exercise.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${exercise.muscleGroup} · ${exercise.equipment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        PrimaryButton(
            text = if (selectedIds.isEmpty()) "Select exercises to continue" else "Add ${selectedIds.size} to Workout",
            enabled = selectedIds.isNotEmpty(),
            onClick = {
                val selected = exerciseLibrary.filter { selectedIds.contains(it._id) }
                onConfirm(selected)
            }
        )
    }
}
