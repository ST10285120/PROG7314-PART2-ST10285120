package com.fitquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitquest.app.R
import com.fitquest.app.ui.components.BrandMark
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.viewmodel.AuthUiState

/** The entry screen. Accounts are created by signing in with Google or GitHub. */
@Composable
fun SsoSignInScreen(
    uiState: AuthUiState,
    onSignInWithGoogle: () -> Unit,
    onSignInWithGitHub: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandMark(size = 96.dp)
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(56.dp))

            PrimaryButton(
                text = stringResource(R.string.continue_google),
                onClick = onSignInWithGoogle,
                enabled = uiState !is AuthUiState.Loading
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSignInWithGitHub,
                enabled = uiState !is AuthUiState.Loading,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.continue_github), style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.sso_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (uiState is AuthUiState.Loading) {
                Spacer(Modifier.height(Spacing.lg))
                CircularProgressIndicator(strokeWidth = 2.5.dp)
            }
            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(Spacing.lg))
                val isDuplicateAccount = uiState.message.contains("409")
                ErrorBanner(
                    stringResource(if (isDuplicateAccount) R.string.error_account_exists else R.string.error_sign_in)
                )
            }
            if (uiState is AuthUiState.SessionExpired) {
                Spacer(Modifier.height(Spacing.lg))
                ErrorBanner(stringResource(R.string.error_session_expired))
            }

            Spacer(Modifier.height(Spacing.xl))
            Text(
                stringResource(R.string.terms_notice),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
