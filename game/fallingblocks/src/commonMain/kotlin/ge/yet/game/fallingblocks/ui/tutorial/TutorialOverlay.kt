package ge.yet.game.fallingblocks.ui.tutorial

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.tutorial_hard_drop
import ge.yet.game.fallingblocks.generated.resources.tutorial_move
import ge.yet.game.fallingblocks.generated.resources.tutorial_progress
import ge.yet.game.fallingblocks.generated.resources.tutorial_soft_drop
import ge.yet.game.fallingblocks.generated.resources.tutorial_tap
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal const val TutorialOverlayTag = "falling_blocks_tutorial"

@Composable
internal fun TutorialOverlay(
    progress: TutorialProgress,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val instruction = when (progress.step) {
        TutorialStep.ROTATE -> stringResource(Res.string.tutorial_tap)
        TutorialStep.MOVE -> stringResource(Res.string.tutorial_move)
        TutorialStep.SOFT_DROP -> stringResource(Res.string.tutorial_soft_drop)
        TutorialStep.HARD_DROP -> stringResource(Res.string.tutorial_hard_drop)
    }
    val stepNumber = progress.step.ordinal + 1
    val progressDescription = stringResource(Res.string.tutorial_progress, stepNumber)
    val animatedProgress = if (reducedMotion) {
        0.55f
    } else {
        val transition = rememberInfiniteTransition(label = "tutorial gesture")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1_250, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "tutorial gesture progress",
        )
        value
    }
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .testTag(TutorialOverlayTag)
            .semantics { contentDescription = "$progressDescription. $instruction" },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .background(
                    color = scheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = instruction,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TutorialStep.entries.forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .size(if (index == progress.step.ordinal) 9.dp else 6.dp)
                            .background(
                                color = if (index <= progress.step.ordinal) {
                                    scheme.primary
                                } else {
                                    scheme.outlineVariant
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }

        GesturePictogram(
            step = progress.step,
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GesturePictogram(
    step: TutorialStep,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height * 0.44f)
        val travel = when (step) {
            TutorialStep.ROTATE -> Offset.Zero
            TutorialStep.MOVE -> Offset((progress - 0.5f) * size.width * 0.34f, 0f)
            TutorialStep.SOFT_DROP -> Offset(0f, progress * size.height * 0.12f)
            TutorialStep.HARD_DROP -> Offset(0f, progress * size.height * 0.28f)
        }
        val touch = center + travel
        val stroke = maxOf(2f, size.minDimension * 0.008f)
        val accent = scheme.primary

        when (step) {
            TutorialStep.ROTATE -> {
                val radius = size.minDimension * 0.12f
                drawArc(
                    color = accent.copy(alpha = 0.78f),
                    startAngle = -70f,
                    sweepAngle = 285f * progress.coerceAtLeast(0.22f),
                    useCenter = false,
                    topLeft = center - Offset(radius, radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                val angle = (215f * PI / 180f).toFloat()
                drawArrowHead(
                    tip = center + Offset(cos(angle) * radius, sin(angle) * radius),
                    direction = Offset(-0.65f, -0.76f),
                    color = accent,
                    length = radius * 0.28f,
                    stroke = stroke,
                )
            }
            else -> {
                drawLine(
                    color = accent.copy(alpha = 0.32f),
                    start = center,
                    end = touch,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                val direction = when (step) {
                    TutorialStep.MOVE -> if (progress < 0.5f) Offset(-1f, 0f) else Offset(1f, 0f)
                    else -> Offset(0f, 1f)
                }
                drawArrowHead(touch, direction, accent, size.minDimension * 0.055f, stroke)
            }
        }

        val pulse = size.minDimension * (0.055f + progress * 0.018f)
        drawCircle(accent.copy(alpha = 0.14f), pulse * 1.7f, touch)
        drawCircle(scheme.surface.copy(alpha = 0.92f), pulse, touch)
        drawCircle(accent, pulse, touch, style = Stroke(stroke))
        drawCircle(accent.copy(alpha = 0.70f), pulse * 0.22f, touch)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    tip: Offset,
    direction: Offset,
    color: androidx.compose.ui.graphics.Color,
    length: Float,
    stroke: Float,
) {
    val normal = Offset(-direction.y, direction.x)
    val base = tip - direction * length
    val path = Path().apply {
        moveTo(base.x + normal.x * length * 0.45f, base.y + normal.y * length * 0.45f)
        lineTo(tip.x, tip.y)
        lineTo(base.x - normal.x * length * 0.45f, base.y - normal.y * length * 0.45f)
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
}
