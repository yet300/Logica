package ge.yet.game.fallingblocks.ui.board

import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell

internal data class BeamColumn(
    val x: Int,
    val top: Float,
    val bottom: Float,
)

internal data class HalftoneDot(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val alpha: Float,
)

internal const val MAX_HALFTONE_DOTS = 240

private const val DOTS_PER_CELL = 3

internal fun dropTrailBeam(from: List<Cell>, to: List<Cell>): List<BeamColumn> {
    val tops = mutableMapOf<Int, Int>()
    val bottoms = mutableMapOf<Int, Int>()
    to.forEachIndexed { index, target ->
        if (target.x !in 0 until Board.WIDTH) return@forEachIndexed
        if (target.y !in Board.HIDDEN_ROWS until Board.TOTAL_HEIGHT) return@forEachIndexed
        val source = from.getOrElse(index) { target }
        val top = minOf(source.y, target.y).coerceAtLeast(Board.HIDDEN_ROWS)
        val bottom = maxOf(source.y, target.y)
        if (bottom <= top) return@forEachIndexed
        tops[target.x] = minOf(tops[target.x] ?: top, top)
        bottoms[target.x] = maxOf(bottoms[target.x] ?: bottom, bottom)
    }
    return tops.keys.sorted().mapNotNull { x ->
        val top = tops.getValue(x)
        val bottom = bottoms.getValue(x)
        if (bottom <= top) {
            null
        } else {
            BeamColumn(
                x = x,
                top = (top - Board.HIDDEN_ROWS).toFloat() / Board.VISIBLE_HEIGHT,
                bottom = (bottom - Board.HIDDEN_ROWS + 1f) / Board.VISIBLE_HEIGHT,
            )
        }
    }
}

internal fun trailHalftoneDots(
    eventId: Long,
    columns: List<BeamColumn>,
): List<HalftoneDot> {
    if (columns.isEmpty()) return emptyList()
    val dots = ArrayList<HalftoneDot>(128)
    val stepX = 1f / (Board.WIDTH * DOTS_PER_CELL)
    val stepY = 1f / (Board.VISIBLE_HEIGHT * DOTS_PER_CELL)
    columns.forEachIndexed { colIndex, column ->
        val left = column.x.toFloat() / Board.WIDTH
        val right = (column.x + 1f) / Board.WIDTH
        val span = (column.bottom - column.top).coerceAtLeast(1e-4f)
        var iy = 0
        var y = column.top + stepY / 2f
        while (y < column.bottom && dots.size < MAX_HALFTONE_DOTS) {
            val depth = ((y - column.top) / span).coerceIn(0f, 1f)
            val density = 0.15f + 0.85f * depth
            var ix = 0
            var x = left + stepX / 2f
            while (x < right && dots.size < MAX_HALFTONE_DOTS) {
                if (halftoneUnit(eventId, colIndex, ix, iy) < density) {
                    dots += HalftoneDot(
                        cx = x,
                        cy = y,
                        radius = 0.004f + 0.009f * depth,
                        alpha = 0.30f + 0.55f * depth,
                    )
                }
                ix++
                x += stepX
            }
            iy++
            y += stepY
        }
        if (dots.size >= MAX_HALFTONE_DOTS) return dots
    }
    return dots
}

private fun halftoneUnit(eventId: Long, col: Int, ix: Int, iy: Int): Float {
    var value = eventId xor (col.toLong() * -5755276462620442387L)
    value = value xor (ix.toLong() * -5461769869401523961L)
    value = value xor (iy.toLong() * -5805269444623933541L)
    value = (value xor (value ushr 30)) * -5461769869401523961L
    value = (value xor (value ushr 27)) * -5805269444623933541L
    value = value xor (value ushr 31)
    return ((value ushr 40) and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
}
