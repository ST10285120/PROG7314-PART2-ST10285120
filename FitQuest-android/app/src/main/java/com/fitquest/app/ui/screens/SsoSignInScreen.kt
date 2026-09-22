package com.fitquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitquest.app.auth.GitHubOAuthManager
import com.fitquest.app.ui.components.ErrorBanner
import com.fitquest.app.ui.components.PrimaryButton
import com.fitquest.app.ui.components.Spacing
import com.fitquest.app.viewmodel.AuthUiState

@Composable
fun SsoSignInScreen(
    uiState: AuthUiState,
    onSignInWithGoogle: () -> Unit,
    onSignInWithGitHub: () -> Unit,
    gitHubOAuthManager: GitHubOAuthManager
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            "FitQuest",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Level up every workout",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(56.dp))

        PrimaryButton(
            text = "Continue with Google",
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
            Text("Continue with GitHub", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(Spacing.md))
        Text(
            "Single Sign-On only — no password to manage",
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
            ErrorBanner(uiState.message)
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            "By continuing you agree to the Terms of Service",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
