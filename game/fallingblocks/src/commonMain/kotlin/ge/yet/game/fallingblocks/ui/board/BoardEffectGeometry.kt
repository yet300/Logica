package ge.yet.game.fallingblocks.ui.board

import ge.yet.game.fallingblocks.component.game.VisualCell
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class NormalizedEffectCell(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal data class BoardEffectParticle(
    val originX: Float,
    val originY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val type: Tetromino,
)

internal data class BoardEffectTrail(
    val centerX: Float,
    val fromY: Float,
    val toY: Float,
)

internal fun normalizedEffectCell(cell: Cell): NormalizedEffectCell? {
    if (cell.x !in 0 until Board.WIDTH || cell.y !in Board.HIDDEN_ROWS until Board.TOTAL_HEIGHT) {
        return null
    }
    val left = cell.x.toFloat() / Board.WIDTH
    val top = (cell.y - Board.HIDDEN_ROWS).toFloat() / Board.VISIBLE_HEIGHT
    return NormalizedEffectCell(
        left = left,
        top = top,
        right = (cell.x + 1f) / Board.WIDTH,
        bottom = (cell.y - Board.HIDDEN_ROWS + 1f) / Board.VISIBLE_HEIGHT,
    )
}

internal fun hardDropTrails(from: List<Cell>, to: List<Cell>): List<BoardEffectTrail> =
    to.mapIndexedNotNull { index, target ->
        val targetBounds = normalizedEffectCell(target) ?: return@mapIndexedNotNull null
        val source = from.getOrElse(index) { target }
        if (source.x !in 0 until Board.WIDTH || source.y >= Board.TOTAL_HEIGHT) {
            return@mapIndexedNotNull null
        }
        val sourceVisibleY = (source.y - Board.HIDDEN_ROWS)
            .coerceIn(0, Board.VISIBLE_HEIGHT - 1)
        BoardEffectTrail(
            centerX = (targetBounds.left + targetBounds.right) / 2f,
            fromY = (sourceVisibleY + 0.5f) / Board.VISIBLE_HEIGHT,
            toY = (targetBounds.top + targetBounds.bottom) / 2f,
        )
    }

internal fun lineClearParticles(
    eventId: Long,
    cells: List<VisualCell>,
): List<BoardEffectParticle> {
    val visible = cells.mapNotNull { visual ->
        normalizedEffectCell(visual.cell)?.let { bounds -> visual to bounds }
    }
    if (visible.isEmpty()) return emptyList()
    val perCell = minOf(MAX_PARTICLES_PER_CELL, MAX_PARTICLES_TOTAL / visible.size)
        .coerceAtLeast(1)
    return buildList {
        visible.forEachIndexed { cellIndex, (visual, bounds) ->
            repeat(perCell) { particleIndex ->
                if (size >= MAX_PARTICLES_TOTAL) return@buildList
                val angleUnit = deterministicUnit(eventId, cellIndex, particleIndex, 0)
                val speedUnit = deterministicUnit(eventId, cellIndex, particleIndex, 1)
                val sizeUnit = deterministicUnit(eventId, cellIndex, particleIndex, 2)
                val angle = angleUnit * (2f * PI.toFloat())
                val speed = 0.12f + speedUnit * 0.18f
                add(
                    BoardEffectParticle(
                        originX = (bounds.left + bounds.right) / 2f,
                        originY = (bounds.top + bounds.bottom) / 2f,
                        velocityX = cos(angle) * speed,
                        velocityY = sin(angle) * speed - 0.08f,
                        size = 0.008f + sizeUnit * 0.014f,
                        type = visual.type,
                    ),
                )
            }
        }
    }
}

private fun deterministicUnit(
    eventId: Long,
    cellIndex: Int,
    particleIndex: Int,
    channel: Int,
): Float {
    var value = eventId xor (cellIndex.toLong() * -7046029254386353131L)
    value = value xor (particleIndex.toLong() * -4658895280553007687L)
    value = value xor (channel.toLong() * -7723592293110705685L)
    value = (value xor (value ushr 30)) * -4658895280553007687L
    value = (value xor (value ushr 27)) * -7723592293110705685L
    value = value xor (value ushr 31)
    return ((value ushr 40) and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
}

private const val MAX_PARTICLES_PER_CELL = 8
private const val MAX_PARTICLES_TOTAL = 96
