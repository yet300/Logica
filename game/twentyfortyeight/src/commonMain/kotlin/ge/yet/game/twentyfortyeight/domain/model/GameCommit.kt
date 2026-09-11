package ge.yet.game.twentyfortyeight.domain.model

internal enum class MetadataRecord {
    BestScore,
    Statistics,
    Tutorial,
}

internal data class GameCommit(
    val revision: Long,
    val game: GameState,
    val bestScore: Long,
    val statistics: GameStatistics,
    val tutorialSeen: Boolean,
    val tutorialReason: TutorialCompletionReason?,
    val metadataWrites: Set<MetadataRecord> = MetadataRecord.entries.toSet(),
) {
    init {
        require(revision >= 0L) { "Revision must be non-negative: $revision" }
        require(bestScore >= game.score) { "Best score cannot be below current score" }
        require(tutorialSeen == (tutorialReason != null)) {
            "Tutorial completion and reason must agree"
        }
    }
}
