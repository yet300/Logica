package ge.yet.game.fallingblocks.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import ge.yet.game.fallingblocks.component.game.FallingBlocksVisualEvent
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.ui.motion.FallingBlocksMotionPolicy
import kotlin.math.roundToInt

@Composable
internal fun FallingBlocksBoardEffects(
    event: FallingBlocksVisualEvent?,
    motionPolicy: FallingBlocksMotionPolicy,
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = remember { Animatable(1f) }
    var animatedEventId by remember { mutableStateOf<Long?>(null) }
    val duration = event?.durationMillis(motionPolicy) ?: 0
    val particles = remember(event) {
        (event as? FallingBlocksVisualEvent.LineClear)?.let { clear ->
            lineClearParticles(clear.id, clear.cells)
        }.orEmpty()
    }

    LaunchedEffect(event?.id, active, duration) {
        if (event == null) {
            animatedEventId = null
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        if (animatedEventId != event.id) {
            animatedEventId = event.id
            progress.snapTo(0f)
        }
        if (active && progress.value < 1f) {
            val remaining = (duration * (1f - progress.value)).roundToInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        }
    }

    Box(modifier = modifier) {
        content()
        val scheme = MaterialTheme.colorScheme
        Canvas(
            Modifier
                .fillMaxSize()
                .testTag("falling_blocks_effects"),
        ) {
            val current = event ?: return@Canvas
            if (progress.value >= 1f) return@Canvas
            clipRect {
                when (current) {
                    is FallingBlocksVisualEvent.HardDrop -> drawHardDropEffect(
                        event = current,
                        progress = progress.value,
                        policy = motionPolicy,
                        primary = scheme.primary,
                        tertiary = scheme.tertiary,
                        outline = scheme.outline,
                    )
                    is FallingBlocksVisualEvent.LineClear -> drawLineClearEffect(
                        event = current,
                        particles = particles,
                        progress = progress.value,
                        policy = motionPolicy,
                        primary = scheme.primary,
                        tertiary = scheme.tertiary,
                        scheme = scheme,
                    )
                }
            }
        }
    }
}

private fun FallingBlocksVisualEvent.durationMillis(policy: FallingBlocksMotionPolicy): Int =
    when (this) {
        is FallingBlocksVisualEvent.HardDrop ->
            policy.hardDropTrailDurationMillis + policy.hardDropImpactDurationMillis
        is FallingBlocksVisualEvent.LineClear ->
            policy.lineClearFlashDurationMillis + policy.lineClearCollapseDurationMillis
    }

private fun DrawScope.drawHardDropEffect(
    event: FallingBlocksVisualEvent.HardDrop,
    progress: Float,
    policy: FallingBlocksMotionPolicy,
    primary: androidx.compose.ui.graphics.Color,
    tertiary: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
) {
    val total = event.durationMillis(policy).coerceAtLeast(1)
    val trailSplit = policy.hardDropTrailDurationMillis.toFloat() / total
    if (policy.spatialMotionEnabled && trailSplit > 0f && progress < trailSplit) {
        val trailProgress = (progress / trailSplit).coerceIn(0f, 1f)
        hardDropTrails(event.from, event.to).forEach { trail ->
            repeat(3) { echo ->
                val delayed = (trailProgress - echo * 0.16f).coerceIn(0f, 1f)
                val y = trail.fromY + (trail.toY - trail.fromY) * delayed
                drawNormalizedEffectCell(
                    centerX = trail.centerX,
                    centerY = y,
                    color = if (echo % 2 == 0) primary else tertiary,
                    alpha = (1f - trailProgress) * (0.28f - echo * 0.06f),
                    horizontalOffset = if (echo % 2 == 0) -0.05f else 0.05f,
                )
            }
        }
    }

    val impactProgress = if (trailSplit >= 1f) 1f else {
        ((progress - trailSplit) / (1f - trailSplit)).coerceIn(0f, 1f)
    }
    if (progress >= trailSplit) {
        val alpha = (1f - impactProgress) * 0.72f
        val cellSize = size.width / Board.WIDTH
        drawRoundRect(
            color = outline.copy(alpha = alpha),
            style = Stroke(maxOf(1f, cellSize * 0.10f)),
            cornerRadius = CornerRadius(cellSize * 0.16f),
        )
        event.to.forEach { cell ->
            drawEffectCell(cell.x.toFloat(), cell.y.toFloat(), primary, alpha * 0.55f)
        }
    }
}

private fun DrawScope.drawNormalizedEffectCell(
    centerX: Float,
    centerY: Float,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
    horizontalOffset: Float = 0f,
) {
    val cellWidth = size.width / Board.WIDTH
    val cellHeight = size.height / Board.VISIBLE_HEIGHT
    val inset = cellWidth * 0.10f
    drawRoundRect(
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        topLeft = Offset(
            (centerX + horizontalOffset / Board.WIDTH) * size.width - cellWidth / 2f + inset,
            centerY * size.height - cellHeight / 2f + inset,
        ),
        size = Size(cellWidth - inset * 2f, cellHeight - inset * 2f),
        cornerRadius = CornerRadius(cellWidth * 0.12f),
    )
}

private fun DrawScope.drawLineClearEffect(
    event: FallingBlocksVisualEvent.LineClear,
    particles: List<BoardEffectParticle>,
    progress: Float,
    policy: FallingBlocksMotionPolicy,
    primary: androidx.compose.ui.graphics.Color,
    tertiary: androidx.compose.ui.graphics.Color,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val total = event.durationMillis(policy).coerceAtLeast(1)
    val flashSplit = policy.lineClearFlashDurationMillis.toFloat() / total
    val flashProgress = if (flashSplit == 0f) 1f else (progress / flashSplit).coerceIn(0f, 1f)
    val flashAlpha = (1f - flashProgress) * 0.78f
    val cellHeight = size.height / Board.VISIBLE_HEIGHT

    event.rows.forEach { row ->
        val visibleY = row - Board.HIDDEN_ROWS
        if (visibleY in 0 until Board.VISIBLE_HEIGHT) {
            val top = visibleY * cellHeight
            drawRect(primary.copy(alpha = flashAlpha), Offset(0f, top), Size(size.width, cellHeight))
            drawLine(
                tertiary.copy(alpha = flashAlpha),
                Offset(0f, top + cellHeight / 2f),
                Offset(size.width, top + cellHeight / 2f),
                strokeWidth = maxOf(1f, cellHeight * 0.10f),
            )
        }
    }
    event.cells.forEach { visual ->
        drawEffectCell(
            visual.cell.x.toFloat(),
            visual.cell.y.toFloat(),
            visual.type.colors(scheme).fill,
            (1f - progress) * 0.70f,
        )
    }
    if (policy.spatialMotionEnabled) {
        particles.forEach { particle ->
            val x = particle.originX + particle.velocityX * progress
            val y = particle.originY + particle.velocityY * progress + 0.18f * progress * progress
            val side = particle.size * size.width
            drawRect(
                color = particle.type.colors(scheme).fill.copy(
                    alpha = (1f - progress) * 0.82f,
                ),
                topLeft = Offset(x * size.width - side / 2f, y * size.height - side / 2f),
                size = Size(side, side),
            )
        }
    }
}

private fun DrawScope.drawEffectCell(
    x: Float,
    y: Float,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
    horizontalOffset: Float = 0f,
) {
    if (y < Board.HIDDEN_ROWS || y >= Board.TOTAL_HEIGHT) return
    val cellWidth = size.width / Board.WIDTH
    val cellHeight = size.height / Board.VISIBLE_HEIGHT
    val visibleY = y - Board.HIDDEN_ROWS
    val inset = cellWidth * 0.10f
    drawRoundRect(
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        topLeft = Offset(
            (x + horizontalOffset) * cellWidth + inset,
            visibleY * cellHeight + inset,
        ),
        size = Size(cellWidth - inset * 2f, cellHeight - inset * 2f),
        cornerRadius = CornerRadius(cellWidth * 0.12f),
    )
}
