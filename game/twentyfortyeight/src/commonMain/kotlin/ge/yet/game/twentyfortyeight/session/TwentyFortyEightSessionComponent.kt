package ge.yet.game.twentyfortyeight.session

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.twentyfortyeight.component.playing.PlayingComponent
import ge.yet.game.twentyfortyeight.component.result.ResultComponent
import ge.yet.game.twentyfortyeight.component.playing.store.AnnouncementFact
import ge.yet.game.twentyfortyeight.component.playing.store.FocusTarget
import ge.yet.game.twentyfortyeight.component.playing.store.UiErrorCode

internal interface TwentyFortyEightSessionComponent {
    val stack: Value<ChildStack<*, Child>>
    val frameMode: Value<MiniAppFrameMode>
    val effect: Value<EffectState>
    fun onEffectConsumed(effectId: Long)
    fun handleBack(): Boolean

    data class EffectState(val effects: List<Effect> = emptyList()) {
        init {
            require(effects.size <= MaxPendingEffects) {
                "Too many pending UI effects: ${effects.size}"
            }
            require(effects.map { it.id }.distinct().size == effects.size) {
                "Pending UI effect IDs must be unique"
            }
        }

        val effect: Effect?
            get() = effects.firstOrNull()
    }

    sealed interface Effect {
        val id: Long

        data class Announcement(override val id: Long, val fact: AnnouncementFact) : Effect
        data class Focus(override val id: Long, val target: FocusTarget) : Effect
        data class Error(override val id: Long, val code: UiErrorCode) : Effect
    }

    sealed interface Child {
        class Playing(val component: PlayingComponent) : Child
        class Result(val component: ResultComponent) : Child
    }

    companion object {
        const val MaxPendingEffects: Int = 16
    }

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
        ): TwentyFortyEightSessionComponent
    }
}
