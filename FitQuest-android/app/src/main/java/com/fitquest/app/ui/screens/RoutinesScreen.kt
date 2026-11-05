package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fitquest.app.R
import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.data.remote.dto.RoutineDto
import com.fitquest.app.ui.components.ConfirmDialog
import com.fitquest.app.ui.components.ExerciseSelectionList
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.ui.components.exerciseName

private const val MAX_NAME_LENGTH = 60

/** Which routine is open in the editor. A null routine means a new one. */
private data class RoutineEditor(val routine: RoutineDto?)

/** Lists custom routines and opens the editor to create or change them (FR2). */
@Composable
fun RoutinesScreen(
    routines: List<RoutineDto>,
    exerciseLibrary: List<ExerciseDto>,
    hasActiveWorkout: Boolean,
    onStartRoutine: (RoutineDto) -> Unit,
    onSaveRoutine: (routineId: String?, name: String, exercises: List<ExerciseDto>) -> Unit,
    onDeleteRoutine: (String) -> Unit
) {
    var editor by remember { mutableStateOf<RoutineEditor?>(null) }
    var pendingDelete by remember { mutableStateOf<RoutineDto?>(null) }

    val openEditor = editor
    if (openEditor != null) {
        RoutineEditorContent(
            routine = openEditor.routine,
            exerciseLibrary = exerciseLibrary,
            onSave = { name, exercises ->
                onSaveRoutine(openEditor.routine?.id, name, exercises)
                editor = null
            },
            onCancel = { editor = null }
        )
        return
    }

    pendingDelete?.let { routine ->
        ConfirmDialog(
            title = stringResource(R.string.delete_routine),
            message = stringResource(R.string.delete_routine_message, routine.name),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                onDeleteRoutine(routine.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            PrimaryButton(
                text = stringResource(R.string.new_routine),
                onClick = { editor = RoutineEditor(null) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }
        if (hasActiveWorkout) {
            item {
                Text(
                    stringResource(R.string.finish_active_first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (routines.isEmpty()) {
            item {
                FitQuestCard {
                    Text(stringResource(R.string.no_routines), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(routines, key = { it.id }) { routine ->
            FitQuestCard {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    routine.exercises.map { exerciseName(it.name) }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onStartRoutine(routine) },
                        enabled = !hasActiveWorkout,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.start_routine))
                    }
                    IconButton(onClick = { editor = RoutineEditor(routine) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_routine))
                    }
                    IconButton(onClick = { pendingDelete = routine }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_routine))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineEditorContent(
    routine: RoutineDto?,
    exerciseLibrary: List<ExerciseDto>,
    onSave: (name: String, exercises: List<ExerciseDto>) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(routine?.name.orEmpty()) }
    val selectedIds = remember {
        mutableStateListOf<String>().apply { routine?.exercises?.forEach { add(it.exerciseId) } }
    }
    val isNameValid = name.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(MAX_NAME_LENGTH) },
            label = { Text(stringResource(R.string.routine_name)) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.sm))
        ExerciseSelectionList(
            exerciseLibrary = exerciseLibrary,
            selectedIds = selectedIds,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.height(Spacing.sm))
        PrimaryButton(
            text = stringResource(R.string.save_routine, selectedIds.size),
            enabled = isNameValid && selectedIds.isNotEmpty(),
            onClick = {
                val libraryById = exerciseLibrary.associateBy { it._id }
                onSave(name, selectedIds.mapNotNull { libraryById[it] })
            }
        )
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel))
        }
    }
}
