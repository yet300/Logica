package ge.yet.game.twentyfortyeight.component.playing

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import ge.yet.game.twentyfortyeight.component.overlay.OverlayComponent
import ge.yet.game.twentyfortyeight.domain.model.Direction
import ge.yet.game.twentyfortyeight.domain.model.RuntimeBoard
import ge.yet.game.twentyfortyeight.component.playing.store.OverlayState
import ge.yet.game.twentyfortyeight.component.playing.store.TwentyFortyEightStore
import ge.yet.game.twentyfortyeight.component.playing.store.VisualTransition

internal interface PlayingComponent {
    val model: Value<Model>
    val overlay: Value<ChildSlot<*, OverlayComponent>>

    fun onMove(direction: Direction)
    fun onUndoRequested()
    fun onRestartRequested()
    fun onContinueAfterVictory()
    fun onTutorialSkipped()
    fun onAnimationCompleted(transitionId: Long)
    fun handleBack(): Boolean

    data class Model(
        val board: RuntimeBoard?,
        val transition: VisualTransition?,
        val score: Long,
        val bestScore: Long,
        val bestImprovedInRun: Boolean = false,
        val gesturesEnabled: Boolean,
        val undoEnabled: Boolean,
        val tutorialVisible: Boolean,
        val overlay: OverlayState?,
        val persistenceStatus: PersistenceStatus,
    )

    enum class PersistenceStatus { Clean, Saving, Dirty }

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
            store: TwentyFortyEightStore,
        ): PlayingComponent
    }
}
