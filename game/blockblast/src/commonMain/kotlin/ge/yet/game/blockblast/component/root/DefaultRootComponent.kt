package ge.yet.game.blockblast.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import dev.zacsweers.metro.Inject
import ge.yet.game.blockblast.component.game.GameComponent
import ge.yet.game.blockblast.component.result.BlockBlastResultSnapshot
import ge.yet.game.blockblast.component.result.GameResultComponent
import ge.yet.game.blockblast.domain.model.GameState
import ge.yet.game.miniapp.api.MiniAppReviewOpportunity
import ge.yet.game.miniapp.api.MiniAppSessionHost
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    private val gameFactory: GameComponent.Factory,
    private val resultFactory: GameResultComponent.Factory,
    @Suppress("UNUSED_PARAMETER") visibility: MiniAppVisibilitySource,
    private val host: MiniAppSessionHost,
) : RootComponent,
    ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()
    private var lastGameInstanceId = 1L

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Playing(
            instanceId = 1L,
            isNewGame = false,
        ),
        handleBackButton = false,
        childFactory = ::createChild,
    )

    override val frameMode: Value<MiniAppFrameMode> = stack.map { childStack ->
        when (childStack.active.instance) {
            is RootComponent.Child.Playing -> MiniAppFrameMode.Standard
            is RootComponent.Child.Result -> MiniAppFrameMode.ContentOnly
        }
    }

    override fun handleBack(): Boolean =
        (stack.value.active.instance as? RootComponent.Child.Playing)
            ?.component
            ?.handleBack()
            ?: false

    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): RootComponent.Child = when (config) {
        is Config.Playing -> {
            lastGameInstanceId = maxOf(lastGameInstanceId, config.instanceId)
            RootComponent.Child.Playing(
                gameFactory.create(
                    componentContext = componentContext,
                    isNewGame = config.isNewGame,
                    restoredResultState = null,
                    onGameCompleted = { finalState, canContinue, reviewOpportunity ->
                        showResult(
                            gameInstanceId = config.instanceId,
                            finalState = finalState,
                            canContinue = canContinue,
                            reviewOpportunity = reviewOpportunity,
                        )
                    },
                    onReviveCompleted = { playableState ->
                        finishContinue(config.instanceId, playableState)
                    },
                    onReviveFailed = { failContinue(config.instanceId) },
                ),
            )
        }

        is Config.Result -> RootComponent.Child.Result(
            resultFactory.create(
                componentContext = componentContext,
                snapshot = config.snapshot,
                canContinue = config.canContinue,
                onContinueRequested = { continueGame(config) },
                onNewGameRequested = { startNewGame(config) },
            ),
        )
    }

    private fun continueGame(resultConfig: Config.Result) {
        if (!isActiveResult(resultConfig)) return
        val game = stack.value.items
            .asReversed()
            .firstNotNullOfOrNull { child ->
                val config = child.configuration as? Config.Playing
                if (config?.instanceId == resultConfig.gameInstanceId) {
                    (child.instance as? RootComponent.Child.Playing)?.component
                } else {
                    null
                }
            }
        game?.onReviveClicked()
    }

    private fun finishContinue(
        gameInstanceId: Long,
        playableState: GameState,
    ) {
        if (playableState.isGameOver) return
        navigation.navigate { configurations ->
            val activeResult = configurations.lastOrNull() as? Config.Result
            if (
                activeResult?.gameInstanceId != gameInstanceId ||
                configurations.none {
                    it is Config.Playing && it.instanceId == gameInstanceId
                }
            ) {
                configurations
            } else {
                configurations.dropLast(1)
            }
        }
    }

    private fun failContinue(gameInstanceId: Long) {
        val active = stack.value.active
        val config = active.configuration as? Config.Result
        if (config?.gameInstanceId != gameInstanceId) return
        (active.instance as? RootComponent.Child.Result)
            ?.component
            ?.onContinueFailed()
    }

    private fun startNewGame(resultConfig: Config.Result) {
        if (!isActiveResult(resultConfig)) return
        navigation.replaceAll(
            Config.Playing(
                instanceId = ++lastGameInstanceId,
                isNewGame = true,
            ),
        )
    }

    private fun isActiveResult(config: Config.Result): Boolean =
        stack.value.active.configuration == config

    private fun showResult(
        gameInstanceId: Long,
        finalState: GameState,
        canContinue: Boolean,
        reviewOpportunity: Boolean,
    ) {
        var added = false
        val snapshot = BlockBlastResultSnapshot.from(finalState)
        navigation.navigate { configurations ->
            if (configurations.lastOrNull() is Config.Result) return@navigate configurations
            if (
                configurations.none {
                    it is Config.Playing && it.instanceId == gameInstanceId
                }
            ) {
                return@navigate configurations
            }
            added = true
            configurations + Config.Result(gameInstanceId, snapshot, canContinue)
        }
        if (added && reviewOpportunity) {
            host.requestReview(
                MiniAppReviewOpportunity(
                    triggerId = "block_blast_result",
                    score = finalState.score,
                    bestScore = finalState.bestScore,
                    revivesUsed = finalState.revivesUsed,
                ),
            )
        }
    }
}

@Inject
internal class DefaultRootComponentFactory(
    private val gameFactory: GameComponent.Factory,
    private val resultFactory: GameResultComponent.Factory,
) : RootComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
        visibility: MiniAppVisibilitySource,
        host: MiniAppSessionHost,
    ): RootComponent = DefaultRootComponent(
        componentContext = componentContext,
        gameFactory = gameFactory,
        resultFactory = resultFactory,
        visibility = visibility,
        host = host,
    )
}

@Serializable
private sealed interface Config {
    @Serializable
    data class Playing(
        val instanceId: Long,
        val isNewGame: Boolean,
    ) : Config

    @Serializable
    data class Result(
        val gameInstanceId: Long,
        val snapshot: BlockBlastResultSnapshot,
        val canContinue: Boolean,
    ) : Config
}
