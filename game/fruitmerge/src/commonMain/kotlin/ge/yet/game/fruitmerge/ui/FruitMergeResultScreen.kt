package ge.yet.game.fruitmerge.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fruitmerge.component.result.FruitMergeResultComponent

@Composable
internal fun FruitMergeResultScreen(
    component: FruitMergeResultComponent,
    modifier: Modifier = Modifier,
) {
    val model by component.model.subscribeAsState()
    val reducedMotion = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor == 0f
    // Infinite transitions stay idle-friendly for UI tests, unlike a raw frame loop.
    val faceTransition = rememberInfiniteTransition(label = "resultFace")
    val animatedFaceTime by faceTransition.animateFloat(
        initialValue = 0f,
        targetValue = FACE_CLOCK_WRAP_SECONDS,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = FACE_CLOCK_PERIOD_MILLIS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "resultFaceTime",
    )
    val faceTimeSeconds = if (reducedMotion) 0f else animatedFaceTime
    FruitMergeResultContent(
        snapshot = model.snapshot,
        faceTimeSeconds = faceTimeSeconds,
        reducedMotion = reducedMotion,
        onNewGame = component::onNewGame,
        modifier = modifier,
    )
}

private const val FACE_CLOCK_WRAP_SECONDS: Float = 120f
private const val FACE_CLOCK_PERIOD_MILLIS: Int = 120_000
