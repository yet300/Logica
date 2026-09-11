package ge.yet.game.fruitmerge.component.result

import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import kotlinx.serialization.Serializable

@Serializable
internal data class FruitMergeResultSnapshot(
    val score: Long,
    val bestScore: Long,
    val bestImprovedInRun: Boolean,
    val largestFruit: FruitLevel,
    val runOrdinal: Long,
) {
    companion object {
        fun from(state: FruitMergeState): FruitMergeResultSnapshot = FruitMergeResultSnapshot(
            score = state.score,
            bestScore = state.bestScore,
            bestImprovedInRun = state.bestImprovedInRun,
            largestFruit = state.bodies.maxByOrNull { body -> body.level.ordinal }?.level
                ?: state.previewLevel,
            runOrdinal = state.runOrdinal,
        )
    }
}
