package ge.yet.game.fallingblocks.component.game.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import dev.zacsweers.metro.Inject
import ge.yet.game.fallingblocks.data.SessionPersistenceCoordinator
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.TutorialSeenRepository
import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.random.Random

internal fun interface NewGameSeedSource {
    fun nextSeed(): Long
}

internal class DefaultNewGameSeedSource @Inject constructor() : NewGameSeedSource {
    override fun nextSeed(): Long = Random.nextLong()
}

@Inject
internal class FallingBlocksStoreFactory(
    private val storeFactory: StoreFactory,
    private val engine: FallingBlocksEngine,
    private val loader: GameSnapshotLoader,
    private val writer: GameCommitWriter,
    private val tutorial: TutorialSeenRepository,
    private val visibility: MiniAppVisibilitySource,
    private val seedSource: NewGameSeedSource,
) {
    fun create(): FallingBlocksStore =
        object : FallingBlocksStore,
            Store<FallingBlocksStore.Intent, FallingBlocksStore.State, FallingBlocksStore.Label> by
            storeFactory.create(
                name = "FallingBlocksStore",
                initialState = FallingBlocksStore.State(
                    active = visibility.visibility.value == MiniAppVisibility.ACTIVE,
                ),
                bootstrapper = SimpleBootstrapper(Action.Init),
                executorFactory = ::ExecutorImpl,
                reducer = ReducerImpl,
            ) {}

    private sealed interface Action {
        data object Init : Action
    }

    private sealed interface Msg {
        data class Ready(
            val game: FallingBlocksState,
            val tutorialSeen: Boolean,
            val bestScore: Long,
        ) : Msg

        data class GameChanged(val game: FallingBlocksState) : Msg
        data class VisibilityChanged(val active: Boolean) : Msg
        data object TutorialCompleted : Msg
    }

    private object ReducerImpl : Reducer<FallingBlocksStore.State, Msg> {
        override fun FallingBlocksStore.State.reduce(msg: Msg): FallingBlocksStore.State = when (msg) {
            is Msg.Ready -> copy(
                game = msg.game,
                loading = false,
                tutorialSeen = msg.tutorialSeen,
                bestScore = maxOf(bestScore, msg.bestScore, msg.game.score),
            )
            is Msg.GameChanged -> copy(
                game = msg.game,
                bestScore = maxOf(bestScore, msg.game.score),
            )
            is Msg.VisibilityChanged -> copy(active = msg.active)
            Msg.TutorialCompleted -> copy(tutorialSeen = true)
        }
    }

    private inner class ExecutorImpl : CoroutineExecutor<
        FallingBlocksStore.Intent,
        Action,
        FallingBlocksStore.State,
        Msg,
        FallingBlocksStore.Label,
        >() {
        private val persistence by lazy { SessionPersistenceCoordinator(loader, writer, scope) }
        private var previousVisibility = visibility.visibility.value

        override fun executeAction(action: Action) {
            when (action) {
                Action.Init -> initialize()
            }
        }

        override fun executeIntent(intent: FallingBlocksStore.Intent) {
            val current = state()
            val game = current.game ?: return
            if (current.loading) return
            when (intent) {
                is FallingBlocksStore.Intent.Frame -> {
                    if (current.active && game.phase == GamePhase.PLAYING) {
                        applyAction(GameAction.AdvanceTime(intent.elapsedMillis))
                    }
                }
                FallingBlocksStore.Intent.Rotate -> if (acceptsInput(current)) {
                    applyAction(GameAction.RotateClockwise)
                }
                is FallingBlocksStore.Intent.Move -> if (acceptsInput(current)) {
                    applyAction(GameAction.MoveHorizontal(intent.cells))
                }
                is FallingBlocksStore.Intent.SoftDrop -> if (acceptsInput(current)) {
                    applyAction(GameAction.SoftDrop(intent.cells))
                }
                FallingBlocksStore.Intent.HardDrop -> if (acceptsInput(current)) {
                    applyAction(GameAction.HardDrop)
                }
                FallingBlocksStore.Intent.Revive -> if (current.active && game.phase == GamePhase.TERMINAL) {
                    applyAction(GameAction.Revive, forceCheckpoint = true)
                }
                FallingBlocksStore.Intent.NewGame -> if (current.active) {
                    val fresh = engine.initial(seedSource.nextSeed(), game.runId + 1)
                    dispatch(Msg.GameChanged(fresh))
                    persistence.checkpoint(fresh)
                }
                FallingBlocksStore.Intent.TutorialCompleted -> completeTutorial(game)
            }
        }

        private fun initialize() {
            scope.launch {
                visibility.visibility.collect(::onVisibilityChanged)
            }
            scope.launch {
                val restored = try {
                    persistence.load()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    null
                }
                val game = restored?.state ?: engine.initial(seedSource.nextSeed(), runId = 1)
                dispatch(
                    Msg.Ready(
                        game = game,
                        tutorialSeen = restored?.tutorialSeen ?: false,
                        bestScore = restored?.bestScore ?: 0,
                    ),
                )
            }
        }

        private fun onVisibilityChanged(current: MiniAppVisibility) {
            val wasActive = previousVisibility == MiniAppVisibility.ACTIVE
            val isActive = current == MiniAppVisibility.ACTIVE
            previousVisibility = current
            dispatch(Msg.VisibilityChanged(isActive))
            if (wasActive && !isActive) state().game?.let(persistence::checkpoint)
        }

        private fun acceptsInput(current: FallingBlocksStore.State): Boolean =
            current.active && current.game?.phase == GamePhase.PLAYING

        private fun applyAction(action: GameAction, forceCheckpoint: Boolean = false) {
            val before = state().game ?: return
            val transition = engine.reduce(before, action)
            if (transition.state == before && transition.facts.isEmpty()) return
            dispatch(Msg.GameChanged(transition.state))

            val toppedOut = transition.facts.any { it == GameFact.ToppedOut }
            when {
                toppedOut -> scope.launch {
                    persistence.flush(transition.state)
                    publish(FallingBlocksStore.Label.ToppedOut(transition.state.runId))
                }
                forceCheckpoint || transition.facts.any { it == GameFact.Locked || it == GameFact.Revived } ->
                    persistence.checkpoint(transition.state)
            }
        }

        private fun completeTutorial(game: FallingBlocksState) {
            if (state().tutorialSeen) return
            dispatch(Msg.TutorialCompleted)
            scope.launch {
                tutorial.markTutorialSeen()
                persistence.flush(game)
            }
        }
    }
}
