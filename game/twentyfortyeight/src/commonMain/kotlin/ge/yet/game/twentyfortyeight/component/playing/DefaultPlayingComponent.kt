package ge.yet.game.twentyfortyeight.component.playing

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.Inject
import ge.yet.game.twentyfortyeight.component.overlay.OverlayComponent
import ge.yet.game.twentyfortyeight.component.playing.integration.stateToModel
import ge.yet.game.twentyfortyeight.engine.Direction
import ge.yet.game.twentyfortyeight.component.playing.store.OverlayState
import ge.yet.game.twentyfortyeight.component.playing.store.TwentyFortyEightStore
import kotlinx.serialization.Serializable

internal class DefaultPlayingComponent(
    componentContext: ComponentContext,
    private val store: TwentyFortyEightStore,
    private val overlayFactory: OverlayComponent.Factory,
) : PlayingComponent,
    ComponentContext by componentContext {

    private val overlayNavigation = SlotNavigation<OverlayConfig>()
    private var componentAlive = true

    override val model: Value<PlayingComponent.Model> = store.asValue().map(stateToModel)

    override val overlay: Value<ChildSlot<*, OverlayComponent>> = childSlot(
        source = overlayNavigation,
        serializer = OverlayConfig.serializer(),
        key = "TwentyFortyEightOverlay",
        handleBackButton = false,
        childFactory = ::createOverlay,
    )

    init {
        val cancellation = store.asValue().subscribe { state ->
            val desired = state.overlay?.toConfig()
            val current = overlay.value.child?.configuration
            when {
                desired == null && current != null -> overlayNavigation.dismiss()
                desired != null && desired != current -> overlayNavigation.activate(desired)
            }
        }
        lifecycle.doOnDestroy {
            componentAlive = false
            cancellation.cancel()
        }
    }

    override fun onMove(direction: Direction) = store.accept(TwentyFortyEightStore.Intent.Move(direction))
    override fun onUndoRequested() = store.accept(TwentyFortyEightStore.Intent.Undo)
    override fun onRestartRequested() = store.accept(TwentyFortyEightStore.Intent.RequestRestart)
    override fun onContinueAfterVictory() = store.accept(TwentyFortyEightStore.Intent.ContinueAfterVictory)
    override fun onTutorialSkipped() = store.accept(TwentyFortyEightStore.Intent.SkipTutorial)
    override fun onAnimationCompleted(transitionId: Long) =
        store.accept(TwentyFortyEightStore.Intent.AnimationCompleted(transitionId))

    override fun handleBack(): Boolean {
        if (overlay.value.child == null) return false
        store.accept(TwentyFortyEightStore.Intent.CancelOverlay)
        return true
    }

    private fun createOverlay(
        config: OverlayConfig,
        @Suppress("UNUSED_PARAMETER") componentContext: ComponentContext,
    ): OverlayComponent {
        val state = store.state
        val game = state.game
        return when (config) {
            OverlayConfig.Victory -> {
                lateinit var origin: OverlayComponent
                origin = overlayFactory.createVictory(
                    score = game?.score ?: 0L,
                    bestScore = game?.bestScore ?: 0L,
                    onContinue = {
                        acceptFromOverlay(origin, TwentyFortyEightStore.Intent.ContinueAfterVictory)
                    },
                    onRestart = { acceptFromOverlay(origin, TwentyFortyEightStore.Intent.RequestRestart) },
                    onDismiss = { acceptFromOverlay(origin, TwentyFortyEightStore.Intent.CancelOverlay) },
                )
                origin
            }
            OverlayConfig.RestartConfirmation -> {
                lateinit var origin: OverlayComponent
                origin = overlayFactory.createRestartConfirmation(
                    score = game?.score ?: 0L,
                    successfulMovesInRun = game?.successfulMovesInRun ?: 0L,
                    onConfirm = { acceptFromOverlay(origin, TwentyFortyEightStore.Intent.ConfirmRestart) },
                    onDismiss = { acceptFromOverlay(origin, TwentyFortyEightStore.Intent.CancelOverlay) },
                )
                origin
            }
        }
    }

    private fun acceptFromOverlay(origin: OverlayComponent, intent: TwentyFortyEightStore.Intent) {
        if (componentAlive && overlay.value.child?.instance === origin) store.accept(intent)
    }
}

@Inject
internal class DefaultPlayingComponentFactory(
    private val overlayFactory: OverlayComponent.Factory,
) : PlayingComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
        store: TwentyFortyEightStore,
    ): DefaultPlayingComponent = DefaultPlayingComponent(
        componentContext = componentContext,
        store = store,
        overlayFactory = overlayFactory,
    )
}

@Serializable
private enum class OverlayConfig { Victory, RestartConfirmation }

private fun OverlayState.toConfig(): OverlayConfig = when (this) {
    OverlayState.Victory -> OverlayConfig.Victory
    OverlayState.RestartConfirmation -> OverlayConfig.RestartConfirmation
}
