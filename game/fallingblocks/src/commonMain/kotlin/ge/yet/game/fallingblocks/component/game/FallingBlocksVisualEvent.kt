package ge.yet.game.fallingblocks.component.game

import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Tetromino

internal sealed interface FallingBlocksVisualEvent {
    val id: Long

    data class HardDrop(
        override val id: Long,
        val type: Tetromino,
        val from: List<Cell>,
        val to: List<Cell>,
    ) : FallingBlocksVisualEvent

    data class LineClear(
        override val id: Long,
        val rows: List<Int>,
        val cells: List<VisualCell>,
    ) : FallingBlocksVisualEvent
}

internal data class VisualCell(
    val cell: Cell,
    val type: Tetromino,
)
