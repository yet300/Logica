package ge.yet.game.fruitmerge.domain.repository

internal interface TutorialSeenRepository {
    suspend fun isTutorialSeen(): Boolean
    suspend fun markTutorialSeen()
}
