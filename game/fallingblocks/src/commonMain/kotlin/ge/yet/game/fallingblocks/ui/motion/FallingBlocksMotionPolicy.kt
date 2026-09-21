package ge.yet.game.fallingblocks.ui.motion

import androidx.compose.runtime.Composable
import ge.yet.game.uikit.motion.rememberReducedMotion

internal data class FallingBlocksMotionPolicy(
    val spatialMotionEnabled: Boolean,
    val moveDurationMillis: Int,
    val rotateDurationMillis: Int,
    val lockDurationMillis: Int,
    val lineClearDurationMillis: Int,
    val levelDurationMillis: Int,
    val scoreDurationMillis: Int,
    val gameOverDurationMillis: Int,
    val opacityDurationMillis: Int,
    val hardDropBeamDurationMillis: Int,
    val hardDropImpactDurationMillis: Int,
    val lineClearFlashDurationMillis: Int,
    val lineClearCollapseDurationMillis: Int,
)

internal fun fallingBlocksMotionPolicy(reducedMotion: Boolean): FallingBlocksMotionPolicy =
    if (reducedMotion) {
        FallingBlocksMotionPolicy(
            spatialMotionEnabled = false,
            moveDurationMillis = 0,
            rotateDurationMillis = 0,
            lockDurationMillis = 0,
            lineClearDurationMillis = 0,
            levelDurationMillis = 0,
            scoreDurationMillis = 0,
            gameOverDurationMillis = 0,
            opacityDurationMillis = 72,
            hardDropBeamDurationMillis = 0,
            hardDropImpactDurationMillis = 80,
            lineClearFlashDurationMillis = 90,
            lineClearCollapseDurationMillis = 0,
        )
    } else {
        FallingBlocksMotionPolicy(
            spatialMotionEnabled = true,
            moveDurationMillis = 70,
            rotateDurationMillis = 110,
            lockDurationMillis = 90,
            lineClearDurationMillis = 140,
            levelDurationMillis = 180,
            scoreDurationMillis = 160,
            gameOverDurationMillis = 280,
            opacityDurationMillis = 90,
            hardDropBeamDurationMillis = 180,
            hardDropImpactDurationMillis = 140,
            lineClearFlashDurationMillis = 180,
            lineClearCollapseDurationMillis = 160,
        )
    }

@Composable
internal fun rememberFallingBlocksMotionPolicy(): FallingBlocksMotionPolicy =
    fallingBlocksMotionPolicy(rememberReducedMotion())
