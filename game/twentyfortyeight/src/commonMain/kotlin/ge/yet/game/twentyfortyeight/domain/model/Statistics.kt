package ge.yet.game.twentyfortyeight.domain.model

import kotlinx.serialization.Serializable

@Serializable
internal data class GameStatistics(
    val gamesStarted: Long = 0L,
    val gamesWon: Long = 0L,
    val gamesEndedByGameOver: Long = 0L,
    val successfulMoves: Long = 0L,
    val totalMerges: Long = 0L,
    val totalScoreEarned: Long = 0L,
    val highestTileEver: Long = 0L,
    val undoUses: Long = 0L,
) {
    init {
        require(
            listOf(
                gamesStarted,
                gamesWon,
                gamesEndedByGameOver,
                successfulMoves,
                totalMerges,
                totalScoreEarned,
                highestTileEver,
                undoUses,
            ).all { it >= 0L },
        ) { "2048 statistics cannot contain negative values" }
        if (highestTileEver != 0L) TileValue(highestTileEver)
    }
}

@Serializable
internal data class ResultSnapshot(
    val score: Long,
    val bestScore: Long,
    val highestTile: Long,
    val statistics: GameStatistics,
)

internal enum class TutorialCompletionReason {
    Move,
    Skip,
}
