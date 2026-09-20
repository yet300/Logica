package ge.yet.game.fallingblocks.persistence

import ge.yet.game.fallingblocks.data.SessionPersistenceCoordinator
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.repository.CommitResult
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.RestoredSession
import ge.yet.game.fallingblocks.gameFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionPersistenceCoordinatorTest {
    @Test
    fun `one write runs and pending checkpoint is latest wins`() = runTest {
        val writer = SuspendedWriter()
        val coordinator = SessionPersistenceCoordinator(EmptyLoader, writer, backgroundScope)

        coordinator.checkpoint(gameFixture(score = 1))
        assertEquals(1, writer.started.receive())
        coordinator.checkpoint(gameFixture(score = 2))
        coordinator.checkpoint(gameFixture(score = 3))
        writer.release.send(Unit)
        assertEquals(3, writer.started.receive())
        writer.release.send(Unit)
        writer.finished.await()

        assertEquals(listOf(1L, 3L), writer.writes)
    }

    @Test
    fun `flush writes an exact barrier and returns its result`() = runTest {
        val writer = ImmediateWriter()
        val coordinator = SessionPersistenceCoordinator(EmptyLoader, writer, backgroundScope)
        val state = gameFixture(score = 42)

        val result = coordinator.flush(state)

        assertIs<CommitResult.Stored>(result)
        assertEquals(listOf(state), writer.writes)
    }
}

private object EmptyLoader : GameSnapshotLoader {
    override suspend fun load(): RestoredSession = RestoredSession(null, 0, false)
}

private class ImmediateWriter : GameCommitWriter {
    val writes = mutableListOf<FallingBlocksState>()
    override suspend fun write(state: FallingBlocksState): CommitResult {
        writes += state
        return CommitResult.Stored
    }
}

private class SuspendedWriter : GameCommitWriter {
    val started = Channel<Long>(Channel.UNLIMITED)
    val release = Channel<Unit>(Channel.UNLIMITED)
    val writes = mutableListOf<Long>()
    val finished = CompletableDeferred<Unit>()

    override suspend fun write(state: FallingBlocksState): CommitResult {
        started.send(state.score)
        release.receive()
        writes += state.score
        if (writes.size == 2) finished.complete(Unit)
        return CommitResult.Stored
    }
}
