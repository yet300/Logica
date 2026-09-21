package ge.yet.game.fallingblocks.component.game.store

import com.arkivanov.mvikotlin.core.store.Store
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.component.game.FallingBlocksVisualEvent
import ge.yet.game.fallingblocks.ui.tutorial.TutorialProgress

internal interface FallingBlocksStore : Store<
    FallingBlocksStore.Intent,
    FallingBlocksStore.State,
    FallingBlocksStore.Label,
    > {
    sealed interface Intent {
        data object Rotate : Intent
        data class Move(val cells: Int) : Intent
        data class SoftDrop(val cells: Int) : Intent
        data object HardDrop : Intent
        data class Frame(val elapsedMillis: Int) : Intent
        data object Revive : Intent
        data object NewGame : Intent
    }

    data class State(
        val game: FallingBlocksState? = null,
        val loading: Boolean = true,
        val tutorialSeen: Boolean = false,
        val tutorialProgress: TutorialProgress? = null,
        val bestScore: Long = 0,
        val active: Boolean = true,
        val visualEvent: FallingBlocksVisualEvent? = null,
        val nextVisualEventId: Long = 1L,
    )

    sealed interface Label {
        data class ToppedOut(val runId: Long) : Label
    }
}
