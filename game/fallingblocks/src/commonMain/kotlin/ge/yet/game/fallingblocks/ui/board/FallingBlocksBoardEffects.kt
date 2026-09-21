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
        val hardDrop = event as? FallingBlocksVisualEvent.HardDrop
        val beamColumns = remember(hardDrop) {
            hardDrop?.let { dropTrailBeam(it.from, it.to) }.orEmpty()
        }
        val halftone = remember(hardDrop, beamColumns) {
            hardDrop?.let { trailHalftoneDots(it.id, beamColumns) }.orEmpty()
        }
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
                        beamColumns = beamColumns,
                        halftone = halftone,
                        progress = progress.value,
                        policy = motionPolicy,
                        scheme = scheme,
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
            policy.hardDropBeamDurationMillis + policy.hardDropImpactDurationMillis
        is FallingBlocksVisualEvent.LineClear ->
            policy.lineClearFlashDurationMillis + policy.lineClearCollapseDurationMillis
    }

private fun DrawScope.drawHardDropEffect(
    event: FallingBlocksVisualEvent.HardDrop,
    beamColumns: List<BeamColumn>,
    halftone: List<HalftoneDot>,
    progress: Float,
    policy: FallingBlocksMotionPolicy,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val total = event.durationMillis(policy).coerceAtLeast(1)
    val beamSplit = policy.hardDropBeamDurationMillis.toFloat() / total
    val pieceColors = event.type.colors(scheme)
    if (beamSplit > 0f && progress < beamSplit) {
        val beamAlpha = 1f - (progress / beamSplit).coerceIn(0f, 1f)
        drawTrailBeam(
            beamColumns,
            halftone,
            glow = pieceColors.fill,
            core = pieceColors.highlight,
            dots = pieceColors.highlight,
            hatch = scheme.tertiary,
            beamAlpha = beamAlpha,
        )
    }

    val impactProgress = if (beamSplit >= 1f) {
        1f
    } else {
        ((progress - beamSplit) / (1f - beamSplit)).coerceIn(0f, 1f)
    }
    if (progress < beamSplit) return
    val alpha = (1f - impactProgress) * 0.88f
    val cellSize = size.width / Board.WIDTH
    drawRoundRect(
        color = scheme.outline.copy(alpha = alpha),
        style = Stroke(maxOf(1f, cellSize * 0.10f)),
        cornerRadius = CornerRadius(cellSize * 0.16f),
    )
    event.to.forEach { cell ->
        drawEffectCell(cell.x.toFloat(), cell.y.toFloat(), scheme.primary, alpha * 0.55f)
    }
}

private fun DrawScope.drawTrailBeam(
    columns: List<BeamColumn>,
    halftone: List<HalftoneDot>,
    glow: androidx.compose.ui.graphics.Color,
    core: androidx.compose.ui.graphics.Color,
    dots: androidx.compose.ui.graphics.Color,
    hatch: androidx.compose.ui.graphics.Color,
    beamAlpha: Float,
) {
    if (columns.isEmpty() || beamAlpha <= 0f) return
    val cellWidth = size.width / Board.WIDTH
    val glowWidth = cellWidth * 0.86f
    val coreWidth = cellWidth * 0.44f
    columns.forEach { column ->
        val topPx = column.top * size.height
        val bottomPx = column.bottom * size.height
        if (bottomPx <= topPx) return@forEach
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to glow.copy(alpha = 0f),
                1f to glow.copy(alpha = 0.32f * beamAlpha),
                startY = topPx,
                endY = bottomPx,
            ),
            topLeft = Offset(
                column.x * cellWidth + (cellWidth - glowWidth) / 2f,
                topPx,
            ),
            size = Size(glowWidth, bottomPx - topPx),
            cornerRadius = CornerRadius(glowWidth * 0.20f),
        )
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to core.copy(alpha = 0f),
                1f to core.copy(alpha = 0.85f * beamAlpha),
                startY = topPx,
                endY = bottomPx,
            ),
            topLeft = Offset(
                column.x * cellWidth + (cellWidth - coreWidth) / 2f,
                topPx,
            ),
            size = Size(coreWidth, bottomPx - topPx),
            cornerRadius = CornerRadius(coreWidth * 0.22f),
        )
    }
    halftone.forEach { dot ->
        drawCircle(
            color = dots.copy(alpha = (dot.alpha * beamAlpha).coerceIn(0f, 1f)),
            radius = dot.radius * size.width,
            center = Offset(dot.cx * size.width, dot.cy * size.height),
        )
    }
    val topPx = columns.minOf { it.top } * size.height
    val bottomPx = columns.maxOf { it.bottom } * size.height
    val heightPx = bottomPx - topPx
    if (heightPx < 2f * size.height / Board.VISIBLE_HEIGHT) return
    val leftPx = columns.minOf { it.x } * cellWidth
    val rightPx = (columns.maxOf { it.x } + 1) * cellWidth
    clipRect(left = leftPx, top = topPx, right = rightPx, bottom = bottomPx) {
        val spacing = maxOf(4f, cellWidth * 0.8f)
        var startX = leftPx - heightPx
        var drawn = 0
        while (startX < rightPx && drawn < 12) {
            drawLine(
                color = hatch.copy(alpha = 0.38f * beamAlpha),
                start = Offset(startX, bottomPx),
                end = Offset(startX + heightPx, topPx),
                strokeWidth = maxOf(1f, cellWidth * 0.05f),
            )
            startX += spacing
            drawn++
        }
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
            drawRect(primary.copy(alpha = 0.90f * fade), Offset(0f, top), Size(size.width, cellHeight))
            drawLine(
                tertiary.copy(alpha = 0.95f * fade),
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
            visual.type.colors(scheme).highlight,
            0.85f * fade,
            horizontalOffset = lean,
        )
    }
    if (policy.spatialMotionEnabled) {
        particles.forEach { particle ->
            val x = particle.originX + particle.velocityX * progress
            val y = particle.originY + particle.velocityY * progress + 0.55f * progress * progress
            val side = particle.size * size.width
            drawRect(
                color = particle.type.colors(scheme).highlight.copy(
                    alpha = (1f - progress) * 0.92f,
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
                color = tertiary.copy(alpha = (1f - t) * 0.72f),
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
