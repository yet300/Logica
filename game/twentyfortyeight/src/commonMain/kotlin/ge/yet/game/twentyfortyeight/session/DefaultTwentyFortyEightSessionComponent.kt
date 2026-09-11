package ge.yet.game.twentyfortyeight.session

import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import dev.zacsweers.metro.Inject
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.twentyfortyeight.component.playing.PlayingComponent
import ge.yet.game.twentyfortyeight.component.result.ResultComponent
import ge.yet.game.twentyfortyeight.domain.model.GamePhase
import ge.yet.game.twentyfortyeight.domain.model.ResultSnapshot
import ge.yet.game.twentyfortyeight.component.playing.store.TwentyFortyEightStore
import ge.yet.game.twentyfortyeight.component.playing.store.TwentyFortyEightStoreFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(DelicateDecomposeApi::class)
internal class DefaultTwentyFortyEightSessionComponent(
    componentContext: ComponentContext,
    private val playingFactory: PlayingComponent.Factory,
    private val resultFactory: ResultComponent.Factory,
    storeFactory: TwentyFortyEightStoreFactory,
    adapter: TwentyFortyEightSessionAdapter,
    private val ports: TwentyFortyEightSessionPorts,
) : TwentyFortyEightSessionComponent,
    ComponentContext by componentContext {

    internal val retainedStore: TwentyFortyEightStore = instanceKeeper.getStore(storeFactory::create)
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, TwentyFortyEightSessionComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = retainedStore.state.toInitialConfig(),
        handleBackButton = false,
        childFactory = ::createChild,
    )

    override val frameMode: Value<MiniAppFrameMode> = stack.map { MiniAppFrameMode.Standard }
    override val effect: Value<TwentyFortyEightSessionComponent.EffectState> = ports.effect

    override fun onEffectConsumed(effectId: Long) = ports.consumeEffect(effectId)

    internal val labelCollector: Job

    init {
        ports.bind(::navigateToResult, ::onNewGameCommitted)
        labelCollector = coroutineScope().launch(start = CoroutineStart.UNDISPATCHED) {
            adapter.collect(retainedStore.labels)
        }
    }

    override fun handleBack(): Boolean =
        (stack.value.active.instance as? TwentyFortyEightSessionComponent.Child.Playing)
            ?.component
            ?.handleBack()
            ?: false

    internal fun navigateToResult(snapshot: ResultSnapshot) {
        if (stack.value.active.instance is TwentyFortyEightSessionComponent.Child.Result) return
        navigation.replaceAll(Config.Result(retainedStore.state.game?.runOrdinal ?: 0L, snapshot))
    }

    private fun onNewGameCommitted(runOrdinal: Long) {
        if (stack.value.active.instance !is TwentyFortyEightSessionComponent.Child.Result) return
        navigation.replaceAll(Config.Playing(runOrdinal))
    }

    private fun createChild(config: Config, componentContext: ComponentContext): TwentyFortyEightSessionComponent.Child =
        when (config) {
            is Config.Playing -> TwentyFortyEightSessionComponent.Child.Playing(
                playingFactory.create(componentContext, retainedStore),
            )
            is Config.Result -> TwentyFortyEightSessionComponent.Child.Result(
                resultFactory.create(config.snapshot) {
                    retainedStore.accept(TwentyFortyEightStore.Intent.NewGameFromResult)
                },
            )
        }
}

@Inject
internal class DefaultTwentyFortyEightSessionComponentFactory(
    private val playingFactory: PlayingComponent.Factory,
    private val resultFactory: ResultComponent.Factory,
    private val storeFactory: TwentyFortyEightStoreFactory,
    private val adapter: TwentyFortyEightSessionAdapter,
    private val ports: TwentyFortyEightSessionPorts,
) : TwentyFortyEightSessionComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
    ): DefaultTwentyFortyEightSessionComponent = DefaultTwentyFortyEightSessionComponent(
        componentContext = componentContext,
        playingFactory = playingFactory,
        resultFactory = resultFactory,
        storeFactory = storeFactory,
        adapter = adapter,
        ports = ports,
    )
}

@Serializable
private sealed interface Config {
    @Serializable
    data class Playing(val runOrdinal: Long) : Config

    @Serializable
    data class Result(val runOrdinal: Long, val snapshot: ResultSnapshot) : Config
}

private fun TwentyFortyEightStore.State.toInitialConfig(): Config {
    val authoritativeGame = game
    return if (authoritativeGame?.phase == GamePhase.GameOver) {
        Config.Result(
            runOrdinal = authoritativeGame.runOrdinal,
            snapshot = ResultSnapshot(
                score = authoritativeGame.score,
                bestScore = authoritativeGame.bestScore,
                highestTile = authoritativeGame.board.values().filterNotNull().maxOrNull() ?: 0L,
                statistics = statistics,
            ),
        )
    } else {
        Config.Playing(authoritativeGame?.runOrdinal ?: 0L)
    }
}
