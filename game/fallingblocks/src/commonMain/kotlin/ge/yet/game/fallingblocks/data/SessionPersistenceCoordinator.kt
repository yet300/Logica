package ge.yet.game.fallingblocks.data

import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.repository.CommitResult
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.RestoredSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SessionPersistenceCoordinator(
    private val loader: GameSnapshotLoader,
    private val writer: GameCommitWriter,
    private val scope: CoroutineScope,
) {
    private data class Request(
        val state: FallingBlocksState,
        val barrier: CompletableDeferred<CommitResult>?,
    )

    private val mutex = Mutex()
    private var writing: Boolean = false
    private var pending: Request? = null

    suspend fun load(): RestoredSession = loader.load()

    fun checkpoint(state: FallingBlocksState) {
        scope.launch { submit(Request(state, barrier = null)) }
    }

    suspend fun flush(state: FallingBlocksState): CommitResult {
        val result = CompletableDeferred<CommitResult>()
        submit(Request(state, barrier = result))
        return result.await()
    }

    private suspend fun submit(request: Request) {
        val ownsWriter = mutex.withLock {
            if (!writing) {
                writing = true
                true
            } else {
                if (pending?.barrier == null || request.barrier != null) pending = request
                false
            }
        }
        if (ownsWriter) drain(request)
    }

    private suspend fun drain(initial: Request) {
        var current: Request? = initial
        try {
            while (current != null) {
                val request = current
                val result = try {
                    writer.write(request.state)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    CommitResult.Failed
                }
                request.barrier?.complete(result)
                current = mutex.withLock {
                    pending.also { next ->
                        pending = null
                        if (next == null) writing = false
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            mutex.withLock {
                current?.barrier?.cancel(cancellation)
                pending?.barrier?.cancel(cancellation)
                pending = null
                writing = false
            }
            throw cancellation
        }
    }
}
