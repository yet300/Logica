package ge.yet.game.twentyfortyeight.component.playing.store

import ge.yet.game.twentyfortyeight.domain.engine.GameRules
import ge.yet.game.twentyfortyeight.domain.engine.RngState
import ge.yet.game.twentyfortyeight.domain.model.GamePhase
import ge.yet.game.twentyfortyeight.domain.model.GameState
import ge.yet.game.twentyfortyeight.domain.model.RestoredGameData
import ge.yet.game.twentyfortyeight.domain.model.RulesState

// Pure bootstrap decisions: no dispatch, publish, scope or RNG consumption.
// The executor supplies the seed and owns side effects (diagnostics, labels).
internal data class FreshRules(
    val rules: RulesState,
    val counterOverflow: Boolean,
)

internal object PlayingBootstrapPlanner {
    fun resolveRestoredOverlay(game: GameState): OverlayState? =
        if (
            game.phase == GamePhase.Playing &&
            game.facts.victoryReached &&
            !game.facts.victoryAcknowledged
        ) {
            OverlayState.Victory
        } else {
            null
        }

    fun freshRules(data: RestoredGameData, seed: RngState): FreshRules {
        val fresh = GameRules.newGame(previous = null, seed = seed)
        val counterOverflow = data.statistics.gamesStarted == Long.MAX_VALUE
        val gamesStarted = if (counterOverflow) {
            Long.MAX_VALUE
        } else {
            data.statistics.gamesStarted + 1L
        }
        val highest = fresh.game.board.values().filterNotNull().maxOrNull() ?: 0L
        return FreshRules(
            rules = RulesState(
                game = fresh.game.copy(
                    runOrdinal = maxOf(1L, gamesStarted),
                    bestScore = maxOf(data.bestScore, fresh.game.score),
                ),
                statistics = data.statistics.copy(
                    gamesStarted = gamesStarted,
                    highestTileEver = maxOf(data.statistics.highestTileEver, highest),
                ),
            ),
            counterOverflow = counterOverflow,
        )
    }
}
