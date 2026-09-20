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
            lockRemainingMillis = LOCK_DELAY_MILLIS,
            lockResetCount = 0,
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
            is GameAction.SoftDrop,
            GameAction.HardDrop,
            is GameAction.AdvanceTime,
            GameAction.Revive,
            -> GameTransition(state, emptyList())
        }
    }

    private fun rotate(state: FallingBlocksState): GameTransition {
        val rotated = tryRotateClockwise(state.active, state.board)
            ?: return GameTransition(state, listOf(GameFact.Blocked))
        return GameTransition(
            state = state.copy(active = rotated),
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
            state = state.copy(active = active),
            facts = listOf(GameFact.Moved(horizontalCells = moved, downwardCells = 0)),
        )
    }

    private const val PREVIEW_SIZE: Int = 5
    private const val LOCK_DELAY_MILLIS: Int = 500
    private val SPAWN_ORIGIN: Cell = Cell(4, Board.HIDDEN_ROWS)
}
