package ge.yet.game.fallingblocks.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.Inject
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.result.FallingBlocksResultSnapshot
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    private val gameFactory: FallingBlocksComponent.Factory,
    private val resultFactory: ResultComponent.Factory,
) : RootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()
    private var lastGameInstanceId = 1L

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Playing(instanceId = 1L, startFresh = false),
        handleBackButton = false,
        childFactory = ::createChild,
    )

    override val frameMode: Value<MiniAppFrameMode> = stack.map { children ->
        when (children.active.instance) {
            is RootComponent.Child.Playing -> MiniAppFrameMode.Standard
            is RootComponent.Child.Result -> MiniAppFrameMode.ContentOnly
        }
    }

    override fun handleBack(): Boolean = stack.value.active.instance is RootComponent.Child.Result

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child = when (config) {
        is Config.Playing -> {
            lastGameInstanceId = maxOf(lastGameInstanceId, config.instanceId)
            val component = gameFactory.create(
                componentContext = context,
                startFresh = config.startFresh,
                onToppedOut = { runId -> showResult(config.instanceId, runId) },
            )
            val subscription = component.model.subscribe { model ->
                model.game
                    ?.takeIf { it.phase == GamePhase.TERMINAL }
                    ?.let { showResult(config.instanceId, it.runId) }
            }
            context.lifecycle.doOnDestroy(subscription::cancel)
            RootComponent.Child.Playing(component)
        }

        is Config.Result -> RootComponent.Child.Result(createResult(config, context))
    }

    private fun showResult(gameInstanceId: Long, runId: Long) {
        val playing = findPlaying(gameInstanceId) ?: return
        val model = playing.model.value
        val game = model.game ?: return
        if (game.runId != runId || game.phase != GamePhase.TERMINAL) return
        val active = stack.value.active.configuration
        if (active is Config.Result && active.gameInstanceId == gameInstanceId) return
        navigation.navigate { configurations ->
            if (configurations.lastOrNull() is Config.Result) return@navigate configurations
            if (configurations.none { it is Config.Playing && it.instanceId == gameInstanceId }) {
                return@navigate configurations
            }
            configurations + Config.Result(
                gameInstanceId = gameInstanceId,
                snapshot = FallingBlocksResultSnapshot.from(game, model.bestScore),
                canContinue = game.revivesUsed == 0,
            )
        }
    }

    private fun createResult(config: Config.Result, context: ComponentContext): ResultComponent {
        lateinit var origin: ResultComponent
        origin = resultFactory.create(
            componentContext = context,
            snapshot = config.snapshot,
            canContinue = config.canContinue,
            onContinueRequested = continueRequest@{
                if (!isActiveResult(origin, config)) return@continueRequest
                val playing = findPlaying(config.gameInstanceId)
                playing?.revive()
                val game = playing?.model?.value?.game
                if (game?.runId == config.snapshot.runId && game.phase == GamePhase.PLAYING) {
                    navigation.navigate { it.dropLast(1) }
                } else {
                    origin.onContinueFailed()
                }
            },
            onNewGameRequested = newGameRequest@{
                if (!isActiveResult(origin, config)) return@newGameRequest
                navigation.replaceAll(
                    Config.Playing(instanceId = ++lastGameInstanceId, startFresh = true),
                )
            },
        )
        return origin
    }

    private fun findPlaying(instanceId: Long): FallingBlocksComponent? = stack.value.items
        .asReversed()
        .firstNotNullOfOrNull { child ->
            val config = child.configuration as? Config.Playing
            if (config?.instanceId == instanceId) {
                (child.instance as? RootComponent.Child.Playing)?.component
            } else {
                null
            }
        }

    private fun isActiveResult(origin: ResultComponent, config: Config.Result): Boolean {
        val active = stack.value.active
        return active.configuration == config &&
            (active.instance as? RootComponent.Child.Result)?.component === origin
    }
}

@Inject
internal class DefaultRootComponentFactory(
    private val gameFactory: FallingBlocksComponent.Factory,
    private val resultFactory: ResultComponent.Factory,
) : RootComponent.Factory {
    override fun create(componentContext: ComponentContext): RootComponent =
        DefaultRootComponent(componentContext, gameFactory, resultFactory)
}

@Serializable
private sealed interface Config {
    @Serializable
    data class Playing(val instanceId: Long, val startFresh: Boolean) : Config

    @Serializable
    data class Result(
        val gameInstanceId: Long,
        val snapshot: FallingBlocksResultSnapshot,
        val canContinue: Boolean,
    ) : Config
}
