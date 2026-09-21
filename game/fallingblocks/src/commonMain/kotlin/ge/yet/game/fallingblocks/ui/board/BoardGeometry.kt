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
            supportReserve: Float,
            maxBoardWidth: Float,
        ): BoardGeometry {
            require(viewportWidth > 0f && viewportHeight > 0f)
            require(edgeInset >= 0f)
            require(supportReserve >= 0f)
            require(maxBoardWidth > 0f)
            val horizontalReserve = max(edgeInset * 2f, MIN_HORIZONTAL_RESERVE)
            val availableWidth = max(
                MIN_DIMENSION,
                viewportWidth - min(horizontalReserve, viewportWidth - MIN_DIMENSION),
            )
            val verticalReserve = edgeInset * 2f + supportReserve
            val availableHeight = max(
                MIN_DIMENSION * 2f,
                viewportHeight - min(verticalReserve, viewportHeight - MIN_DIMENSION * 2f),
            )
            val aspect = Board.WIDTH.toFloat() / Board.VISIBLE_HEIGHT
            val width = min(maxBoardWidth, min(availableWidth, availableHeight * aspect))
            val height = width / aspect
            return BoardGeometry(
                left = (viewportWidth - width) / 2f,
                top = (viewportHeight - height) / 2f,
                width = width,
                height = height,
            )
        }

        private const val MIN_HORIZONTAL_RESERVE = 64f
        private const val MIN_DIMENSION = 0.5f
    }
}
