package ge.yet.game.fruitmerge.session

import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.zacsweers.metro.Inject
import ge.yet.game.fruitmerge.domain.model.TargetingMode
import ge.yet.game.fruitmerge.domain.repository.TutorialSeenRepository
import ge.yet.game.fruitmerge.session.store.FruitMergeStore
import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(DelicateDecomposeApi::class)
internal class DefaultFruitMergeComponent(
    componentContext: ComponentContext,
    private val store: FruitMergeStore,
    private val tutorial: TutorialSeenRepository,
    private val visibility: MiniAppVisibilitySource,
) : FruitMergeComponent,
    ComponentContext by componentContext {
    private val mutableModel = MutableValue(
        FruitMergeComponent.Model(
            game = store.state.game,
            initialized = store.state.initialized,
            visible = visibility.visibility.value == MiniAppVisibility.ACTIVE,
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

    init {
        val scope = coroutineScope()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            store.states.collect { state ->
                mutableModel.value = mutableModel.value.copy(
                    game = state.game,
                    initialized = state.initialized,
                )
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            visibility.visibility.collect { value ->
                val active = value == MiniAppVisibility.ACTIVE
                mutableModel.value = mutableModel.value.copy(visible = active)
                store.accept(FruitMergeStore.Intent.VisibilityChanged(active))
            }
        }
        scope.launch {
            val seen = tutorial.isTutorialSeen()
            if (alive) {
                mutableModel.value = mutableModel.value.copy(
                    tutorialReady = true,
                    tutorialStep = if (seen) null else TutorialStep.Gesture,
                )
            }
        }
        lifecycle.doOnDestroy {
            alive = false
            pendingToken = null
            presentationChannel.close()
        }
    }

    override fun frame(elapsedSeconds: Float) {
        if (alive && model.value.visible) store.accept(FruitMergeStore.Intent.Frame(elapsedSeconds))
    }

    override fun movePreview(x: Float) {
        if (alive) store.accept(FruitMergeStore.Intent.MovePreview(x))
    }

    override fun drop(dragged: Boolean) {
        if (!alive || !model.value.tutorialReady) return
        val previousNextBodyId = store.state.game.nextBodyId
        store.accept(FruitMergeStore.Intent.Drop)
        if (store.state.game.nextBodyId != previousNextBodyId) onDropAccepted()
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

    override fun newGame() {
        if (!alive) return
        pendingToken = null
        pendingClearPaid = false
        store.accept(FruitMergeStore.Intent.NewGame)
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
        if (alive && label is FruitMergeStore.Label.MergeResolved && model.value.tutorialStep == TutorialStep.Merge) {
            mutableModel.value = mutableModel.value.copy(tutorialStep = TutorialStep.Traits)
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

    private fun onDropAccepted() {
        if (model.value.tutorialStep == TutorialStep.Gesture) {
            mutableModel.value = mutableModel.value.copy(tutorialStep = TutorialStep.Merge)
        }
    }

    private fun finishTutorial() {
        mutableModel.value = mutableModel.value.copy(tutorialStep = null)
        coroutineScope().launch { tutorial.markTutorialSeen() }
    }
}

@Inject
internal class DefaultFruitMergeComponentFactory(
    private val tutorial: TutorialSeenRepository,
    private val visibility: MiniAppVisibilitySource,
) : FruitMergeComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
        store: FruitMergeStore,
    ): DefaultFruitMergeComponent = DefaultFruitMergeComponent(
        componentContext = componentContext,
        store = store,
        tutorial = tutorial,
        visibility = visibility,
    )
}
