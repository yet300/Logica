package ge.yet.game.fallingblocks.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ge.yet.game.fallingblocks.domain.engine.cells
import ge.yet.game.fallingblocks.domain.engine.landingPiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.Tetromino

@Composable
internal fun CrtBoard(
    state: FallingBlocksState,
    spatialEffectsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val cellSize = size.width / Board.WIDTH
        val transparent = scheme.onSurface.copy(alpha = 0f)
        val boardCorner = CornerRadius(cellSize * 0.16f)

        drawRoundRect(
            color = scheme.surfaceContainerLowest,
            cornerRadius = boardCorner,
        )
        drawRoundRect(
            color = scheme.outlineVariant.copy(alpha = 0.72f),
            cornerRadius = boardCorner,
            style = Stroke(width = maxOf(1f, cellSize * 0.055f)),
        )

        drawBoardGrid(cellSize, scheme.outlineVariant.copy(alpha = 0.11f))

        state.board.cells.forEachIndexed { index, type ->
            if (type != null) {
                val cell = Cell(index % Board.WIDTH, index / Board.WIDTH)
                if (cell.y >= Board.HIDDEN_ROWS) drawPieceCell(cell, type, cellSize, scheme)
            }
        }

        val ghost = landingPiece(state.active, state.board)
        if (ghost.origin != state.active.origin) {
            ghost.cells().forEach { cell ->
                if (cell.y >= Board.HIDDEN_ROWS) {
                    drawGhostCell(cell, state.active.type, cellSize, scheme)
                }
            }
        }

        state.active.cells().forEach { cell ->
            if (cell.y >= Board.HIDDEN_ROWS) {
                if (spatialEffectsEnabled) {
                    drawChannelEcho(cell, state.active.type, cellSize, scheme)
                }
                drawPieceCell(cell, state.active.type, cellSize, scheme)
            }
        }

        val scanlineHeight = 4.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to transparent,
                    0.5f to transparent,
                    0.5f to scheme.onSurface.copy(alpha = 0.055f),
                    1f to scheme.onSurface.copy(alpha = 0.055f),
                ),
                startY = 0f,
                endY = scanlineHeight,
                tileMode = TileMode.Repeated,
            ),
        )
    }
}

private fun DrawScope.drawBoardGrid(cellSize: Float, color: Color) {
    val stroke = maxOf(1f, cellSize * 0.018f)
    for (x in 1 until Board.WIDTH) {
        val px = x * cellSize
        drawLine(color, Offset(px, 0f), Offset(px, size.height), stroke)
    }
    for (y in 1 until Board.VISIBLE_HEIGHT) {
        val py = y * cellSize
        drawLine(color, Offset(0f, py), Offset(size.width, py), stroke)
    }
}

private fun DrawScope.drawPieceCell(
    cell: Cell,
    type: Tetromino,
    cellSize: Float,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val style = type.colors(scheme)
    val visibleY = cell.y - Board.HIDDEN_ROWS
    val gap = cellSize * 0.075f
    val topLeft = Offset(cell.x * cellSize + gap, visibleY * cellSize + gap)
    val tileSize = Size(cellSize - gap * 2f, cellSize - gap * 2f)
    val radius = CornerRadius(cellSize * 0.14f)
    drawRoundRect(style.fill, topLeft, tileSize, radius)
    drawRoundRect(
        style.highlight.copy(alpha = 0.64f),
        topLeft = topLeft + Offset(gap, gap),
        size = Size(tileSize.width - gap * 2f, cellSize * 0.10f),
        cornerRadius = radius,
    )
    drawRoundRect(
        color = style.outline.copy(alpha = 0.72f),
        topLeft = topLeft,
        size = tileSize,
        cornerRadius = radius,
        style = Stroke(maxOf(1f, cellSize * 0.045f)),
    )
}

private fun DrawScope.drawGhostCell(
    cell: Cell,
    type: Tetromino,
    cellSize: Float,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val style = type.colors(scheme)
    val visibleY = cell.y - Board.HIDDEN_ROWS
    val gap = cellSize * 0.12f
    drawRoundRect(
        color = style.fill.copy(alpha = 0.14f),
        topLeft = Offset(cell.x * cellSize + gap, visibleY * cellSize + gap),
        size = Size(cellSize - gap * 2f, cellSize - gap * 2f),
        cornerRadius = CornerRadius(cellSize * 0.12f),
        style = Stroke(
            width = maxOf(1f, cellSize * 0.055f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(cellSize * 0.16f, cellSize * 0.11f)),
        ),
    )
}

private fun DrawScope.drawChannelEcho(
    cell: Cell,
    type: Tetromino,
    cellSize: Float,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val visibleY = cell.y - Board.HIDDEN_ROWS
    val gap = cellSize * 0.10f
    val echoOffset = cellSize * 0.035f
    val size = Size(cellSize - gap * 2f, cellSize - gap * 2f)
    val base = Offset(cell.x * cellSize + gap, visibleY * cellSize + gap)
    val radius = CornerRadius(cellSize * 0.12f)
    drawRoundRect(
        type.colors(scheme).fill.copy(alpha = 0.10f),
        topLeft = base - Offset(echoOffset, 0f),
        size = size,
        cornerRadius = radius,
    )
    drawRoundRect(
        scheme.tertiary.copy(alpha = 0.08f),
        topLeft = base + Offset(echoOffset, 0f),
        size = size,
        cornerRadius = radius,
    )
}
