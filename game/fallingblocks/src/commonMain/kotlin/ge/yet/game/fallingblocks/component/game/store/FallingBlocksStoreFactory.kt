package ge.yet.game.fallingblocks.component.game.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import dev.zacsweers.metro.Inject
import ge.yet.game.fallingblocks.audio.FallingBlocksAudioPlayer
import ge.yet.game.fallingblocks.component.game.FallingBlocksTransitionPlanner
import ge.yet.game.fallingblocks.component.game.FallingBlocksVisualEvent
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
import ge.yet.game.fallingblocks.ui.tutorial.TutorialProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

internal fun interface NewGameSeedSource {
    fun nextSeed(): Long
}

internal const val TUTORIAL_SEED: Long = 0x46414C4C494E47L

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
    private val audio: FallingBlocksAudioPlayer,
) {
    fun create(startFresh: Boolean = false): FallingBlocksStore =
        object : FallingBlocksStore,
            Store<FallingBlocksStore.Intent, FallingBlocksStore.State, FallingBlocksStore.Label> by
            storeFactory.create(
                name = "FallingBlocksStore",
                initialState = FallingBlocksStore.State(
                    active = visibility.visibility.value == MiniAppVisibility.ACTIVE,
                ),
                bootstrapper = SimpleBootstrapper(Action.Init),
                executorFactory = { ExecutorImpl(startFresh) },
                reducer = ReducerImpl,
            ) {}

    private sealed interface Action {
        data object Init : Action
    }

    private sealed interface Msg {
        data class Ready(
            val game: FallingBlocksState,
            val tutorialSeen: Boolean,
            val tutorialProgress: TutorialProgress?,
            val bestScore: Long,
        ) : Msg

        data class GameChanged(
            val game: FallingBlocksState,
            val visualEvent: FallingBlocksVisualEvent?,
            val nextVisualEventId: Long,
        ) : Msg
        data class TutorialProgressChanged(val progress: TutorialProgress) : Msg
        data class TutorialFinished(val game: FallingBlocksState) : Msg
        data class VisibilityChanged(val active: Boolean) : Msg
    }

    private object ReducerImpl : Reducer<FallingBlocksStore.State, Msg> {
        override fun FallingBlocksStore.State.reduce(msg: Msg): FallingBlocksStore.State = when (msg) {
            is Msg.Ready -> copy(
                game = msg.game,
                loading = false,
                tutorialSeen = msg.tutorialSeen,
                tutorialProgress = msg.tutorialProgress,
                bestScore = maxOf(bestScore, msg.bestScore, msg.game.score),
            )
            is Msg.GameChanged -> copy(
                game = msg.game,
                bestScore = maxOf(bestScore, msg.game.score),
                visualEvent = msg.visualEvent,
                nextVisualEventId = msg.nextVisualEventId,
            )
            is Msg.TutorialProgressChanged -> copy(tutorialProgress = msg.progress)
            is Msg.TutorialFinished -> copy(
                game = msg.game,
                tutorialSeen = true,
                tutorialProgress = null,
                bestScore = maxOf(bestScore, msg.game.score),
                visualEvent = null,
            )
            is Msg.VisibilityChanged -> copy(active = msg.active)
        }
    }

    private inner class ExecutorImpl(
        private val startFresh: Boolean,
    ) : CoroutineExecutor<
        FallingBlocksStore.Intent,
        Action,
        FallingBlocksStore.State,
        Msg,
        FallingBlocksStore.Label,
        >() {
        private val persistence by lazy { SessionPersistenceCoordinator(loader, writer, scope) }
        private val transitionPlanner = FallingBlocksTransitionPlanner()
        private var previousVisibility = visibility.visibility.value
        private var tutorialCompletionInFlight = false

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
                    if (current.tutorialSeen && current.active && game.phase == GamePhase.PLAYING) {
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
                FallingBlocksStore.Intent.Revive -> if (
                    current.tutorialSeen && current.active && game.phase == GamePhase.TERMINAL
                ) {
                    applyAction(GameAction.Revive, forceCheckpoint = true)
                }
                FallingBlocksStore.Intent.NewGame -> if (current.tutorialSeen && current.active) {
                    val fresh = engine.initial(seedSource.nextSeed(), game.runId + 1)
                    dispatch(
                        Msg.GameChanged(
                            game = fresh,
                            visualEvent = null,
                            nextVisualEventId = current.nextVisualEventId,
                        ),
                    )
                    persistence.checkpoint(fresh)
                }
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
                val tutorialSeen = restored?.tutorialSeen ?: false
                val game = if (tutorialSeen) {
                    if (startFresh) {
                        engine.initial(
                            seed = seedSource.nextSeed(),
                            runId = (restored.state?.runId ?: 0L) + 1L,
                        )
                    } else {
                        restored.state ?: engine.initial(seedSource.nextSeed(), runId = 1)
                    }
                } else {
                    engine.initial(TUTORIAL_SEED, runId = 1)
                }
                dispatch(
                    Msg.Ready(
                        game = game,
                        tutorialSeen = tutorialSeen,
                        tutorialProgress = if (tutorialSeen) null else TutorialProgress.initial(),
                        bestScore = restored?.bestScore ?: 0,
                    ),
                )
                audio.start(game)
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
            current.active &&
                current.game?.phase == GamePhase.PLAYING &&
                current.tutorialProgress?.complete != true

        private fun applyAction(action: GameAction, forceCheckpoint: Boolean = false) {
            val current = state()
            val before = current.game ?: return
            val transition = engine.reduce(before, action)
            if (transition.state == before && transition.facts.isEmpty()) return
            val visualEvent = transitionPlanner.plan(
                before = before,
                action = action,
                after = transition.state,
                facts = transition.facts,
                nextId = current.nextVisualEventId,
            )
            // Ambient ticks (gravity/lock countdowns) change timers without
            // producing facts. They must not wipe a transient visual event
            // whose animation the board overlay still owns by event id.
            // Explicit clears (New Game, tutorial finish) bypass this path.
            dispatch(
                Msg.GameChanged(
                    game = transition.state,
                    visualEvent = visualEvent ?: current.visualEvent,
                    nextVisualEventId = visualEvent?.let { it.id + 1L }
                        ?: current.nextVisualEventId,
                ),
            )
            audio.onTransition(action, transition.state, transition.facts)

            val tutorialProgress = state().tutorialProgress
            if (!state().tutorialSeen && tutorialProgress != null) {
                val advanced = tutorialProgress.accept(transition.facts)
                if (advanced != tutorialProgress) dispatch(Msg.TutorialProgressChanged(advanced))
                if (advanced.complete) completeTutorial(transition.state)
                return
            }

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

        private fun completeTutorial(practice: FallingBlocksState) {
            if (state().tutorialSeen || tutorialCompletionInFlight) return
            tutorialCompletionInFlight = true
            scope.launch {
                persistTutorialSeen()
                val fresh = engine.initial(seedSource.nextSeed(), practice.runId + 1)
                dispatch(Msg.TutorialFinished(fresh))
                persistence.flush(fresh)
            }
        }

        private suspend fun persistTutorialSeen() {
            while (currentCoroutineContext().isActive) {
                try {
                    tutorial.markTutorialSeen()
                    return
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    delay(TUTORIAL_RETRY_MILLIS)
                }
            }
        }
    }

    private companion object {
        const val TUTORIAL_RETRY_MILLIS = 250L
    }
}
