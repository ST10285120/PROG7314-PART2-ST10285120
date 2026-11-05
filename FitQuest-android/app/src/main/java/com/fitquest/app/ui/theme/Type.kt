package com.fitquest.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp

/**
 * Android's built-in bold condensed face gives headings a sporty scoreboard feel. It
 * ships on every device, so the app needs no font file.
 */
private val QuestDisplay = FontFamily(
    Typeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD))
)

/** Heavy headings and tight spacing give the game-like feel of the brand. */
val FitQuestTypography = Typography(
    displaySmall = TextStyle(fontFamily = QuestDisplay, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = QuestDisplay, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = QuestDisplay, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = QuestDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
