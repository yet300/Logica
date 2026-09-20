package ge.yet.game.fallingblocks.component.result

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

internal interface ResultComponent {
    val model: Value<Model>

    fun onPrimaryClicked(requestContinue: (onApproved: () -> Unit) -> Unit)
    fun onContinueFailed()

    data class Model(
        val score: Long,
        val bestScore: Long,
        val canContinue: Boolean,
        val continueSecondsRemaining: Int,
    ) {
        val isContinuePhase: Boolean get() = canContinue && continueSecondsRemaining > 0
    }

    interface Factory {
        fun create(
            componentContext: ComponentContext,
            score: Long,
            bestScore: Long,
            canContinue: Boolean,
            onContinueRequested: () -> Unit,
            onNewGameRequested: () -> Unit,
        ): ResultComponent
    }
}
