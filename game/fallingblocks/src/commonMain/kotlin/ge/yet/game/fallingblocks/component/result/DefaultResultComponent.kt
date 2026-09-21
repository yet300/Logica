package ge.yet.game.fallingblocks.component.result

import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.Inject
import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

internal class DefaultResultComponent(
    componentContext: ComponentContext,
    snapshot: FallingBlocksResultSnapshot,
    canContinue: Boolean,
    private val visibility: MiniAppVisibilitySource,
    private val onContinueRequested: () -> Unit,
    private val onNewGameRequested: () -> Unit,
) : ResultComponent, ComponentContext by componentContext {
    private val scope = coroutineScope()
    private val mutableModel = MutableValue(
        ResultComponent.Model(snapshot, canContinue, if (canContinue) 5 else 0),
    )
    override val model: Value<ResultComponent.Model> = mutableModel
    private var countdown: Job? = null
    private var terminalActionHandled = false

    init { restartCountdown() }

    override fun onPrimaryClicked(requestContinue: (onApproved: () -> Unit) -> Unit) {
        if (terminalActionHandled) return
        terminalActionHandled = true
        countdown?.cancel()
        if (mutableModel.value.isContinuePhase) {
            var approved = false
            requestContinue {
                scope.launch {
                    if (approved) return@launch
                    approved = true
                    onContinueRequested()
                }
            }
        } else {
            onNewGameRequested()
        }
    }

    override fun onContinueFailed() {
        terminalActionHandled = false
        mutableModel.value = mutableModel.value.copy(continueSecondsRemaining = 5)
        restartCountdown()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun restartCountdown() {
        countdown?.cancel()
        if (!mutableModel.value.isContinuePhase) return
        countdown = scope.launch {
            visibility.visibility.flatMapLatest { state ->
                if (state == MiniAppVisibility.ACTIVE) flow {
                    while (mutableModel.value.continueSecondsRemaining > 0) {
                        delay(1_000)
                        emit(Unit)
                    }
                } else emptyFlow()
            }.collect {
                val remaining = mutableModel.value.continueSecondsRemaining
                if (remaining > 0) mutableModel.value = mutableModel.value.copy(
                    continueSecondsRemaining = remaining - 1,
                )
            }
        }
    }
}

@Inject
internal class DefaultResultComponentFactory(
    private val visibility: MiniAppVisibilitySource,
) : ResultComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
        snapshot: FallingBlocksResultSnapshot,
        canContinue: Boolean,
        onContinueRequested: () -> Unit,
        onNewGameRequested: () -> Unit,
    ): ResultComponent = DefaultResultComponent(
        componentContext, snapshot, canContinue, visibility,
        onContinueRequested, onNewGameRequested,
    )
}
