package com.fitquest.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fitquest.app.BuildConfig
import com.fitquest.app.R
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.ui.components.ConfirmDialog
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.FitQuestCard
import com.fitquest.app.ui.components.SectionHeader
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.viewmodel.SettingsUiState

private val REMINDER_TIME_PATTERN = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
private val REST_SECONDS_RANGE = 10..600

/** Preferences that make sense for this app, plus the account card. */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    profile: UserDto?,
    language: String,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onUnitsChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onSave: (reminderTime: String, restTimerDefaultSec: Int) -> Unit,
    onChangeAvatar: () -> Unit,
    onTestNotification: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var confirmLogOut by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }

    if (confirmLogOut) {
        ConfirmDialog(
            title = stringResource(R.string.log_out),
            message = stringResource(R.string.log_out_message),
            confirmLabel = stringResource(R.string.log_out),
            onConfirm = {
                confirmLogOut = false
                onLogOut()
            },
            onDismiss = { confirmLogOut = false }
        )
    }
    if (confirmDeleteAccount) {
        ConfirmDialog(
            title = stringResource(R.string.delete_account),
            message = stringResource(R.string.delete_account_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDeleteAccount = false
                onDeleteAccount()
            },
            onDismiss = { confirmDeleteAccount = false }
        )
    }

    var reminderTimeDraft by remember(uiState.preferences.reminderTime) {
        mutableStateOf(uiState.preferences.reminderTime)
    }
    var restTimerDraft by remember(uiState.preferences.restTimerDefaultSec) {
        mutableStateOf(uiState.preferences.restTimerDefaultSec.toString())
    }

    val hasUnsavedChanges = reminderTimeDraft != uiState.preferences.reminderTime ||
        restTimerDraft != uiState.preferences.restTimerDefaultSec.toString()
    val reminderValid = REMINDER_TIME_PATTERN.matches(reminderTimeDraft)
    val restTimerValid = restTimerDraft.toIntOrNull()?.let { it in REST_SECONDS_RANGE } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        SectionHeader(stringResource(R.string.section_account))
        FitQuestCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile = profile, onClick = onChangeAvatar)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        profile?.username?.takeIf { it.isNotBlank() } ?: stringResource(R.string.account_signed_in),
                        style = MaterialTheme.typography.titleMedium
                    )
                    profile?.email?.takeIf { it.isNotBlank() }?.let { email ->
                        Text(
                            email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onChangeAvatar, enabled = !uiState.isUploadingAvatar) {
                        Text(
                            stringResource(
                                if (uiState.isUploadingAvatar) R.string.uploading else R.string.change_photo
                            )
                        )
                    }
                }
            }
            if (uiState.avatarError) {
                Spacer(Modifier.height(Spacing.sm))
                ErrorBanner(stringResource(R.string.avatar_error))
            }
        }

        SectionHeader(stringResource(R.string.section_preferences))

        FitQuestCard {
            SettingTitle(stringResource(R.string.units))
            SingleChoiceRow(
                options = listOf("KG" to stringResource(R.string.kilograms), "LB" to stringResource(R.string.pounds)),
                selected = uiState.preferences.units,
                onSelect = onUnitsChange
            )
        }

        FitQuestCard {
            SettingTitle(stringResource(R.string.theme))
            SingleChoiceRow(
                options = listOf(
                    "LIGHT" to stringResource(R.string.theme_light),
                    "DARK" to stringResource(R.string.theme_dark),
                    "SYSTEM" to stringResource(R.string.theme_system)
                ),
                selected = uiState.preferences.theme,
                onSelect = onThemeChange
            )
        }

        FitQuestCard {
            SettingTitle(stringResource(R.string.language))
            SingleChoiceRow(
                options = listOf(
                    "" to stringResource(R.string.language_system),
                    "en" to "English",
                    "af" to "Afrikaans",
                    "zu" to "isiZulu"
                ),
                selected = language,
                onSelect = onLanguageChange
            )
        }

        FitQuestCard {
            SettingTitle(stringResource(R.string.reminder_time))
            OutlinedTextField(
                value = reminderTimeDraft,
                onValueChange = { reminderTimeDraft = it.take(5) },
                singleLine = true,
                isError = !reminderValid,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                supportingText = {
                    Text(stringResource(if (reminderValid) R.string.reminder_hint else R.string.reminder_invalid))
                }
            )
        }

        FitQuestCard {
            SettingTitle(stringResource(R.string.rest_default))
            OutlinedTextField(
                value = restTimerDraft,
                onValueChange = { restTimerDraft = it.take(3) },
                singleLine = true,
                isError = !restTimerValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                supportingText = {
                    Text(stringResource(if (restTimerValid) R.string.rest_hint else R.string.rest_invalid))
                }
            )
        }

        if (hasUnsavedChanges) {
            Button(
                onClick = { restTimerDraft.toIntOrNull()?.let { onSave(reminderTimeDraft, it) } },
                enabled = reminderValid && restTimerValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(if (uiState.isSaving) R.string.saving else R.string.save_changes))
            }
        }

        if (uiState.errorMessage != null) ErrorBanner(stringResource(R.string.settings_error))

        SectionHeader(stringResource(R.string.section_security))
        FitQuestCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.biometric_lock),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        stringResource(
                            if (biometricAvailable) R.string.biometric_lock_hint else R.string.biometric_unavailable
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = onBiometricChange
                )
            }
        }

        OutlinedButton(
            onClick = onTestNotification,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small
        ) { Text(stringResource(R.string.test_notification)) }
        uiState.testNotificationResult?.let { delivered ->
            Text(
                stringResource(R.string.test_notification_sent, delivered),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionHeader(stringResource(R.string.section_about))
        FitQuestCard {
            Text(
                stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }) {
                Text(stringResource(R.string.privacy_policy))
            }
        }

        OutlinedButton(
            onClick = { confirmLogOut = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.log_out))
        }

        TextButton(
            onClick = { confirmDeleteAccount = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.delete_account)) }
        if (uiState.deleteAccountError) ErrorBanner(stringResource(R.string.delete_account_error))

        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun Avatar(profile: UserDto?, onClick: () -> Unit) {
    val avatarPath = profile?.avatarPath
    val modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable(onClick = onClick)

    if (avatarPath != null) {
        AsyncImage(
            model = BuildConfig.API_BASE_URL + avatarPath,
            contentDescription = stringResource(R.string.avatar_description),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = stringResource(R.string.avatar_description),
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(Spacing.sm))
}

@Composable
private fun SingleChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}
