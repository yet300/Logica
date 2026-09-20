package ge.yet.game.fallingblocks.domain.repository

import ge.yet.game.fallingblocks.domain.model.FallingBlocksState

internal sealed interface CommitResult {
    data object Stored : CommitResult
    data object Failed : CommitResult
}

internal interface GameCommitWriter {
    suspend fun write(state: FallingBlocksState): CommitResult
}
