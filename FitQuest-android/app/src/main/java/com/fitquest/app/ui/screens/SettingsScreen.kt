package com.fitquest.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitquest.app.data.remote.dto.PreferencesDto
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUnitsChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onSave: (reminderTime: String, restTimerDefaultSec: Int) -> Unit,
    onLogOut: () -> Unit
) {
    var reminderTimeDraft by remember(uiState.preferences.reminderTime) {
        mutableStateOf(uiState.preferences.reminderTime)
    }
    var restTimerDraft by remember(uiState.preferences.restTimerDefaultSec) {
        mutableStateOf(uiState.preferences.restTimerDefaultSec.toString())
    }

    val hasUnsavedChanges = reminderTimeDraft != uiState.preferences.reminderTime ||
        restTimerDraft != uiState.preferences.restTimerDefaultSec.toString()
    val restTimerValid = restTimerDraft.toIntOrNull() != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        SectionHeader("Preferences")

        FitQuestCard {
            Text("Units", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.sm))
            SingleChoiceRow(
                options = listOf("KG" to "Kilograms", "LB" to "Pounds"),
                selected = uiState.preferences.units,
                onSelect = onUnitsChange
            )
        }

        FitQuestCard {
            Text("Theme", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.sm))
            SingleChoiceRow(
                options = listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System"),
                selected = uiState.preferences.theme,
                onSelect = onThemeChange
            )
        }

        FitQuestCard {
            Text("Streak reminder time", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(
                "24-hour format, e.g. 18:00",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = reminderTimeDraft,
                onValueChange = { reminderTimeDraft = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )
        }

        FitQuestCard {
            Text("Rest timer default", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(
                "Seconds between sets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = restTimerDraft,
                onValueChange = { restTimerDraft = it },
                singleLine = true,
                isError = restTimerDraft.isNotEmpty() && !restTimerValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                supportingText = {
                    if (restTimerDraft.isNotEmpty() && !restTimerValid) {
                        Text("Enter a whole number of seconds")
                    }
                }
            )
        }

        if (hasUnsavedChanges) {
            Button(
                onClick = { restTimerDraft.toIntOrNull()?.let { onSave(reminderTimeDraft, it) } },
                enabled = restTimerValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (uiState.isSaving) "Saving…" else "Save Changes")
            }
        }

        uiState.errorMessage?.let { ErrorBanner(it) }

        SectionHeader("Account")
        FitQuestCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text("Signed in", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Google or GitHub account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onLogOut,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("Log Out")
        }

        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun SingleChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}
