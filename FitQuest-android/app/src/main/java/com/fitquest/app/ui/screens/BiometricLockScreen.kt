package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitquest.app.R
import com.fitquest.app.ui.components.BrandMark
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.Spacing

/** Covers the app after SSO and on each launch until the user passes the biometric prompt. */
@Composable
fun BiometricLockScreen(
    showError: Boolean,
    onUnlock: () -> Unit,
    onSignOut: () -> Unit
) {
    LaunchedEffect(Unit) { onUnlock() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandMark(size = 72.dp)
            Spacer(Modifier.height(Spacing.lg))
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (showError) {
                Spacer(Modifier.height(Spacing.md))
                ErrorBanner(stringResource(R.string.unlock_failed))
            }
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(text = stringResource(R.string.unlock_button), onClick = onUnlock)
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = onSignOut) { Text(stringResource(R.string.use_other_account)) }
        }
    }
}
