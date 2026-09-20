package ge.yet.game.fallingblocks

import ge.yet.game.fallingblocks.domain.engine.RandomState
import ge.yet.game.fallingblocks.domain.engine.SevenBag
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino

internal fun gameFixture(
    board: Board = Board.empty(),
    active: ActivePiece = ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(3, 2)),
    score: Long = 0,
    level: Int = 1,
    lines: Int = 0,
    combo: Int = -1,
    backToBack: Boolean = false,
    gravityRemainingMillis: Int = 800,
    lockRemainingMillis: Int = 500,
    lockResetCount: Int = 0,
    lastActionWasRotation: Boolean = false,
    revivesUsed: Int = 0,
    phase: GamePhase = GamePhase.PLAYING,
): FallingBlocksState {
    val previewDraw = SevenBag.initial(RandomState(1)).draw(5)
    return FallingBlocksState(
        board = board,
        active = active,
        preview = previewDraw.items,
        bag = previewDraw.next,
        score = score,
        level = level,
        lines = lines,
        combo = combo,
        backToBack = backToBack,
        gravityRemainingMillis = gravityRemainingMillis,
        lockRemainingMillis = lockRemainingMillis,
        lockResetCount = lockResetCount,
        lastActionWasRotation = lastActionWasRotation,
        revivesUsed = revivesUsed,
        runId = 1,
        phase = phase,
    )
}

internal fun boardWith(vararg occupied: Pair<Cell, Tetromino>): Board {
    val cells = MutableList<Tetromino?>(Board.WIDTH * Board.TOTAL_HEIGHT) { null }
    occupied.forEach { (cell, piece) -> cells[cell.y * Board.WIDTH + cell.x] = piece }
    return Board(cells)
}

internal fun restingFixture(
    lockRemainingMillis: Int = 500,
    lockResetCount: Int = 0,
): FallingBlocksState = gameFixture(
    active = ActivePiece(Tetromino.O, Rotation.SPAWN, Cell(3, Board.TOTAL_HEIGHT - 1)),
    lockRemainingMillis = lockRemainingMillis,
    lockResetCount = lockResetCount,
)

internal fun terminalFixture(): FallingBlocksState = gameFixture(phase = GamePhase.TERMINAL)

internal fun spawnBlockedFixture(): FallingBlocksState = gameFixture(
    board = boardWith(Cell(4, 2) to Tetromino.Z),
)

internal fun hiddenRowLockFixture(): FallingBlocksState = gameFixture(
    active = ActivePiece(Tetromino.O, Rotation.SPAWN, Cell(3, 0)),
)
