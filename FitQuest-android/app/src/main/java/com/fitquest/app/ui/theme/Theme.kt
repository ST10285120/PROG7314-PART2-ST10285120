package com.fitquest.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = FitQuestGreen,
    onPrimary = Color.White,
    primaryContainer = FitQuestGreenContainer,
    onPrimaryContainer = FitQuestGreenDark,
    secondary = FitQuestSlate,
    onSecondary = Color.White,
    secondaryContainer = FitQuestBlueContainer,
    onSecondaryContainer = FitQuestBlue,
    tertiary = FitQuestAmber,
    tertiaryContainer = FitQuestAmberContainer,
    background = FitQuestLightBg,
    onBackground = FitQuestNavy,
    surface = FitQuestSurfaceTint,
    onSurface = FitQuestNavy,
    surfaceVariant = FitQuestDivider,
    onSurfaceVariant = FitQuestGrey,
    outline = FitQuestCardBorder,
    outlineVariant = FitQuestDivider,
    error = FitQuestRed,
    errorContainer = FitQuestRedContainer,
    onErrorContainer = FitQuestRed,
)

private val DarkColors = darkColorScheme(
    primary = FitQuestGreen,
    onPrimary = Color.White,
    primaryContainer = FitQuestGreenDark,
    onPrimaryContainer = FitQuestGreenLight,
    secondary = FitQuestGreySoft,
    onSecondary = FitQuestNavy,
    secondaryContainer = FitQuestDarkSurfaceAlt,
    onSecondaryContainer = FitQuestBlueContainer,
    tertiary = FitQuestAmber,
    tertiaryContainer = Color(0xFF3A2E10),
    background = FitQuestDarkBg,
    onBackground = Color(0xFFE5E7EB),
    surface = FitQuestDarkSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = FitQuestDarkSurfaceAlt,
    onSurfaceVariant = FitQuestGreySoft,
    outline = FitQuestDarkBorder,
    outlineVariant = FitQuestDarkBorder,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF3F1D1D),
    onErrorContainer = Color(0xFFF87171),
)

@Composable
fun FitQuestTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FitQuestTypography,
        shapes = FitQuestShapes,
        content = content
    )
}
