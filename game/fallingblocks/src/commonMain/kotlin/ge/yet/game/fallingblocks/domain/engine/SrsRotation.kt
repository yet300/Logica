package ge.yet.game.fallingblocks.domain.engine

import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino

internal fun ActivePiece.cells(): List<Cell> = localCells(type, rotation).map { offset ->
    Cell(x = origin.x + offset.x, y = origin.y + offset.y)
}

internal fun canOccupy(piece: ActivePiece, board: Board): Boolean = piece.cells().all { cell ->
    cell.x in 0 until Board.WIDTH &&
        cell.y in 0 until Board.TOTAL_HEIGHT &&
        board[cell] == null
}

internal fun landingPiece(piece: ActivePiece, board: Board): ActivePiece {
    var landing = piece
    while (true) {
        val candidate = landing.copy(origin = landing.origin.copy(y = landing.origin.y + 1))
        if (!canOccupy(candidate, board)) return landing
        landing = candidate
    }
}

internal fun tryRotateClockwise(piece: ActivePiece, board: Board): ActivePiece? {
    val to = piece.rotation.clockwise()
    return clockwiseKicks(piece.type, piece.rotation, to)
        .asSequence()
        .map { kick ->
            piece.copy(
                rotation = to,
                origin = Cell(piece.origin.x + kick.x, piece.origin.y + kick.y),
            )
        }
        .firstOrNull { candidate -> canOccupy(candidate, board) }
}

private fun localCells(type: Tetromino, rotation: Rotation): List<Cell> = when (type) {
    Tetromino.I -> I_CELLS.getValue(rotation)
    Tetromino.O -> O_CELLS
    else -> {
        val spawn = SPAWN_CELLS.getValue(type)
        var result = spawn
        repeat(rotation.ordinal) {
            result = result.map { cell -> Cell(x = -cell.y, y = cell.x) }
        }
        result
    }
}

private fun clockwiseKicks(
    type: Tetromino,
    from: Rotation,
    to: Rotation,
): List<Cell> = when (type) {
    Tetromino.O -> ZERO_KICK
    Tetromino.I -> I_CLOCKWISE_KICKS.getValue(from to to)
    else -> JLSTZ_CLOCKWISE_KICKS.getValue(from to to)
}

private fun Rotation.clockwise(): Rotation = Rotation.entries[(ordinal + 1) % Rotation.entries.size]

private val O_CELLS = listOf(Cell(0, -1), Cell(1, -1), Cell(0, 0), Cell(1, 0))

private val I_CELLS = mapOf(
    Rotation.SPAWN to listOf(Cell(-1, 0), Cell(0, 0), Cell(1, 0), Cell(2, 0)),
    Rotation.RIGHT to listOf(Cell(1, -1), Cell(1, 0), Cell(1, 1), Cell(1, 2)),
    Rotation.REVERSE to listOf(Cell(-1, 1), Cell(0, 1), Cell(1, 1), Cell(2, 1)),
    Rotation.LEFT to listOf(Cell(0, -1), Cell(0, 0), Cell(0, 1), Cell(0, 2)),
)

private val SPAWN_CELLS = mapOf(
    Tetromino.J to listOf(Cell(-1, -1), Cell(-1, 0), Cell(0, 0), Cell(1, 0)),
    Tetromino.L to listOf(Cell(1, -1), Cell(-1, 0), Cell(0, 0), Cell(1, 0)),
    Tetromino.S to listOf(Cell(0, -1), Cell(1, -1), Cell(-1, 0), Cell(0, 0)),
    Tetromino.T to listOf(Cell(0, -1), Cell(-1, 0), Cell(0, 0), Cell(1, 0)),
    Tetromino.Z to listOf(Cell(-1, -1), Cell(0, -1), Cell(0, 0), Cell(1, 0)),
)

private val ZERO_KICK = listOf(Cell(0, 0))

// Canonical SRS uses upward-positive Y. These tables are converted once to
// this engine's downward-positive board coordinates.
private val JLSTZ_CLOCKWISE_KICKS = mapOf(
    (Rotation.SPAWN to Rotation.RIGHT) to listOf(
        Cell(0, 0), Cell(-1, 0), Cell(-1, -1), Cell(0, 2), Cell(-1, 2),
    ),
    (Rotation.RIGHT to Rotation.REVERSE) to listOf(
        Cell(0, 0), Cell(1, 0), Cell(1, 1), Cell(0, -2), Cell(1, -2),
    ),
    (Rotation.REVERSE to Rotation.LEFT) to listOf(
        Cell(0, 0), Cell(1, 0), Cell(1, -1), Cell(0, 2), Cell(1, 2),
    ),
    (Rotation.LEFT to Rotation.SPAWN) to listOf(
        Cell(0, 0), Cell(-1, 0), Cell(-1, 1), Cell(0, -2), Cell(-1, -2),
    ),
)

private val I_CLOCKWISE_KICKS = mapOf(
    (Rotation.SPAWN to Rotation.RIGHT) to listOf(
        Cell(0, 0), Cell(-2, 0), Cell(1, 0), Cell(-2, 1), Cell(1, -2),
    ),
    (Rotation.RIGHT to Rotation.REVERSE) to listOf(
        Cell(0, 0), Cell(-1, 0), Cell(2, 0), Cell(-1, -2), Cell(2, 1),
    ),
    (Rotation.REVERSE to Rotation.LEFT) to listOf(
        Cell(0, 0), Cell(2, 0), Cell(-1, 0), Cell(2, -1), Cell(-1, 2),
    ),
    (Rotation.LEFT to Rotation.SPAWN) to listOf(
        Cell(0, 0), Cell(1, 0), Cell(-2, 0), Cell(1, 2), Cell(-2, -1),
    ),
)
