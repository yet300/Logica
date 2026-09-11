package ge.yet.game.twentyfortyeight.domain.model

import ge.yet.game.twentyfortyeight.diagnostics.TwentyFortyEightFailure

internal data class RestoredGameData(
    val revision: Long,
    val game: GameState?,
    val bestScore: Long,
    val statistics: GameStatistics,
    val tutorialSeen: Boolean,
    val tutorialReason: TutorialCompletionReason?,
    val terminal: Boolean,
)

internal sealed interface LoadResult {
    data class Loaded(
        val data: RestoredGameData,
        val validationFailures: Set<TwentyFortyEightFailure>,
    ) : LoadResult

    data class Failed(val failure: TwentyFortyEightFailure) : LoadResult
}
