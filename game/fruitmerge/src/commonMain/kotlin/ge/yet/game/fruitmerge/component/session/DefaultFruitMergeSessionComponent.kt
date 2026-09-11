package ge.yet.game.fruitmerge.component.session

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import dev.zacsweers.metro.Inject
import ge.yet.game.fruitmerge.component.game.DefaultFruitMergeComponent
import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.game.PaidActionToken
import ge.yet.game.fruitmerge.component.result.FruitMergeResultComponent
import ge.yet.game.fruitmerge.component.result.FruitMergeResultSnapshot
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import kotlinx.serialization.Serializable

internal class DefaultFruitMergeSessionComponent(
    componentContext: ComponentContext,
    private val gameFactory: FruitMergeComponent.Factory,
    private val resultFactory: FruitMergeResultComponent.Factory,
) : FruitMergeSessionComponent,
    ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()
    private var lastRunId = 1L

    override val stack: Value<ChildStack<*, FruitMergeSessionComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Playing(runId = 1L, isNewGame = false),
        handleBackButton = false,
        childFactory = ::createChild,
    )

    override val frameMode: Value<MiniAppFrameMode> = stack.map { childStack ->
        when (childStack.active.instance) {
            is FruitMergeSessionComponent.Child.Playing -> MiniAppFrameMode.Standard
            is FruitMergeSessionComponent.Child.Result -> MiniAppFrameMode.ContentOnly
        }
    }

    override val game: FruitMergeComponent
        get() = gameComponent

    internal val gameComponent: DefaultFruitMergeComponent
        get() = stack.value.items
            .mapNotNull { child -> child.instance as? FruitMergeSessionComponent.Child.Playing }
            .last()
            .component as DefaultFruitMergeComponent

    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): FruitMergeSessionComponent.Child = when (config) {
        is Config.Playing -> FruitMergeSessionComponent.Child.Playing(
            gameFactory.create(
                componentContext = componentContext,
                isNewGame = config.isNewGame,
                onGameCompleted = ::showResult,
            ) as DefaultFruitMergeComponent,
        )

        is Config.Result -> FruitMergeSessionComponent.Child.Result(
            resultFactory.create(
                componentContext = componentContext,
                snapshot = config.snapshot,
                onNewGameRequested = ::startNewRun,
            ),
        )
    }

    private fun showResult(snapshot: FruitMergeResultSnapshot) {
        navigation.navigate { configurations ->
            if (configurations.lastOrNull() is Config.Result) {
                configurations
            } else {
                configurations + Config.Result(snapshot)
            }
        }
    }

    private fun startNewRun() {
        navigation.replaceAll(Config.Playing(runId = ++lastRunId, isNewGame = true))
    }

    override fun completePaidAction(token: PaidActionToken) {
        game.completePaidAction(token)
    }

    override fun handleBack(): Boolean =
        (stack.value.active.instance as? FruitMergeSessionComponent.Child.Playing)
            ?.component
            ?.handleBack()
            ?: false
}

@Inject
internal class DefaultFruitMergeSessionComponentFactory(
    private val gameFactory: FruitMergeComponent.Factory,
    private val resultFactory: FruitMergeResultComponent.Factory,
) : FruitMergeSessionComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
    ): DefaultFruitMergeSessionComponent = DefaultFruitMergeSessionComponent(
        componentContext = componentContext,
        gameFactory = gameFactory,
        resultFactory = resultFactory,
    )
}

@Serializable
private sealed interface Config {
    @Serializable
    data class Playing(val runId: Long, val isNewGame: Boolean) : Config

    @Serializable
    data class Result(val snapshot: FruitMergeResultSnapshot) : Config
}
