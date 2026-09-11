package ge.yet.game.twentyfortyeight.data

import ge.yet.game.twentyfortyeight.domain.model.GameCommit
import ge.yet.game.twentyfortyeight.domain.repository.GameCommitWriter
import ge.yet.game.twentyfortyeight.domain.repository.GameSnapshotLoader
import ge.yet.game.twentyfortyeight.domain.model.LoadResult
import ge.yet.game.twentyfortyeight.domain.model.MetadataRecord
import ge.yet.game.twentyfortyeight.domain.model.RestoredGameData

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.miniapp.api.MiniAppSnapshotSpec
import ge.yet.game.miniapp.api.MiniAppStorage
import ge.yet.game.twentyfortyeight.diagnostics.StorageOperation
import ge.yet.game.twentyfortyeight.diagnostics.TwentyFortyEightFailure
import ge.yet.game.twentyfortyeight.domain.model.GamePhase
import ge.yet.game.twentyfortyeight.domain.model.GameState
import ge.yet.game.twentyfortyeight.domain.model.GameStatistics
import ge.yet.game.twentyfortyeight.domain.model.TutorialCompletionReason
import kotlinx.coroutines.CancellationException

internal class PersistenceWriteException(
    val failure: TwentyFortyEightFailure.StorageWrite,
) : RuntimeException(failure.toString())

@Inject
@SingleIn(AppScope::class)
internal class TwentyFortyEightPersistence : GameCommitWriter, GameSnapshotLoader {
    override suspend fun load(storage: MiniAppStorage): LoadResult = try {
        val validationFailures = linkedSetOf<TwentyFortyEightFailure>()
        val currentPayload = read(
            storage,
            TwentyFortyEightSchemas.CurrentGameKey,
            TwentyFortyEightSchemas.currentGame,
            StorageOperation.CurrentGameRead,
        )
        val bestPayload = read(
            storage,
            TwentyFortyEightSchemas.BestScoreKey,
            TwentyFortyEightSchemas.bestScore,
            StorageOperation.BestScoreRead,
        )
        val statisticsPayload = read(
            storage,
            TwentyFortyEightSchemas.StatisticsKey,
            TwentyFortyEightSchemas.statistics,
            StorageOperation.StatisticsRead,
        )
        val tutorialPayload = read(
            storage,
            TwentyFortyEightSchemas.TutorialKey,
            TwentyFortyEightSchemas.tutorial,
            StorageOperation.TutorialRead,
        )

        val validCurrent = currentPayload?.let { payload ->
            TwentyFortyEightSchemas.toDomain(payload)
                .recoverValidationFailure(validationFailures)
                ?.let { payload to it }
        }
        val game = validCurrent?.second
        val mirroredBest = validCurrent?.first?.bestMirror?.bestScore ?: 0L
        val validBest = bestPayload?.let { payload ->
            TwentyFortyEightSchemas.toBestScore(payload)
                .recoverValidationFailure(validationFailures)
                ?.let { payload to it }
        }
        val dedicatedBest = validBest?.second ?: 0L
        val bestScore = maxOf(game?.bestScore ?: 0L, mirroredBest, dedicatedBest)

        val mirroredStatistics = validCurrent?.first?.statisticsMirror?.let { payload ->
            TwentyFortyEightSchemas.toStatistics(payload).getOrThrow()
        } ?: GameStatistics()
        val validStatistics = statisticsPayload?.let { payload ->
            TwentyFortyEightSchemas.toStatistics(payload)
                .recoverValidationFailure(validationFailures)
                ?.let { payload to it }
        }
        val dedicatedStatistics = validStatistics?.second ?: GameStatistics()
        val statistics = reconcileStatistics(mirroredStatistics, dedicatedStatistics)

        val mirroredTutorial = validCurrent?.first?.tutorialMirror?.let { payload ->
            payload to TwentyFortyEightSchemas.toTutorial(payload).getOrThrow()
        }
        val validTutorial = tutorialPayload?.let { payload ->
            TwentyFortyEightSchemas.toTutorial(payload)
                .recoverValidationFailure(validationFailures)
                ?.let { payload to it }
        }
        val tutorial = reconcileTutorial(mirroredTutorial, validTutorial)
        val revision = listOfNotNull(
            validCurrent?.first?.revision,
            validBest?.first?.revision,
            validStatistics?.first?.revision,
            validTutorial?.first?.revision,
        ).maxOrNull() ?: 0L
        val reconciledGame = game?.copy(bestScore = maxOf(game.score, bestScore))

        LoadResult.Loaded(
            RestoredGameData(
                revision = revision,
                game = reconciledGame,
                bestScore = bestScore,
                statistics = statistics,
                tutorialSeen = tutorial.seen,
                tutorialReason = tutorial.reason,
                terminal = reconciledGame?.phase == GamePhase.GameOver,
            ),
            validationFailures = validationFailures.toSet(),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: PersistenceReadException) {
        LoadResult.Failed(failure.failure)
    }

    private fun <T> Result<T>.recoverValidationFailure(
        failures: MutableSet<TwentyFortyEightFailure>,
    ): T? = fold(
        onSuccess = { it },
        onFailure = { throwable ->
            val validation = throwable as? SnapshotValidationException ?: throw throwable
            failures += validation.failure
            null
        },
    )

    override suspend fun commit(storage: MiniAppStorage, commit: GameCommit) {
        val current = commit.toCurrentGameV1()
        write(
            storage,
            TwentyFortyEightSchemas.CurrentGameKey,
            current,
            TwentyFortyEightSchemas.currentGame,
            StorageOperation.CurrentGameWrite,
        )
        if (MetadataRecord.BestScore in commit.metadataWrites) {
            write(
                storage,
                TwentyFortyEightSchemas.BestScoreKey,
                current.bestMirror,
                TwentyFortyEightSchemas.bestScore,
                StorageOperation.BestScoreWrite,
            )
        }
        if (MetadataRecord.Statistics in commit.metadataWrites) {
            write(
                storage,
                TwentyFortyEightSchemas.StatisticsKey,
                current.statisticsMirror,
                TwentyFortyEightSchemas.statistics,
                StorageOperation.StatisticsWrite,
            )
        }
        if (MetadataRecord.Tutorial in commit.metadataWrites) {
            write(
                storage,
                TwentyFortyEightSchemas.TutorialKey,
                current.tutorialMirror,
                TwentyFortyEightSchemas.tutorial,
                StorageOperation.TutorialWrite,
            )
        }
    }

    private suspend fun <T> read(
        storage: MiniAppStorage,
        localName: String,
        spec: MiniAppSnapshotSpec<T>,
        operation: StorageOperation,
    ): T? = try {
        storage.readSnapshot(localName, spec)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        throw PersistenceReadException(TwentyFortyEightFailure.StorageRead(operation))
    }

    private suspend fun <T> write(
        storage: MiniAppStorage,
        localName: String,
        value: T,
        spec: MiniAppSnapshotSpec<T>,
        operation: StorageOperation,
    ) {
        try {
            storage.writeSnapshot(localName, value, spec)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            throw PersistenceWriteException(TwentyFortyEightFailure.StorageWrite(operation))
        }
    }
}

private class PersistenceReadException(
    val failure: TwentyFortyEightFailure.StorageRead,
) : RuntimeException(failure.toString())

private fun GameCommit.toCurrentGameV1(): CurrentGameV1 = CurrentGameV1(
    revision = revision,
    runOrdinal = game.runOrdinal,
    phase = game.phase.toPersistedName(),
    board = game.board.values(),
    score = game.score,
    rngAlgorithm = game.rng.algorithm,
    rngStateHex = game.rng.stateHex,
    undo = game.undo?.let { snapshot ->
        UndoV1(
            board = snapshot.board.values,
            score = snapshot.score,
            rngAlgorithm = snapshot.rng.algorithm,
            rngStateHex = snapshot.rng.stateHex,
            victoryAcknowledged = snapshot.victoryAcknowledged,
            phase = snapshot.phase.toPersistedName(),
        )
    },
    victoryReached = game.facts.victoryReached,
    victoryAcknowledged = game.facts.victoryAcknowledged,
    gamesWonRecorded = game.facts.gamesWonRecorded,
    reviewReserved = game.facts.reviewReserved,
    bestImprovedInRun = game.facts.bestImprovedInRun,
    analyticsReservations = game.facts.analyticsReservations,
    milestoneReservations = game.facts.milestoneReservations,
    momentumStreak = game.momentumStreak,
    successfulMovesInRun = game.successfulMovesInRun,
    bestMirror = BestScoreV1(revision, bestScore),
    statisticsMirror = statistics.toV1(revision),
    tutorialMirror = TutorialV1(
        revision = revision,
        seen = tutorialSeen,
        reason = tutorialReason?.name?.uppercase(),
    ),
)

private fun GameStatistics.toV1(revision: Long): StatisticsV1 = StatisticsV1(
    revision = revision,
    gamesStarted = gamesStarted,
    gamesWon = gamesWon,
    gamesEndedByGameOver = gamesEndedByGameOver,
    successfulMoves = successfulMoves,
    totalMerges = totalMerges,
    totalScoreEarned = totalScoreEarned,
    highestTileEver = highestTileEver,
    undoUses = undoUses,
)

private fun reconcileStatistics(
    first: GameStatistics,
    second: GameStatistics,
): GameStatistics = GameStatistics(
    gamesStarted = maxOf(first.gamesStarted, second.gamesStarted),
    gamesWon = maxOf(first.gamesWon, second.gamesWon),
    gamesEndedByGameOver = maxOf(first.gamesEndedByGameOver, second.gamesEndedByGameOver),
    successfulMoves = maxOf(first.successfulMoves, second.successfulMoves),
    totalMerges = maxOf(first.totalMerges, second.totalMerges),
    totalScoreEarned = maxOf(first.totalScoreEarned, second.totalScoreEarned),
    highestTileEver = maxOf(first.highestTileEver, second.highestTileEver),
    undoUses = maxOf(first.undoUses, second.undoUses),
)

private fun reconcileTutorial(
    first: Pair<TutorialV1, TutorialState>?,
    second: Pair<TutorialV1, TutorialState>?,
): TutorialState {
    val candidates = listOfNotNull(first, second)
    val seen = candidates.any { it.second.seen }
    val reason = candidates
        .filter { it.second.reason != null }
        .maxByOrNull { it.first.revision }
        ?.second
        ?.reason
    return TutorialState(seen = seen, reason = reason)
}

private fun GamePhase.toPersistedName(): String = when (this) {
    GamePhase.Playing -> "PLAYING"
    GamePhase.GameOver -> "GAME_OVER"
}
