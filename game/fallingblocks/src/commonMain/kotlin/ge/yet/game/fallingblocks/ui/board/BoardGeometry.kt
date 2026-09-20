package ge.yet.game.fallingblocks.ui.board

import ge.yet.game.fallingblocks.domain.model.Board
import kotlin.math.max
import kotlin.math.min

internal data class BoardGeometry(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
    val cellSize: Float get() = width / Board.WIDTH

    companion object {
        fun fit(
            viewportWidth: Float,
            viewportHeight: Float,
            edgeInset: Float,
        ): BoardGeometry {
            require(viewportWidth > 0f && viewportHeight > 0f)
            require(edgeInset >= 0f)
            val availableWidth = max(1f, viewportWidth - min(edgeInset * 2f, viewportWidth - 1f))
            val availableHeight = max(1f, viewportHeight - min(edgeInset * 2f, viewportHeight - 1f))
            val aspect = Board.WIDTH.toFloat() / Board.VISIBLE_HEIGHT
            val width = min(availableWidth, availableHeight * aspect)
            val height = width / aspect
            return BoardGeometry(
                left = (viewportWidth - width) / 2f,
                top = (viewportHeight - height) / 2f,
                width = width,
                height = height,
            )
        }
    }
}
