package ge.yet.game.fruitmerge.component.game

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import dev.zacsweers.metro.Inject
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.component.game.integration.toModel
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStore
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStoreFactory
import ge.yet.game.fruitmerge.component.result.FruitMergeResultSnapshot
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.RunPhase
import ge.yet.game.fruitmerge.domain.model.TargetingMode
import ge.yet.game.fruitmerge.domain.repository.TutorialSeenRepository
import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(DelicateDecomposeApi::class)
internal class DefaultFruitMergeComponent(
    componentContext: ComponentContext,
    private val gameStoreFactory: FruitMergeStoreFactory,
    private val audio: FruitMergeAudioAdapter,
    private val tutorial: TutorialSeenRepository,
    private val visibility: MiniAppVisibilitySource,
    private val isNewGame: Boolean,
    private val onGameCompletedCb: (FruitMergeResultSnapshot) -> Unit,
) : FruitMergeComponent,
    ComponentContext by componentContext {
    internal val store: FruitMergeStore = instanceKeeper.getStore {
        gameStoreFactory.create(isNewGame)
    }
    // UI-local flags merged with store state via integration/Mappers.toModel.
    // Kept in the component (not the store) because they mirror host/session
    // sources: visibility gate, one-shot tutorial overlay. The store stays the
    // single owner of game/initialized/active.
    private var visibleFlag: Boolean = visibility.visibility.value == MiniAppVisibility.ACTIVE
    private var tutorialReadyFlag: Boolean = false
    private var tutorialStepFlag: TutorialStep? = null
    private val mutableModel = MutableValue(
        store.state.toModel(
            visible = visibleFlag,
            tutorialReady = tutorialReadyFlag,
            tutorialStep = tutorialStepFlag,
        ),
    )
    override val model: Value<FruitMergeComponent.Model> = mutableModel
    private val presentationChannel = Channel<FruitMergeComponent.PresentationEvent>(capacity = Channel.BUFFERED)
    override val presentationEvents: Flow<FruitMergeComponent.PresentationEvent> = presentationChannel.receiveAsFlow()

    private val sessionKey = componentContext.hashCode().toLong()
    private var nextTokenId = 1L
    private var pendingToken: PaidActionToken? = null
    private var pendingClearPaid = false
    private var alive = true
    private var completionReported = false

    init {
        audio.start()
        val storeValue = store.asValue()
        val subscription = storeValue.subscribe { state ->
            refreshModel(state)
            if (state.initialized && state.game.phase == RunPhase.RESULT) {
                reportCompletion(state.game)
            }
        }
        val scope = coroutineScope()
        scope.launch {
            store.labels.collect { label ->
                onStoreLabel(label)
                audio.play(label)
                if (label is FruitMergeStore.Label.ResultReached) {
                    reportCompletion(store.state.game)
                }
            }
        }
        scope.launch {
            visibility.visibility.collect { value ->
                val active = value == MiniAppVisibility.ACTIVE
                visibleFlag = active
                refreshModel()
                store.accept(FruitMergeStore.Intent.VisibilityChanged(active))
            }
        }
        scope.launch {
            val seen = tutorial.isTutorialSeen()
            if (alive) {
                tutorialReadyFlag = true
                tutorialStepFlag = if (seen) null else TutorialStep.Gesture
                refreshModel()
            }
        }
        lifecycle.doOnDestroy {
            alive = false
            pendingToken = null
            subscription.cancel()
            presentationChannel.close()
        }
    }

    private fun refreshModel() {
        refreshModel(store.state)
    }

    private fun refreshModel(state: FruitMergeStore.State) {
        mutableModel.value = state.toModel(
            visible = visibleFlag,
            tutorialReady = tutorialReadyFlag,
            tutorialStep = tutorialStepFlag,
        )
    }

    override fun frame(elapsedSeconds: Float) {
        if (alive && model.value.visible) store.accept(FruitMergeStore.Intent.Frame(elapsedSeconds))
    }

    override fun movePreview(x: Float) {
        if (alive) store.accept(FruitMergeStore.Intent.MovePreview(x))
    }

    override fun drop(dragged: Boolean) {
        if (!alive || !model.value.tutorialReady) return
        // Accepted-drop detection is label-driven (DropReleased -> Gesture-to-Merge
        // in onStoreLabel). Never read store.state synchronously after accept:
        // MVIKotlin dispatch is asynchronous and the read would race.
        store.accept(FruitMergeStore.Intent.Drop)
    }

    override fun requestClearGate(): PaidActionToken? {
        if (!alive || !model.value.initialized || pendingToken != null) return null
        if (model.value.game.targetingMode == TargetingMode.CLEAR) return null
        if (model.value.game.freeClears > 0) {
            pendingClearPaid = false
            store.accept(FruitMergeStore.Intent.BeginFreeClear)
            return null
        }
        return createToken(PaidAction.CLEAR)
    }

    override fun selectClearTarget(id: Long) {
        if (!alive || model.value.game.targetingMode != TargetingMode.CLEAR) return
        store.accept(FruitMergeStore.Intent.ClearBody(id, paid = pendingClearPaid))
        pendingClearPaid = false
    }

    override fun cancelClear() {
        pendingClearPaid = false
        if (alive) store.accept(FruitMergeStore.Intent.CancelClear)
    }

    override fun requestShakeGate(): PaidActionToken? {
        if (!alive || !model.value.initialized || pendingToken != null) return null
        if (model.value.game.shakeStepsRemaining > 0) return null
        if (model.value.game.freeShakes > 0) {
            store.accept(FruitMergeStore.Intent.FreeShake)
            return null
        }
        return createToken(PaidAction.SHAKE)
    }

    override fun completePaidAction(token: PaidActionToken) {
        if (!alive || token != pendingToken || token.sessionKey != sessionKey) return
        if (token.runOrdinal != model.value.game.runOrdinal) {
            pendingToken = null
            return
        }
        pendingToken = null
        when (token.action) {
            PaidAction.CLEAR -> {
                pendingClearPaid = true
                store.accept(FruitMergeStore.Intent.PaidClear)
            }
            PaidAction.SHAKE -> store.accept(FruitMergeStore.Intent.PaidShake)
        }
    }

    override fun skipTutorial() {
        if (!alive || model.value.tutorialStep == null) return
        finishTutorial()
    }

    override fun completeTutorial() {
        if (!alive || model.value.tutorialStep != TutorialStep.Traits) return
        finishTutorial()
    }

    internal fun onStoreLabel(label: FruitMergeStore.Label) {
        if (alive && label is FruitMergeStore.Label.DropReleased && tutorialStepFlag == TutorialStep.Gesture) {
            tutorialStepFlag = TutorialStep.Merge
            refreshModel()
        }
        if (alive && label is FruitMergeStore.Label.MergeResolved && tutorialStepFlag == TutorialStep.Merge) {
            tutorialStepFlag = TutorialStep.Traits
            refreshModel()
        }
        if (!alive || !model.value.visible) return
        val event = when (label) {
            is FruitMergeStore.Label.FruitLanded -> FruitMergeComponent.PresentationEvent.Landing(label.level, label.position)
            is FruitMergeStore.Label.MergeResolved -> FruitMergeComponent.PresentationEvent.Merge(label.level, label.position)
            is FruitMergeStore.Label.ClearApplied -> FruitMergeComponent.PresentationEvent.Clear(label.level, label.position)
            is FruitMergeStore.Label.ShakePulse -> FruitMergeComponent.PresentationEvent.ShakePulse(label.index)
            is FruitMergeStore.Label.DropReleased,
            FruitMergeStore.Label.ShakeStarted,
            FruitMergeStore.Label.DangerEntered,
            FruitMergeStore.Label.ResultReached,
            -> null
        }
        if (event != null) presentationChannel.trySend(event)
    }

    override fun handleBack(): Boolean = if (model.value.game.targetingMode == TargetingMode.CLEAR) {
        cancelClear()
        true
    } else {
        false
    }

    private fun reportCompletion(game: FruitMergeState) {
        if (!alive || completionReported || game.phase != RunPhase.RESULT) return
        completionReported = true
        onGameCompletedCb(FruitMergeResultSnapshot.from(game))
    }

    private fun createToken(action: PaidAction): PaidActionToken {
        val token = PaidActionToken(
            sessionKey = sessionKey,
            runOrdinal = model.value.game.runOrdinal,
            id = nextTokenId,
            action = action,
        )
        if (nextTokenId < Long.MAX_VALUE) nextTokenId += 1L
        pendingToken = token
        return token
    }

    private fun finishTutorial() {
        tutorialStepFlag = null
        refreshModel()
        coroutineScope().launch { tutorial.markTutorialSeen() }
    }
}

@Inject
internal class DefaultFruitMergeComponentFactory(
    private val gameStoreFactory: FruitMergeStoreFactory,
    private val audio: FruitMergeAudioAdapter,
    private val tutorial: TutorialSeenRepository,
    private val visibility: MiniAppVisibilitySource,
) : FruitMergeComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
        isNewGame: Boolean,
        onGameCompleted: (FruitMergeResultSnapshot) -> Unit,
    ): DefaultFruitMergeComponent = DefaultFruitMergeComponent(
        componentContext = componentContext,
        gameStoreFactory = gameStoreFactory,
        audio = audio,
        tutorial = tutorial,
        visibility = visibility,
        isNewGame = isNewGame,
        onGameCompletedCb = onGameCompleted,
    )
}
