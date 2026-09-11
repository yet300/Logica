package ge.yet.game.fruitmerge.component.result

import ge.yet.game.fruitmerge.domain.model.FruitBody
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import kotlinx.serialization.Serializable

@Serializable
internal data class FruitMergeResultSnapshot(
    val score: Long,
    val bestScore: Long,
    val bestImprovedInRun: Boolean,
    val runOrdinal: Long,
    val bodies: List<FruitBody>,
    val dangerSeconds: Float,
) {
    companion object {
        fun from(state: FruitMergeState): FruitMergeResultSnapshot = FruitMergeResultSnapshot(
            score = state.score,
            bestScore = state.bestScore,
            bestImprovedInRun = state.bestImprovedInRun,
            runOrdinal = state.runOrdinal,
            bodies = state.bodies.toList(),
            dangerSeconds = state.dangerSeconds,
        )
    }
}
