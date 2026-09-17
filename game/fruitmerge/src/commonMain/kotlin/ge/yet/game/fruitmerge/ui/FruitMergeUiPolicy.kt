package ge.yet.game.fruitmerge.ui

import ge.yet.game.fruitmerge.domain.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.domain.model.FruitBody
import kotlin.math.sin

internal data class ShakeVisualTransform(
    val translationXDp: Float,
    val rotationDegrees: Float,
)

internal data class MergeSqueeze(val scaleX: Float, val scaleY: Float)

internal fun mergePresentationProgress(elapsedSeconds: Float): Float =
    (elapsedSeconds / MERGE_PRESENTATION_SECONDS).coerceIn(0f, 1f)

internal fun mergeSqueeze(progress: Float): MergeSqueeze {
    val pulse = sin(progress.coerceIn(0f, 1f) * kotlin.math.PI.toFloat())
    return MergeSqueeze(scaleX = 1f + pulse * 0.28f, scaleY = 1f - pulse * 0.20f)
}

internal fun guideAlpha(cooldownSeconds: Float): Float =
    (1f - cooldownSeconds / GUIDE_FADE_SECONDS).coerceIn(0f, 1f)

/**
 * Gentle visual wobble derived from the physical body angle.
 * Settled pile bodies keep spinning in physics, but the renderer only shows
 * a small lean so characters never pinwheel. See step B of the fruit-motion fix.
 */
internal fun visualTiltDegrees(angleRadians: Float): Float {
    if (!angleRadians.isFinite()) return 0f
    val pi = kotlin.math.PI.toFloat()
    val twoPi = 2f * pi
    if (kotlin.math.abs(angleRadians) < 1e-6f) return 0f
    val wrapped = (((angleRadians + pi) % twoPi) + twoPi) % twoPi - pi
    if (kotlin.math.abs(wrapped) < 1e-4f) return 0f
    return (wrapped * (180f / pi)).coerceIn(-MAX_VISUAL_TILT_DEGREES, MAX_VISUAL_TILT_DEGREES)
}

/** Faces stay upright and only lean a little with the body tilt. */
internal fun faceTiltDegrees(bodyTiltDegrees: Float): Float =
    (bodyTiltDegrees * FACE_TILT_FOLLOW).coerceIn(-MAX_FACE_TILT_DEGREES, MAX_FACE_TILT_DEGREES)

/**
 * Back-to-front draw order: big fruits first, small fruits last so a small
 * fruit is never fully hidden under a big neighbour's body or crown
 * (e.g. a blueberry resting on a pineapple). Stable for equal sizes.
 */
internal fun fruitDrawOrder(bodies: List<FruitBody>): List<FruitBody> =
    bodies.sortedWith(
        compareByDescending<FruitBody> { it.level.radius }.thenBy { it.id },
    )

internal fun crateHandleRotation(stepsRemaining: Int, reducedMotion: Boolean): Float =
    shakeVisualTransform(stepsRemaining, reducedMotion).rotationDegrees * 7f

internal fun shakeVisualTransform(
    stepsRemaining: Int,
    reducedMotion: Boolean,
): ShakeVisualTransform {
    if (stepsRemaining <= 0) return ShakeVisualTransform(0f, 0f)
    val bounded = stepsRemaining.coerceAtMost(FruitMergeEngine.SHAKE_DURATION_STEPS)
    val elapsed = FruitMergeEngine.SHAKE_DURATION_STEPS - bounded
    val envelope = 0.45f + 0.55f * bounded.toFloat() / FruitMergeEngine.SHAKE_DURATION_STEPS
    val motionScale = if (reducedMotion) REDUCED_MOTION_SCALE else 1f
    val lateralWave = sin(elapsed * 1.73f) + sin(elapsed * 2.91f) * 0.32f
    val rotationWave = sin(elapsed * 0.91f) + sin(elapsed * 2.17f) * 0.24f
    return ShakeVisualTransform(
        translationXDp = lateralWave * 11f * envelope * motionScale,
        rotationDegrees = rotationWave * 1.7f * envelope * motionScale,
    )
}

private const val REDUCED_MOTION_SCALE: Float = 0.22f
internal const val MERGE_PRESENTATION_SECONDS: Float = 0.145f
private const val GUIDE_FADE_SECONDS: Float = 0.25f
internal const val MAX_VISUAL_TILT_DEGREES: Float = 20f
internal const val FACE_TILT_FOLLOW: Float = 0.25f
internal const val MAX_FACE_TILT_DEGREES: Float = 8f

/**
 * Face-clock wrap shared by the game and result screens. Must stay an exact
 * multiple of every blink interval so wrapping never jumps a blink phase.
 * 117.6 = 28 * 4.2 = 14 * 8.4.
 */
internal const val FACE_CLOCK_WRAP_SECONDS: Float = 117.6f
internal const val FACE_CLOCK_PERIOD_MILLIS: Int = 117_600
