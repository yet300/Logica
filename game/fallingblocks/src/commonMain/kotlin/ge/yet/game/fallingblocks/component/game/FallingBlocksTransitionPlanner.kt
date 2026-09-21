package ge.yet.game.fallingblocks.component.game

import ge.yet.game.fallingblocks.domain.engine.cells
import ge.yet.game.fallingblocks.domain.engine.landingPiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact

internal class FallingBlocksTransitionPlanner {
    fun plan(
        before: FallingBlocksState,
        action: GameAction,
        after: FallingBlocksState,
        facts: List<GameFact>,
        nextId: Long,
    ): FallingBlocksVisualEvent? {
        require(nextId > 0L)
        require(after.runId == before.runId)

        val clear = facts.filterIsInstance<GameFact.LinesCleared>().singleOrNull()
        val isHardDrop = action == GameAction.HardDrop && facts.any { it is GameFact.HardDropped }
        if (clear == null && !isHardDrop) return null
        require(nextId < Long.MAX_VALUE) { "Visual event ID exhausted" }

        val landing = if (action == GameAction.HardDrop) {
            landingPiece(before.active, before.board)
        } else {
            before.active
        }
        if (clear != null) {
            val merged = before.board.cells.toMutableList()
            landing.cells().forEach { cell ->
                merged[cell.y * Board.WIDTH + cell.x] = landing.type
            }
            val rowSet = clear.rows.toSet()
            val cells = clear.rows.flatMap { y ->
                (0 until Board.WIDTH).mapNotNull { x ->
                    val type = merged[y * Board.WIDTH + x] ?: return@mapNotNull null
                    VisualCell(
                        cell = ge.yet.game.fallingblocks.domain.model.Cell(x, y),
                        type = type,
                    )
                }
            }
            check(cells.size == rowSet.size * Board.WIDTH) {
                "Every cleared row must be complete before collapse"
            }
            return FallingBlocksVisualEvent.LineClear(
                id = nextId,
                rows = clear.rows.toList(),
                cells = cells,
            )
        }

        return FallingBlocksVisualEvent.HardDrop(
            id = nextId,
            type = before.active.type,
            from = before.active.cells().toList(),
            to = landing.cells().toList(),
        )
    }
}
