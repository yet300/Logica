package ge.yet.game.twentyfortyeight.component.overlay

import com.arkivanov.decompose.value.MutableValue
import dev.zacsweers.metro.Inject

@Inject
internal class DefaultOverlayComponentFactory : OverlayComponent.Factory {
    override fun createVictory(
        score: Long,
        bestScore: Long,
        onContinue: () -> Unit,
        onRestart: () -> Unit,
        onDismiss: () -> Unit,
    ): OverlayComponent = OverlayComponent.Victory(
        model = MutableValue(OverlayComponent.Model.Victory(score, bestScore)),
        onContinue = onContinue,
        onRestart = onRestart,
        onDismiss = onDismiss,
    )

    override fun createRestartConfirmation(
        score: Long,
        successfulMovesInRun: Long,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ): OverlayComponent = OverlayComponent.RestartConfirmation(
        model = MutableValue(OverlayComponent.Model.RestartConfirmation(score, successfulMovesInRun)),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
