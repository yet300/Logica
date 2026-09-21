package ge.yet.game.fallingblocks.component.result

import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlinx.serialization.Serializable

@Serializable
internal data class FallingBlocksResultSnapshot(
    val boardCells: List<Int?>,
    val activeTypeOrdinal: Int,
    val activeRotationOrdinal: Int,
    val activeX: Int,
    val activeY: Int,
    val score: Long,
    val bestScore: Long,
    val runId: Long,
    val revivesUsed: Int,
) {
    init {
        require(boardCells.size == Board.WIDTH * Board.TOTAL_HEIGHT)
        require(boardCells.all { it == null || it in Tetromino.entries.indices })
        require(activeTypeOrdinal in Tetromino.entries.indices)
        require(activeRotationOrdinal in Rotation.entries.indices)
        require(score >= 0L)
        require(bestScore >= 0L)
        require(runId > 0L)
        require(revivesUsed >= 0)
    }

    fun board(): Board = Board(boardCells.map { ordinal -> ordinal?.let(Tetromino.entries::get) })

    fun activePiece(): ActivePiece = ActivePiece(
        type = Tetromino.entries[activeTypeOrdinal],
        rotation = Rotation.entries[activeRotationOrdinal],
        origin = Cell(activeX, activeY),
    )

    companion object {
        fun from(state: FallingBlocksState, bestScore: Long): FallingBlocksResultSnapshot =
            FallingBlocksResultSnapshot(
                boardCells = state.board.cells.map { it?.ordinal },
                activeTypeOrdinal = state.active.type.ordinal,
                activeRotationOrdinal = state.active.rotation.ordinal,
                activeX = state.active.origin.x,
                activeY = state.active.origin.y,
                score = state.score,
                bestScore = maxOf(bestScore, state.score),
                runId = state.runId,
                revivesUsed = state.revivesUsed,
            )
    }
}
