package ge.yet.game.fallingblocks.component.result

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

internal interface ResultComponent {
    val model: Value<Model>

    fun onPrimaryClicked(requestContinue: (onApproved: () -> Unit) -> Unit)
    fun onContinueFailed()

    data class Model(
        val snapshot: FallingBlocksResultSnapshot,
        val canContinue: Boolean,
        val continueSecondsRemaining: Int,
    ) {
        val isContinuePhase: Boolean get() = canContinue && continueSecondsRemaining > 0
        val score: Long get() = snapshot.score
        val bestScore: Long get() = snapshot.bestScore
    }

    interface Factory {
        fun create(
            componentContext: ComponentContext,
            snapshot: FallingBlocksResultSnapshot,
            canContinue: Boolean,
            onContinueRequested: () -> Unit,
            onNewGameRequested: () -> Unit,
        ): ResultComponent
    }
}
