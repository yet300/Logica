package ge.yet.game.fallingblocks.data

import dev.zacsweers.metro.Inject
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.repository.CommitResult
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.RestoredSession
import ge.yet.game.fallingblocks.domain.repository.TutorialSeenRepository
import ge.yet.game.miniapp.api.MiniAppStorage
import kotlinx.coroutines.CancellationException
import kotlin.math.max

internal const val SNAPSHOT_KEY: String = "game_snapshot"
internal const val BEST_SCORE_KEY: String = "best_score"
internal const val TUTORIAL_SEEN_KEY: String = "tutorial_seen"

internal class FallingBlocksPersistence @Inject constructor(
    private val storage: MiniAppStorage,
) : GameSnapshotLoader, GameCommitWriter, TutorialSeenRepository {
    override suspend fun load(): RestoredSession {
        val bestScore = readBestScore()
        val tutorialSeen = isTutorialSeen()
        val state = try {
            storage.readSnapshot(SNAPSHOT_KEY, FallingBlocksSnapshotSpec)
                ?.let(FallingBlocksSchemas::toDomain)
                ?.getOrNull()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
        return RestoredSession(state, bestScore, tutorialSeen)
    }

    override suspend fun write(state: FallingBlocksState): CommitResult = try {
        storage.writeSnapshot(SNAPSHOT_KEY, GameSnapshotV1.from(state), FallingBlocksSnapshotSpec)
        storage.putLong(BEST_SCORE_KEY, max(readBestScore(), state.score))
        CommitResult.Stored
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        CommitResult.Failed
    }

    override suspend fun isTutorialSeen(): Boolean = try {
        storage.getBoolean(TUTORIAL_SEEN_KEY, false)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false
    }

    override suspend fun markTutorialSeen() {
        try {
            storage.putBoolean(TUTORIAL_SEEN_KEY, true)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Tutorial persistence is best-effort; the running session remains usable.
        }
    }

    private suspend fun readBestScore(): Long = try {
        storage.getLong(BEST_SCORE_KEY, 0).coerceAtLeast(0)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        0
    }
}
