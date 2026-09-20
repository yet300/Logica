package ge.yet.game.fallingblocks.domain.repository

import ge.yet.game.fallingblocks.domain.model.FallingBlocksState

internal data class RestoredSession(
    val state: FallingBlocksState?,
    val bestScore: Long,
    val tutorialSeen: Boolean,
)

internal interface GameSnapshotLoader {
    suspend fun load(): RestoredSession
}
