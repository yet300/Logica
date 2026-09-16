package ge.yet.game.twentyfortyeight.session

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.miniapp.metro.MiniAppSessionScope
import ge.yet.game.twentyfortyeight.component.root.RootComponent
import ge.yet.game.twentyfortyeight.domain.model.ResultSnapshot
import ge.yet.game.twentyfortyeight.component.playing.store.AnnouncementFact
import ge.yet.game.twentyfortyeight.component.playing.store.FocusTarget
import ge.yet.game.twentyfortyeight.component.playing.store.UiErrorCode

// Mutable session-owned effect/navigation hub: one instance per session.
@SingleIn(MiniAppSessionScope::class)
@Inject
internal class TwentyFortyEightSessionPorts : SessionNavigation, SessionUiEffects {
    private var navigateToResult: ((ResultSnapshot) -> Unit)? = null
    private var onNewGameCommitted: ((Long) -> Unit)? = null
    private val mutableEffect = MutableValue(RootComponent.EffectState())
    private val effectIds = EffectIdAllocator()

    val effect: Value<RootComponent.EffectState> = mutableEffect

    fun bind(
        navigateToResult: (ResultSnapshot) -> Unit,
        onNewGameCommitted: (Long) -> Unit,
    ) {
        this.navigateToResult = navigateToResult
        this.onNewGameCommitted = onNewGameCommitted
    }

    override fun navigateToResult(snapshot: ResultSnapshot) {
        checkNotNull(navigateToResult)(snapshot)
    }

    override fun onNewGameCommitted(runOrdinal: Long) {
        checkNotNull(onNewGameCommitted)(runOrdinal)
    }

    override fun announce(fact: AnnouncementFact) =
        publish { id -> RootComponent.Effect.Announcement(id, fact) }

    override fun requestFocus(target: FocusTarget) =
        publish { id -> RootComponent.Effect.Focus(id, target) }

    override fun showError(code: UiErrorCode) =
        publish { id -> RootComponent.Effect.Error(id, code) }

    fun consumeEffect(effectId: Long) {
        val pending = mutableEffect.value.effects
        if (pending.firstOrNull()?.id != effectId) return
        mutableEffect.value = RootComponent.EffectState(pending.drop(1))
    }

    private inline fun publish(
        createEffect: (Long) -> RootComponent.Effect,
    ) {
        val pending = mutableEffect.value.effects
        check(pending.size < RootComponent.MaxPendingEffects) {
            "Pending UI effect capacity exceeded"
        }
        val effect = createEffect(effectIds.next())
        mutableEffect.value = RootComponent.EffectState(pending + effect)
    }
}

internal class EffectIdAllocator(initial: Long = 1L) {
    private var nextId = initial.also { require(it > 0L) }
    private var exhausted = false

    fun next(): Long {
        check(!exhausted) { "Effect ID space exhausted" }
        val allocated = nextId
        if (allocated == Long.MAX_VALUE) {
            exhausted = true
        } else {
            nextId = allocated + 1L
        }
        return allocated
    }
}
