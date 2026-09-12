package ge.yet.game.uikit.components.score

/**
 * Shared score/best display policy, extracted from the duplicated
 * 2048 `ScoreBestRow` and Fruit Merge `FruitMergeUiPolicy` implementations.
 */
enum class ScoreCardState {
    ScoreOnly,
    ScoreAndBest,
    BestOnly,
}

fun scoreCardState(
    bestScore: Long,
    bestImprovedInRun: Boolean,
): ScoreCardState = when {
    bestImprovedInRun -> ScoreCardState.BestOnly
    bestScore > 0L -> ScoreCardState.ScoreAndBest
    else -> ScoreCardState.ScoreOnly
}
