package ge.yet.game.fallingblocks.domain.repository

internal interface TutorialSeenRepository {
    suspend fun isTutorialSeen(): Boolean

    suspend fun markTutorialSeen()
}
