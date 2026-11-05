package com.fitquest.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fitquest.app.R
import com.fitquest.app.data.remote.dto.ExerciseDto

/** The exercise's name in the app's language. */
@Composable
fun exerciseName(apiName: String): String =
    com.fitquest.app.util.ExerciseText.nameRes(apiName)?.let { stringResource(it) } ?: apiName

@Composable
private fun equipmentName(apiName: String): String =
    com.fitquest.app.util.ExerciseText.equipmentRes(apiName)?.let { stringResource(it) } ?: apiName

/**
 * A searchable exercise list with muscle group filters, shared by the workout picker
 * and the routine editor. selectedIds keeps the order exercises were chosen in.
 */
@Composable
fun ExerciseSelectionList(
    exerciseLibrary: List<ExerciseDto>,
    selectedIds: SnapshotStateList<String>,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscleFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val displayed = exerciseLibrary.map { it to exerciseName(it.name) }
    val visible = displayed.filter { (exercise, name) ->
        (muscleFilter == null || exercise.muscleGroup == muscleFilter) &&
            name.contains(query.trim(), ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_exercises)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            FilterChip(
                selected = muscleFilter == null,
                onClick = { muscleFilter = null },
                label = { Text(stringResource(R.string.filter_all)) }
            )
            exerciseLibrary.map { it.muscleGroup }.distinct().forEach { group ->
                FilterChip(
                    selected = muscleFilter == group,
                    onClick = { muscleFilter = group },
                    label = { Text(muscleGroupName(group)) }
                )
            }
        }

        if (exerciseLibrary.isEmpty()) {
            Text(stringResource(R.string.library_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (visible.isEmpty()) {
            Text(stringResource(R.string.no_exercise_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(visible, key = { it.first._id }) { (exercise, name) ->
                val isSelected = selectedIds.contains(exercise._id)
                val toggle = {
                    if (isSelected) selectedIds.remove(exercise._id) else selectedIds.add(exercise._id)
                    Unit
                }
                FitQuestCard(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    onClick = toggle
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = isSelected, onCheckedChange = { toggle() })
                        Spacer(Modifier.width(Spacing.xs))
                        Column {
                            Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${muscleGroupName(exercise.muscleGroup)} · ${equipmentName(exercise.equipment)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A consistent yes or no prompt for actions that cannot be undone. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
