package ge.yet.game.fruitmerge.domain.repository

import ge.yet.game.fruitmerge.domain.model.FruitMergeState

internal interface GameSnapshotLoader {
    suspend fun restore(): FruitMergeState
}
