package ge.yet.game.fruitmerge.session

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.RunPhase
import ge.yet.game.fruitmerge.domain.model.Vec2
import ge.yet.game.fruitmerge.session.store.FruitMergeStore
import kotlinx.coroutines.flow.Flow

internal enum class PaidAction {
    CLEAR,
    SHAKE,
}

internal data class PaidActionToken(
    val sessionKey: Long,
    val runOrdinal: Long,
    val id: Long,
    val action: PaidAction,
)

internal sealed interface TutorialStep {
    data object Gesture : TutorialStep
    data object Merge : TutorialStep
    data object Traits : TutorialStep
}

internal interface FruitMergeComponent {
    val model: Value<Model>
    val presentationEvents: Flow<PresentationEvent>

    fun frame(elapsedSeconds: Float)
    fun movePreview(x: Float)
    fun drop(dragged: Boolean = false)
    fun requestClearGate(): PaidActionToken?
    fun selectClearTarget(id: Long)
    fun cancelClear()
    fun requestShakeGate(): PaidActionToken?
    fun completePaidAction(token: PaidActionToken)
    fun newGame()
    fun skipTutorial()
    fun completeTutorial()
    fun handleBack(): Boolean

    sealed interface ScreenState {
        val game: FruitMergeState

        data class Playing(override val game: FruitMergeState) : ScreenState

        data class GameOver(
            override val game: FruitMergeState,
            val largestFruit: FruitLevel,
        ) : ScreenState
    }

    sealed interface PresentationEvent {
        data class Landing(val level: FruitLevel, val position: Vec2) : PresentationEvent
        data class Merge(val level: FruitLevel, val position: Vec2) : PresentationEvent
        data class Clear(val level: FruitLevel, val position: Vec2) : PresentationEvent
        data class ShakePulse(val index: Int) : PresentationEvent
    }

    data class Model(
        val game: FruitMergeState = FruitMergeState(),
        val initialized: Boolean = false,
        val visible: Boolean = true,
        val tutorialReady: Boolean = false,
        val tutorialStep: TutorialStep? = null,
    ) {
        val screen: ScreenState get() = game.toScreenState()
    }

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
            store: FruitMergeStore,
        ): FruitMergeComponent
    }
}

private fun FruitMergeState.toScreenState(): FruitMergeComponent.ScreenState = when (phase) {
    RunPhase.PLAYING -> FruitMergeComponent.ScreenState.Playing(this)
    RunPhase.RESULT -> FruitMergeComponent.ScreenState.GameOver(
        game = this,
        largestFruit = bodies.maxByOrNull { body -> body.level.ordinal }?.level ?: previewLevel,
    )
}
