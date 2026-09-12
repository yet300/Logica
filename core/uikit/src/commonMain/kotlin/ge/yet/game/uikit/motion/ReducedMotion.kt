package ge.yet.game.uikit.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale

/**
 * Shared reduced-motion probe, extracted from the duplicated
 * `scaleFactor == 0f` checks in Block Blast, 2048 and Fruit Merge.
 */
fun MotionDurationScale?.isReducedMotion(): Boolean =
    this?.scaleFactor == 0f

/** Reads the duration scale installed in Compose's coroutine context. */
@Composable
fun rememberReducedMotion(): Boolean =
    rememberCoroutineScope().coroutineContext[MotionDurationScale].isReducedMotion()
