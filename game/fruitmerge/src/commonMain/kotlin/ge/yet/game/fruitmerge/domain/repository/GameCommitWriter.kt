package ge.yet.game.fruitmerge.domain.repository

import ge.yet.game.fruitmerge.domain.model.FruitMergeState

internal interface GameCommitWriter {
    suspend fun checkpoint(state: FruitMergeState)
    suspend fun clearRun()
}
