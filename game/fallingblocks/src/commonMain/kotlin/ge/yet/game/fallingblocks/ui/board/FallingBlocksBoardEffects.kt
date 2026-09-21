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
                    is FallingBlocksVisualEvent.HardDrop -> drawHardDropImpact(
                        event = current,
                        progress = progress.value,
                        primary = scheme.primary,
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
            policy.hardDropImpactDurationMillis
        is FallingBlocksVisualEvent.LineClear ->
            policy.lineClearFlashDurationMillis + policy.lineClearCollapseDurationMillis
    }

private fun DrawScope.drawHardDropImpact(
    event: FallingBlocksVisualEvent.HardDrop,
    progress: Float,
    primary: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
) {
    val alpha = (1f - progress.coerceIn(0f, 1f)) * 0.72f
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
    val elapsedMs = progress * total
    val flashMs = policy.lineClearFlashDurationMillis.coerceAtLeast(1).toFloat()
    val cellHeight = size.height / Board.VISIBLE_HEIGHT
    val sortedRows = event.rows.distinct().sorted()
    // Block Blast-style ripple: each cleared row starts its flash on its own
    // cascade slot. Reduced motion collapses every row onto slot zero.
    fun rowFade(row: Int): Float {
        val start = if (policy.spatialMotionEnabled) {
            lineClearCascadeDelayMs(lineClearRowSlot(row, sortedRows)).toFloat()
        } else {
            0f
        }
        if (elapsedMs < start) return 1f
        return 1f - ((elapsedMs - start) / flashMs).coerceIn(0f, 1f)
    }

    sortedRows.forEach { row ->
        val visibleY = row - Board.HIDDEN_ROWS
        if (visibleY in 0 until Board.VISIBLE_HEIGHT) {
            val fade = rowFade(row)
            val top = visibleY * cellHeight
            drawRect(primary.copy(alpha = 0.78f * fade), Offset(0f, top), Size(size.width, cellHeight))
            drawLine(
                tertiary.copy(alpha = 0.85f * fade),
                Offset(0f, top + cellHeight / 2f),
                Offset(size.width, top + cellHeight / 2f),
                strokeWidth = maxOf(1f, cellHeight * 0.10f),
            )
        }
    }
    event.cells.forEach { visual ->
        val fade = rowFade(visual.cell.y)
        // Tilt shimmer without per-cell animators: alternate columns lean
        // opposite ways while fading, echoing Block Blast's clear rotation.
        val lean = (if (visual.cell.x % 2 == 0) -0.035f else 0.035f) * fade
        drawEffectCell(
            visual.cell.x.toFloat(),
            visual.cell.y.toFloat(),
            visual.type.colors(scheme).fill,
            0.70f * fade,
            horizontalOffset = lean,
        )
    }
    if (policy.spatialMotionEnabled) {
        particles.forEach { particle ->
            val x = particle.originX + particle.velocityX * progress
            val y = particle.originY + particle.velocityY * progress + 0.55f * progress * progress
            val side = particle.size * size.width
            drawRect(
                color = particle.type.colors(scheme).fill.copy(
                    alpha = (1f - progress) * 0.82f,
                ),
                topLeft = Offset(x * size.width - side / 2f, y * size.height - side / 2f),
                size = Size(side, side),
            )
        }
        lineClearShockwaves(event.rows).forEach { wave ->
            val start = lineClearCascadeDelayMs(wave.slot).toFloat()
            if (elapsedMs < start) return@forEach
            val span = (total - start).coerceAtLeast(1f)
            val t = ((elapsedMs - start) / span).coerceIn(0f, 1f)
            if (t >= 1f) return@forEach
            drawCircle(
                color = tertiary.copy(alpha = (1f - t) * 0.6f),
                radius = t * 0.6f * size.width,
                center = Offset(wave.centerX * size.width, wave.centerY * size.height),
                style = Stroke(width = maxOf(1f, 0.04f * size.width)),
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
