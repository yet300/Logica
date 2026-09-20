package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.engine.LOCK_DELAY_MILLIS
import ge.yet.game.fallingblocks.domain.engine.MAX_LOCK_RESETS
import ge.yet.game.fallingblocks.domain.engine.cells
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FallingBlocksPropertyTest {
    private val engine = DefaultFallingBlocksEngine

    @Test
    fun `generated action sequences preserve invariants and replay exactly`() {
        repeat(SEED_COUNT) { seed ->
            val actions = generatedActions(seed.toLong(), ACTION_COUNT)
            val first = replay(seed.toLong(), actions)
            val second = replay(seed.toLong(), actions)

            assertEquals(first, second, "seed=$seed")
        }
    }

    private fun replay(seed: Long, actions: List<GameAction>) =
        actions.fold(engine.initial(seed = seed, runId = seed)) { state, action ->
            val next = engine.reduce(state, action).state
            assertEquals(Board.WIDTH * Board.TOTAL_HEIGHT, next.board.cells.size)
            assertEquals(5, next.preview.size)
            assertEquals(next.bag.remaining.size, next.bag.remaining.toSet().size)
            assertTrue(next.bag.remaining.all(Tetromino.entries::contains))
            assertTrue(next.score >= 0)
            assertTrue(next.lines >= 0)
            assertTrue(next.gravityRemainingMillis >= 0)
            assertTrue(next.lockRemainingMillis in 0..LOCK_DELAY_MILLIS)
            assertTrue(next.lockResetCount in 0..MAX_LOCK_RESETS)
            if (next.phase == GamePhase.PLAYING) {
                assertTrue(next.active.cells().all { cell ->
                    cell.x in 0 until Board.WIDTH &&
                        cell.y in 0 until Board.TOTAL_HEIGHT &&
                        next.board[cell] == null
                })
            }
            next
        }

    private fun generatedActions(seed: Long, count: Int): List<GameAction> {
        var bits = seed
        return List(count) {
            bits = bits * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
            when (((bits ushr 1) % 6).toInt()) {
                0 -> GameAction.RotateClockwise
                1 -> GameAction.MoveHorizontal((((bits ushr 8) % 9) - 4).toInt())
                2 -> GameAction.SoftDrop(((bits ushr 12) % 6).toInt())
                3 -> GameAction.HardDrop
                4 -> GameAction.AdvanceTime(((bits ushr 16) % 1_001).toInt())
                else -> GameAction.Revive
            }
        }
    }

    private companion object {
        const val SEED_COUNT: Int = 256
        const val ACTION_COUNT: Int = 2_000
    }
}
