package ge.yet.game.fallingblocks.ui.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

internal fun Modifier.fallingBlocksGestures(
    enabled: Boolean,
    cellSize: Dp,
    onEvent: (GestureEvent) -> Unit,
): Modifier = pointerInput(enabled, cellSize) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val classifier = GestureClassifier(
            cellSizePx = cellSize.toPx().coerceAtLeast(1f),
            touchSlopPx = viewConfiguration.touchSlop,
            hardDropVelocityPxPerSecond = 1_200f * density,
        )
        classifier.down(down.position.x, down.position.y, down.uptimeMillis)
        down.consume()
        var finished = false
        while (!finished) {
            val pointerEvent = awaitPointerEvent()
            if (pointerEvent.changes.size != 1) {
                classifier.cancelForAdditionalPointer()
                pointerEvent.changes.forEach { it.consume() }
                finished = true
            } else {
                val change = pointerEvent.changes.single()
                val events = if (change.changedToUp()) {
                    finished = true
                    classifier.up(change.position.x, change.position.y, change.uptimeMillis)
                } else {
                    classifier.move(change.position.x, change.position.y, change.uptimeMillis)
                }
                events.forEach(onEvent)
                change.consume()
            }
        }
    }
}
