package ge.yet.game.fallingblocks.component.game

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.ui.tutorial.TutorialProgress

internal interface FallingBlocksComponent {
    val model: Value<Model>

    fun rotate()
    fun move(cells: Int)
    fun softDrop(cells: Int)
    fun hardDrop()
    fun revive()
    fun newGame()

    data class Model(
        val game: FallingBlocksState?,
        val loading: Boolean,
        val tutorialSeen: Boolean,
        val tutorialProgress: TutorialProgress?,
        val bestScore: Long,
        val active: Boolean,
    )

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
            startFresh: Boolean,
            onToppedOut: (runId: Long) -> Unit,
        ): FallingBlocksComponent
    }
}
