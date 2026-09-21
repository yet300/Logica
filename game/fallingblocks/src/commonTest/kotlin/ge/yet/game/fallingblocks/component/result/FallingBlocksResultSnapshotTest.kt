package ge.yet.game.fallingblocks.component.result

import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.gameFixture
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FallingBlocksResultSnapshotTest {
    @Test
    fun `terminal state survives a detached serialization round trip`() {
        val state = gameFixture(
            active = ge.yet.game.fallingblocks.domain.model.ActivePiece(
                Tetromino.L,
                Rotation.RIGHT,
                Cell(3, 2),
            ),
            score = 1_234L,
            revivesUsed = 1,
        )
        val snapshot = FallingBlocksResultSnapshot.from(state, bestScore = 4_321L)

        val restored = Json.decodeFromString<FallingBlocksResultSnapshot>(
            Json.encodeToString(snapshot),
        )

        assertEquals(snapshot, restored)
        assertEquals(state.board, restored.board())
        assertEquals(state.active, restored.activePiece())
        assertEquals(1_234L, restored.score)
        assertEquals(4_321L, restored.bestScore)
        assertEquals(state.runId, restored.runId)
        assertEquals(1, restored.revivesUsed)
    }

    @Test
    fun `snapshot rejects malformed cells and enum ordinals`() {
        assertFailsWith<IllegalArgumentException> {
            FallingBlocksResultSnapshot(
                boardCells = listOf(null),
                activeTypeOrdinal = Tetromino.T.ordinal,
                activeRotationOrdinal = Rotation.SPAWN.ordinal,
                activeX = 3,
                activeY = 2,
                score = 0,
                bestScore = 0,
                runId = 1,
                revivesUsed = 0,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            val state = gameFixture()
            FallingBlocksResultSnapshot.from(state, bestScore = 0).copy(activeTypeOrdinal = -1)
        }
    }
}
