package ge.yet.game.fallingblocks.persistence

import ge.yet.game.fallingblocks.data.FallingBlocksSchemas
import ge.yet.game.fallingblocks.data.GameSnapshotV1
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.boardWith
import ge.yet.game.fallingblocks.gameFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FallingBlocksSchemasTest {
    @Test
    fun `snapshot round trip preserves every authoritative field`() {
        val state = gameFixture(
            board = boardWith(Cell(0, 21) to Tetromino.J, Cell(9, 20) to Tetromino.Z),
            score = 99_123,
            level = 3,
            lines = 27,
            combo = 2,
            backToBack = true,
            gravityRemainingMillis = 71,
            lockRemainingMillis = 341,
            lockResetCount = 8,
            lastActionWasRotation = true,
            revivesUsed = 1,
        )

        val restored = FallingBlocksSchemas.toDomain(GameSnapshotV1.from(state)).getOrThrow()

        assertEquals(state, restored)
    }

    @Test
    fun `schema rejects corrupt version dimensions queue bag counters and overlap`() {
        val state = gameFixture()
        val valid = GameSnapshotV1.from(state)
        val corrupt = listOf(
            valid.copy(version = 2),
            valid.copy(cells = valid.cells.dropLast(1)),
            valid.copy(preview = valid.preview.dropLast(1)),
            valid.copy(bag = listOf(0, 0)),
            valid.copy(score = -1),
            valid.copy(lockRemainingMillis = 501),
            valid.copy(revivesUsed = 2),
            valid.copy(cells = valid.cells.toMutableList().also { cells ->
                cells[valid.activeY * 10 + valid.activeX] = valid.activeType
            }),
        )

        corrupt.forEach { payload ->
            assertTrue(FallingBlocksSchemas.toDomain(payload).isFailure, payload.toString())
        }
    }
}
