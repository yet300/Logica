package ge.yet.game.feature.root

import com.app.common.decompose.LifecycleCoroutineScopeRegistry
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

internal class MiniAppSessionLifecycle private constructor(
    private val parent: Lifecycle,
    private val registry: LifecycleRegistry,
) : Lifecycle by registry,
    Lifecycle.Callbacks,
    LifecycleCoroutineScopeRegistry {
    constructor(parent: Lifecycle) : this(parent, LifecycleRegistry())

    private data class ScopeState(
        val closed: Boolean = false,
        val jobs: Set<Job> = emptySet(),
    )

    private val scopeState = MutableStateFlow(ScopeState())
    private val teardown = CompletableDeferred<Unit>()

    init {
        parent.subscribe(this)
    }

    override fun register(job: Job) {
        while (true) {
            val current = scopeState.value
            if (current.closed) {
                job.cancel()
                return
            }
            if (scopeState.compareAndSet(current, current.copy(jobs = current.jobs + job))) break
        }
        job.invokeOnCompletion { remove(job) }
    }

    fun close() {
        while (true) {
            val current = scopeState.value
            if (current.closed) return
            if (scopeState.compareAndSet(current, current.copy(closed = true))) {
                parent.unsubscribe(this)
                registry.create()
                registry.destroy()
                current.jobs.forEach(Job::cancel)
                completeIfFinished(scopeState.value)
                return
            }
        }
    }

    suspend fun awaitTeardown() = teardown.await()

    override fun onCreate() = registry.onCreate()
    override fun onStart() = registry.onStart()
    override fun onResume() = registry.onResume()
    override fun onPause() = registry.onPause()
    override fun onStop() = registry.onStop()
    override fun onDestroy() = close()

    private fun remove(job: Job) {
        while (true) {
            val current = scopeState.value
            val updated = current.copy(jobs = current.jobs - job)
            if (scopeState.compareAndSet(current, updated)) {
                completeIfFinished(updated)
                return
            }
        }
    }

    private fun completeIfFinished(state: ScopeState) {
        if (state.closed && state.jobs.isEmpty()) teardown.complete(Unit)
    }
}
