package ge.yet.game.fallingblocks.domain.engine

import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.domain.model.GameTransition
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.math.abs

internal object DefaultFallingBlocksEngine : FallingBlocksEngine {
    override fun initial(seed: Long, runId: Long): FallingBlocksState {
        val draw = SevenBag.initial(RandomState(seed)).draw(PREVIEW_SIZE + 1)
        return FallingBlocksState(
            board = Board.empty(),
            active = ActivePiece(
                type = draw.items.first(),
                rotation = Rotation.SPAWN,
                origin = SPAWN_ORIGIN,
            ),
            preview = draw.items.drop(1),
            bag = draw.next,
            score = 0,
            level = 1,
            lines = 0,
            combo = -1,
            backToBack = false,
            gravityRemainingMillis = gravityMillis(level = 1),
            lockRemainingMillis = LOCK_DELAY_MILLIS,
            lockResetCount = 0,
            lastActionWasRotation = false,
            revivesUsed = 0,
            runId = runId,
            phase = GamePhase.PLAYING,
        )
    }

    override fun reduce(state: FallingBlocksState, action: GameAction): GameTransition {
        if (state.phase == GamePhase.TERMINAL) return GameTransition(state, emptyList())
        return when (action) {
            GameAction.RotateClockwise -> rotate(state)
            is GameAction.MoveHorizontal -> moveHorizontal(state, action.cells)
            is GameAction.SoftDrop -> softDrop(state, action.cells)
            GameAction.HardDrop -> hardDrop(state)
            is GameAction.AdvanceTime -> advanceTime(state, action.millis)
            GameAction.Revive,
            -> GameTransition(state, emptyList())
        }
    }

    private fun rotate(state: FallingBlocksState): GameTransition {
        val rotated = tryRotateClockwise(state.active, state.board)
            ?: return GameTransition(state, listOf(GameFact.Blocked))
        return GameTransition(
            state = resetLockAfterGroundedAction(
                before = state,
                after = state.copy(active = rotated, lastActionWasRotation = true),
            ),
            facts = listOf(GameFact.Rotated),
        )
    }

    private fun moveHorizontal(state: FallingBlocksState, requestedCells: Int): GameTransition {
        val direction = requestedCells.compareTo(0)
        if (direction == 0) return GameTransition(state, emptyList())

        val attempts = minOf(abs(requestedCells.toLong()), Board.WIDTH.toLong()).toInt()
        var active = state.active
        var moved = 0
        for (ignored in 0 until attempts) {
            val candidate = active.copy(
                origin = active.origin.copy(x = active.origin.x + direction),
            )
            if (!canOccupy(candidate, state.board)) break
            active = candidate
            moved += direction
        }
        if (moved == 0) return GameTransition(state, listOf(GameFact.Blocked))
        return GameTransition(
            state = resetLockAfterGroundedAction(
                before = state,
                after = state.copy(active = active, lastActionWasRotation = false),
            ),
            facts = listOf(GameFact.Moved(horizontalCells = moved, downwardCells = 0)),
        )
    }

    private fun softDrop(state: FallingBlocksState, requestedCells: Int): GameTransition {
        if (requestedCells <= 0) return GameTransition(state, emptyList())
        var active = state.active
        var moved = 0
        repeat(minOf(requestedCells, Board.TOTAL_HEIGHT)) {
            val candidate = active.movedDown()
            if (canOccupy(candidate, state.board)) {
                active = candidate
                moved += 1
            }
        }
        if (moved == 0) return GameTransition(state, listOf(GameFact.Blocked))
        return GameTransition(
            state = state.copy(
                active = active,
                score = saturatingAdd(state.score, moved.toLong()),
                gravityRemainingMillis = gravityMillis(state.level),
                lastActionWasRotation = false,
            ),
            facts = listOf(GameFact.Moved(horizontalCells = 0, downwardCells = moved)),
        )
    }

    private fun hardDrop(state: FallingBlocksState): GameTransition {
        var active = state.active
        var moved = 0
        while (moved < Board.TOTAL_HEIGHT) {
            val candidate = active.movedDown()
            if (!canOccupy(candidate, state.board)) break
            active = candidate
            moved += 1
        }
        val dropped = state.copy(
            active = active,
            score = saturatingAdd(state.score, saturatingMultiply(2, moved.toLong())),
            lastActionWasRotation = if (moved == 0) state.lastActionWasRotation else false,
        )
        val locked = lock(dropped)
        return GameTransition(
            state = locked.state,
            facts = listOf(GameFact.HardDropped(moved)) + locked.facts,
        )
    }

    private fun advanceTime(state: FallingBlocksState, requestedMillis: Int): GameTransition {
        if (requestedMillis <= 0) return GameTransition(state, emptyList())
        var current = state
        var remaining = minOf(requestedMillis, MAX_ADVANCE_MILLIS)
        var eventCount = 0
        val facts = mutableListOf<GameFact>()
        while (remaining > 0 && current.phase == GamePhase.PLAYING && eventCount < MAX_TIMED_EVENTS) {
            if (isGrounded(current.active, current.board)) {
                val wait = current.lockRemainingMillis.coerceAtLeast(0)
                if (remaining < wait) {
                    current = current.copy(lockRemainingMillis = wait - remaining)
                    remaining = 0
                } else {
                    remaining -= wait
                    val locked = lock(current)
                    current = locked.state
                    facts += locked.facts
                    eventCount += 1
                }
            } else {
                val wait = current.gravityRemainingMillis.coerceAtLeast(0)
                if (remaining < wait) {
                    current = current.copy(gravityRemainingMillis = wait - remaining)
                    remaining = 0
                } else {
                    remaining -= wait
                    current = current.copy(
                        active = current.active.movedDown(),
                        gravityRemainingMillis = gravityMillis(current.level),
                        lockRemainingMillis = LOCK_DELAY_MILLIS,
                        lastActionWasRotation = false,
                    )
                    facts += GameFact.Moved(horizontalCells = 0, downwardCells = 1)
                    eventCount += 1
                }
            }
        }
        return GameTransition(current, facts)
    }

    private fun lock(state: FallingBlocksState): GameTransition {
        val wasTSpin = isTSpin(state)
        val lockedCells = state.active.cells()
        val merged = state.board.cells.toMutableList()
        lockedCells.forEach { cell -> merged[cell.y * Board.WIDTH + cell.x] = state.active.type }
        val mergedBoard = Board(merged)
        val fullRows = (Board.HIDDEN_ROWS until Board.TOTAL_HEIGHT).filter { y ->
            (0 until Board.WIDTH).all { x -> mergedBoard[Cell(x, y)] != null }
        }
        val collapsedBoard = collapseRows(mergedBoard, fullRows)
        val perfect = fullRows.isNotEmpty() && collapsedBoard.cells.all { it == null }
        val clearKind = clearKind(wasTSpin, fullRows.size)
        val nextCombo = if (fullRows.isEmpty()) -1 else state.combo + 1
        val breakdown = score(
            kind = clearKind,
            level = state.level,
            comboIndex = nextCombo,
            backToBack = state.backToBack,
            perfect = perfect,
        )
        val totalLines = state.lines + fullRows.size
        val nextLevel = totalLines / 10 + 1
        val nextBackToBack = when {
            fullRows.isEmpty() -> state.backToBack
            clearKind.backToBackEligible -> true
            else -> false
        }
        val baseFacts = buildList {
            add(GameFact.Locked)
            if (fullRows.isNotEmpty()) add(GameFact.LinesCleared(fullRows.size, perfect))
            if (nextLevel != state.level) add(GameFact.LevelChanged(nextLevel))
        }
        val resolved = state.copy(
            board = collapsedBoard,
            score = saturatingAdd(state.score, breakdown.total),
            level = nextLevel,
            lines = totalLines,
            combo = nextCombo,
            backToBack = nextBackToBack,
            gravityRemainingMillis = gravityMillis(nextLevel),
            lockRemainingMillis = LOCK_DELAY_MILLIS,
            lockResetCount = 0,
            lastActionWasRotation = false,
        )
        if (lockedCells.any { it.y < Board.HIDDEN_ROWS }) {
            return GameTransition(
                state = resolved.copy(phase = GamePhase.TERMINAL),
                facts = baseFacts + GameFact.ToppedOut,
            )
        }

        val refill = resolved.bag.draw(1)
        val spawned = resolved.copy(
            active = ActivePiece(
                type = resolved.preview.first(),
                rotation = Rotation.SPAWN,
                origin = SPAWN_ORIGIN,
            ),
            preview = resolved.preview.drop(1) + refill.items.single(),
            bag = refill.next,
        )
        return if (canOccupy(spawned.active, spawned.board)) {
            GameTransition(spawned, baseFacts)
        } else {
            GameTransition(
                state = spawned.copy(phase = GamePhase.TERMINAL),
                facts = baseFacts + GameFact.ToppedOut,
            )
        }
    }

    private fun resetLockAfterGroundedAction(
        before: FallingBlocksState,
        after: FallingBlocksState,
    ): FallingBlocksState = if (
        isGrounded(before.active, before.board) && before.lockResetCount < MAX_LOCK_RESETS
    ) {
        after.copy(
            lockRemainingMillis = LOCK_DELAY_MILLIS,
            lockResetCount = before.lockResetCount + 1,
        )
    } else {
        after
    }

    private fun isTSpin(state: FallingBlocksState): Boolean {
        if (state.active.type != ge.yet.game.fallingblocks.domain.model.Tetromino.T ||
            !state.lastActionWasRotation
        ) {
            return false
        }
        val pivot = state.active.origin
        val corners = listOf(
            Cell(pivot.x - 1, pivot.y - 1),
            Cell(pivot.x + 1, pivot.y - 1),
            Cell(pivot.x - 1, pivot.y + 1),
            Cell(pivot.x + 1, pivot.y + 1),
        )
        return corners.count { cell ->
            cell.x !in 0 until Board.WIDTH ||
                cell.y !in 0 until Board.TOTAL_HEIGHT ||
                state.board[cell] != null
        } >= 3
    }

    private fun collapseRows(board: Board, fullRows: List<Int>): Board {
        if (fullRows.isEmpty()) return board
        val retainedRows = (0 until Board.TOTAL_HEIGHT)
            .filterNot(fullRows::contains)
            .map { y -> board.cells.subList(y * Board.WIDTH, (y + 1) * Board.WIDTH) }
        val emptyRows = List(fullRows.size) { List<Tetromino?>(Board.WIDTH) { null } }
        return Board((emptyRows + retainedRows).flatten())
    }

    private fun clearKind(tSpin: Boolean, lines: Int): ClearKind = if (tSpin) {
        when (lines) {
            0 -> ClearKind.T_SPIN
            1 -> ClearKind.T_SPIN_SINGLE
            2 -> ClearKind.T_SPIN_DOUBLE
            else -> ClearKind.T_SPIN_TRIPLE
        }
    } else {
        when (lines) {
            0 -> ClearKind.NONE
            1 -> ClearKind.SINGLE
            2 -> ClearKind.DOUBLE
            3 -> ClearKind.TRIPLE
            else -> ClearKind.FOUR
        }
    }

    private fun ActivePiece.movedDown(): ActivePiece = copy(origin = origin.copy(y = origin.y + 1))

    private fun isGrounded(piece: ActivePiece, board: Board): Boolean = !canOccupy(piece.movedDown(), board)

    private const val PREVIEW_SIZE: Int = 5
    private val SPAWN_ORIGIN: Cell = Cell(4, Board.HIDDEN_ROWS)
}

internal val GRAVITY_MILLIS: IntArray = intArrayOf(
    800, 720, 630, 550, 470, 400, 340, 290, 240, 200,
    170, 145, 125, 110, 100, 90, 85, 80,
)

internal const val LOCK_DELAY_MILLIS: Int = 500
internal const val MAX_LOCK_RESETS: Int = 15
private const val MAX_TIMED_EVENTS: Int = 1_024
private const val MAX_ADVANCE_MILLIS: Int = 60_000

internal fun gravityMillis(level: Int): Int = GRAVITY_MILLIS[(level - 1).coerceIn(GRAVITY_MILLIS.indices)]
