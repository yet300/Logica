package ge.yet.game.fallingblocks.ui.input

import kotlin.math.abs

internal sealed interface GestureEvent {
    data object Rotate : GestureEvent
    data class MoveHorizontal(val cells: Int) : GestureEvent
    data class SoftDrop(val cells: Int) : GestureEvent
    data object HardDrop : GestureEvent
}

internal class GestureClassifier(
    private val cellSizePx: Float,
    private val touchSlopPx: Float,
    private val hardDropVelocityPxPerSecond: Float,
) {
    private enum class Axis { HORIZONTAL, VERTICAL }
    private var active = false
    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var horizontalRemainder = 0f
    private var verticalRemainder = 0f
    private var axis: Axis? = null

    fun down(x: Float, y: Float, timeMillis: Long) {
        active = true
        startX = x; startY = y; lastX = x; lastY = y; startTime = timeMillis
        horizontalRemainder = 0f; verticalRemainder = 0f; axis = null
    }

    fun move(x: Float, y: Float, timeMillis: Long): List<GestureEvent> {
        if (!active) return emptyList()
        val totalX = x - startX
        val totalY = y - startY
        if (axis == null && maxOf(abs(totalX), abs(totalY)) >= touchSlopPx) {
            axis = if (abs(totalX) >= abs(totalY)) Axis.HORIZONTAL else Axis.VERTICAL
        }
        val dx = x - lastX
        val dy = y - lastY
        lastX = x; lastY = y
        return when (axis) {
            Axis.HORIZONTAL -> emitHorizontal(dx)
            Axis.VERTICAL -> emitVertical(dy)
            null -> emptyList()
        }
    }

    fun up(x: Float, y: Float, timeMillis: Long): List<GestureEvent> {
        if (!active) return emptyList()
        active = false
        val duration = (timeMillis - startTime).coerceAtLeast(1)
        val velocity = (y - startY) * 1_000f / duration
        return when {
            axis == null && abs(x - startX) < touchSlopPx && abs(y - startY) < touchSlopPx -> listOf(GestureEvent.Rotate)
            axis == Axis.VERTICAL && y > startY && velocity >= hardDropVelocityPxPerSecond -> listOf(GestureEvent.HardDrop)
            else -> emptyList()
        }
    }

    fun cancelForAdditionalPointer(): List<GestureEvent> {
        active = false
        return emptyList()
    }

    private fun emitHorizontal(delta: Float): List<GestureEvent> {
        horizontalRemainder += delta
        val cells = (horizontalRemainder / cellSizePx).toInt()
        if (cells == 0) return emptyList()
        horizontalRemainder -= cells * cellSizePx
        return listOf(GestureEvent.MoveHorizontal(cells))
    }

    private fun emitVertical(delta: Float): List<GestureEvent> {
        verticalRemainder += delta.coerceAtLeast(0f)
        val cells = (verticalRemainder / cellSizePx).toInt()
        if (cells == 0) return emptyList()
        verticalRemainder -= cells * cellSizePx
        return listOf(GestureEvent.SoftDrop(cells))
    }
}
