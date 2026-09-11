package ge.yet.game.fruitmerge.data

import dev.zacsweers.metro.Inject
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.repository.GameCommitWriter
import ge.yet.game.fruitmerge.domain.repository.GameSnapshotLoader
import ge.yet.game.fruitmerge.domain.repository.TutorialSeenRepository
import ge.yet.game.miniapp.api.MiniAppSnapshotMigration
import ge.yet.game.miniapp.api.MiniAppSnapshotSpec
import ge.yet.game.miniapp.api.MiniAppStorage
import kotlinx.coroutines.CancellationException
import kotlin.math.max

internal const val SNAPSHOT_KEY: String = "game_snapshot"
internal const val BEST_SCORE_KEY: String = "best_score"
internal const val TUTORIAL_SEEN_KEY: String = "tutorial_seen"

internal val FruitMergeSnapshotSpec = MiniAppSnapshotSpec(
    serializer = FruitMergeSnapshot.serializer(),
    currentVersion = 2,
    migrations = mapOf(1 to MiniAppSnapshotMigration { payload -> payload }),
)

internal class FruitMergePersistence @Inject constructor(
    private val storage: MiniAppStorage,
) : GameSnapshotLoader, GameCommitWriter, TutorialSeenRepository {
    override suspend fun restore(): FruitMergeState {
        val bestScore = try {
            storage.getLong(BEST_SCORE_KEY, 0L).coerceAtLeast(0L)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            0L
        }
        return try {
            storage.readSnapshot(SNAPSHOT_KEY, FruitMergeSnapshotSpec)?.toState(bestScore)
                ?: FruitMergeState(bestScore = bestScore)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            FruitMergeState(bestScore = bestScore)
        }
    }

    override suspend fun checkpoint(state: FruitMergeState) {
        storage.putLong(BEST_SCORE_KEY, max(state.bestScore, state.score))
        storage.writeSnapshot(SNAPSHOT_KEY, FruitMergeSnapshot.from(state), FruitMergeSnapshotSpec)
    }

    override suspend fun clearRun() {
        storage.remove(SNAPSHOT_KEY)
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
            // Tutorial progress is best-effort and must never block the game session.
        }
    }
}
