package ge.yet.game.fallingblocks.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.Inject
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    gameFactory: FallingBlocksComponent.Factory,
    private val resultFactory: ResultComponent.Factory,
) : RootComponent, ComponentContext by componentContext {
    private val navigation = SlotNavigation<ResultConfig>()
    override val playing = gameFactory.create(componentContext, ::showResult)
    override val result: Value<ChildSlot<*, ResultComponent>> = childSlot(
        source = navigation,
        serializer = ResultConfig.serializer(),
        key = "FallingBlocksResult",
        handleBackButton = false,
        childFactory = ::createResult,
    )
    override val frameMode: Value<MiniAppFrameMode> = MutableValue(MiniAppFrameMode.Standard)

    init {
        val subscription = playing.model.subscribe { model ->
            model.game?.takeIf { it.phase == GamePhase.TERMINAL }?.let { showResult(it.runId) }
        }
        lifecycle.doOnDestroy(subscription::cancel)
    }

    override fun handleBack(): Boolean = result.value.child != null

    private fun showResult(runId: Long) {
        val model = playing.model.value
        val game = model.game ?: return
        if (game.runId != runId || game.phase != GamePhase.TERMINAL) return
        if ((result.value.child?.configuration as? ResultConfig)?.runId == runId) return
        navigation.activate(ResultConfig(runId, game.score, maxOf(model.bestScore, game.score), game.revivesUsed == 0))
    }

    private fun createResult(config: ResultConfig, context: ComponentContext): ResultComponent {
        lateinit var origin: ResultComponent
        origin = resultFactory.create(
            componentContext = context,
            score = config.score,
            bestScore = config.bestScore,
            canContinue = config.canContinue,
            onContinueRequested = {
                if (isActive(origin, config)) {
                    playing.revive()
                    val game = playing.model.value.game
                    if (game?.runId == config.runId && game.phase == GamePhase.PLAYING) navigation.dismiss()
                    else origin.onContinueFailed()
                }
            },
            onNewGameRequested = {
                if (isActive(origin, config)) {
                    playing.newGame()
                    val game = playing.model.value.game
                    if (game != null && game.runId != config.runId && game.phase == GamePhase.PLAYING) navigation.dismiss()
                }
            },
        )
        return origin
    }

    private fun isActive(origin: ResultComponent, config: ResultConfig): Boolean =
        result.value.child?.let { it.instance === origin && it.configuration == config } == true
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
private data class ResultConfig(
    val runId: Long,
    val score: Long,
    val bestScore: Long,
    val canContinue: Boolean,
)
