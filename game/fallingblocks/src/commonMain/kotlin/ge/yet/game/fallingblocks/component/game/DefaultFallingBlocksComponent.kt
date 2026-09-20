package ge.yet.game.fallingblocks.component.game

import com.app.common.decompose.asValue
import com.app.common.decompose.componentCoroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.Inject
import ge.yet.game.fallingblocks.component.game.store.FallingBlocksStore
import ge.yet.game.fallingblocks.component.game.store.FallingBlocksStoreFactory
import ge.yet.game.fallingblocks.component.game.store.TickPlanner
import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class DefaultFallingBlocksComponent(
    componentContext: ComponentContext,
    private val store: FallingBlocksStore,
    visibility: MiniAppVisibilitySource,
) : FallingBlocksComponent, ComponentContext by componentContext {
    private val scope = componentCoroutineScope()
    private val planner = TickPlanner()

    override val model: Value<FallingBlocksComponent.Model> = store.asValue().map { state ->
        FallingBlocksComponent.Model(
            game = state.game,
            loading = state.loading,
            tutorialSeen = state.tutorialSeen,
            bestScore = state.bestScore,
            active = state.active,
        )
    }

    init {
        scope.launch {
            visibility.visibility.collectLatest { current ->
                planner.reset()
                if (current == MiniAppVisibility.ACTIVE) runTicks()
            }
        }
        lifecycle.doOnDestroy(store::dispose)
    }

    override fun rotate() = store.accept(FallingBlocksStore.Intent.Rotate)
    override fun move(cells: Int) = store.accept(FallingBlocksStore.Intent.Move(cells))
    override fun softDrop(cells: Int) = store.accept(FallingBlocksStore.Intent.SoftDrop(cells))
    override fun hardDrop() = store.accept(FallingBlocksStore.Intent.HardDrop)
    override fun revive() = store.accept(FallingBlocksStore.Intent.Revive)
    override fun newGame() = store.accept(FallingBlocksStore.Intent.NewGame)
    override fun completeTutorial() = store.accept(FallingBlocksStore.Intent.TutorialCompleted)

    private suspend fun runTicks() {
        while (currentCoroutineContext().isActive) {
            delay(FRAME_MILLIS.toLong())
            planner.consume(FRAME_MILLIS).forEach { elapsed ->
                store.accept(FallingBlocksStore.Intent.Frame(elapsed))
            }
        }
    }

    private companion object {
        const val FRAME_MILLIS = 16
    }
}

@Inject
internal class DefaultFallingBlocksComponentFactory(
    private val storeFactory: FallingBlocksStoreFactory,
    private val visibility: MiniAppVisibilitySource,
) : FallingBlocksComponent.Factory {
    override fun create(componentContext: ComponentContext): FallingBlocksComponent =
        DefaultFallingBlocksComponent(
            componentContext = componentContext,
            store = storeFactory.create(),
            visibility = visibility,
        )
}
