package com.fitquest.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitquest.app.R
import com.fitquest.app.ui.theme.FitQuestDarkSurface
import com.fitquest.app.ui.theme.FitQuestGold
import com.fitquest.app.ui.theme.FitQuestGreen
import com.fitquest.app.ui.theme.FitQuestNavy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** XP per level. Must match XP_PER_LEVEL in the API's gamification service. */
const val XP_PER_LEVEL = 200

private val HeroGradient = Brush.linearGradient(listOf(FitQuestNavy, Color(0xFF0F3D2E), FitQuestGreen))

/** Points of a flat-sided hexagon, the shape used for level and badge medals. */
private fun hexagonPath(center: Offset, radius: Float): Path = Path().apply {
    for (corner in 0 until 6) {
        val angle = PI / 3 * corner - PI / 2
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (corner == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** The FitQuest mark from the launcher icon: a green peak behind a white barbell. */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(FitQuestNavy),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height
            val peak = Path().apply {
                moveTo(w * 0.22f, h * 0.84f)
                lineTo(w * 0.5f, h * 0.16f)
                lineTo(w * 0.78f, h * 0.84f)
            }
            drawPath(peak, FitQuestGreen, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round))

            val barY = h * 0.6f
            drawLine(Color.White, Offset(w * 0.08f, barY), Offset(w * 0.92f, barY), strokeWidth = w * 0.05f)
            listOf(0.16f, 0.84f).forEach { x ->
                drawLine(
                    Color.White,
                    Offset(w * x, barY - h * 0.16f),
                    Offset(w * x, barY + h * 0.16f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** A gold hexagon showing the user's level. */
@Composable
fun LevelHexagon(level: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.minDimension / 2
            drawPath(hexagonPath(center, radius), FitQuestGold, style = Fill)
            drawPath(hexagonPath(center, radius * 0.84f), FitQuestNavy, style = Fill)
        }
        Text(
            text = level.toString(),
            color = FitQuestGold,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.36f).sp
        )
    }
}

/** The rank name for a level, which gives progress a role-playing feel. */
@Composable
fun rankTitle(level: Int): String = stringResource(
    when {
        level < 5 -> R.string.rank_novice
        level < 10 -> R.string.rank_apprentice
        level < 20 -> R.string.rank_warrior
        level < 35 -> R.string.rank_champion
        else -> R.string.rank_legend
    }
)

/** The home dashboard hero: identity, level, XP bar and streak. */
@Composable
fun QuestHeroCard(
    displayName: String?,
    level: Int,
    xpTotal: Int,
    pendingXp: Int,
    streak: Int,
    longestStreak: Int,
    onClick: () -> Unit
) {
    Card(onClick = onClick, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.background(HeroGradient).padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelHexagon(level = level, size = 64.dp)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (displayName.isNullOrBlank()) stringResource(R.string.hero_greeting_default)
                        else stringResource(R.string.hero_greeting, displayName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = rankTitle(level),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            val xpIntoLevel = xpTotal % XP_PER_LEVEL
            LinearProgressIndicator(
                progress = { xpIntoLevel / XP_PER_LEVEL.toFloat() },
                color = FitQuestGold,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(MaterialTheme.shapes.extraSmall)
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.xp_to_next, xpIntoLevel, XP_PER_LEVEL),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            if (pendingXp > 0) {
                Text(
                    text = stringResource(R.string.xp_pending, pendingXp),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitQuestGold
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = FitQuestGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = stringResource(R.string.streak_days, streak),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.streak_best, longestStreak),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** A hexagonal badge medal. Locked badges are muted and show a padlock. */
@Composable
fun BadgeMedal(earned: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val rim = if (earned) FitQuestGold else MaterialTheme.colorScheme.outline
    val face = if (earned) FitQuestNavy else MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.minDimension / 2
            drawPath(hexagonPath(center, radius), rim, style = Fill)
            drawPath(hexagonPath(center, radius * 0.84f), face, style = Fill)
        }
        Icon(
            imageVector = if (earned) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (earned) FitQuestGold else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

/** The display name of a muscle group from the API, in the user's language. */
@Composable
fun muscleGroupName(group: String): String = when (group) {
    "CHEST" -> stringResource(R.string.muscle_chest)
    "BACK" -> stringResource(R.string.muscle_back)
    "LEGS" -> stringResource(R.string.muscle_legs)
    "SHOULDERS" -> stringResource(R.string.muscle_shoulders)
    "ARMS" -> stringResource(R.string.muscle_arms)
    "CORE" -> stringResource(R.string.muscle_core)
    else -> group
}

/**
 * Line chart of weekly volume, drawn directly so the app needs no chart library. Values
 * are given in the user's unit and the points are scaled to the tallest value.
 */
@Composable
fun VolumeLineChart(values: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val top = 8.dp.toPx()
        val chartHeight = size.height - top * 2
        val maxValue = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

        repeat(3) { line ->
            val y = top + chartHeight * line / 2
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val points = values.mapIndexed { index, value ->
            val x = if (values.size > 1) stepX * index else size.width / 2
            Offset(x, top + chartHeight * (1 - (value / maxValue).toFloat()))
        }
        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
    }
}

/** One row of the muscle group breakdown. */
@Composable
fun SplitBar(label: String, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(88.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(10.dp).clip(MaterialTheme.shapes.extraSmall)
        )
        Text(
            text = "${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp)
        )
    }
}

/** Shown on every signed-in screen while there is no connection. */
@Composable
fun OfflineBanner() {
    Surface(color = FitQuestDarkSurface, contentColor = FitQuestGold, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.offline_banner), style = MaterialTheme.typography.bodySmall)
        }
    }
}
