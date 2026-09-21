package ge.yet.game.fallingblocks.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.result.FallingBlocksResultSnapshot
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RootComponentTest {
    @Test
    fun `top out opens one content only result and consumes back`() {
        val setup = build()

        setup.playing().topOut()
        setup.playing().topOut()

        assertEquals(2, setup.root.stack.value.items.size)
        val result = assertIs<RootComponent.Child.Result>(setup.root.stack.value.active.instance)
        assertEquals(100L, result.component.model.value.snapshot.score)
        assertEquals(MiniAppFrameMode.ContentOnly, setup.root.frameMode.value)
        assertTrue(setup.root.handleBack())
        assertEquals(2, setup.root.stack.value.items.size)
        setup.destroy()
    }

    @Test
    fun `successful continue returns to retained playing child`() {
        val setup = build()
        val original = setup.playing()
        original.topOut()

        setup.result().continueRequested()

        assertEquals(1, setup.root.stack.value.items.size)
        assertSame(original, setup.playing())
        assertEquals(GamePhase.PLAYING, original.model.value.game?.phase)
        assertEquals(MiniAppFrameMode.Standard, setup.root.frameMode.value)
        setup.destroy()
    }

    @Test
    fun `new game replaces stack with a fresh playing child`() {
        val setup = build()
        val original = setup.playing()
        original.topOut()

        setup.result().newGameRequested()

        assertEquals(1, setup.root.stack.value.items.size)
        assertNotSame(original, setup.playing())
        assertEquals(listOf(false, true), setup.gameFactory.startFreshRequests)
        setup.destroy()
    }

    private fun build(): Setup {
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val gameFactory = RecordingGameFactory()
        val resultFactory = RecordingResultFactory()
        return Setup(
            lifecycle = lifecycle,
            gameFactory = gameFactory,
            root = DefaultRootComponent(
                componentContext = lifecycle.componentContext,
                gameFactory = gameFactory,
                resultFactory = resultFactory,
            ),
        )
    }

    private class Setup(
        private val lifecycle: MiniAppLifecycleHarness,
        val gameFactory: RecordingGameFactory,
        val root: RootComponent,
    ) {
        fun playing(): FakePlaying = root.stack.value.items
            .firstNotNullOf { (it.instance as? RootComponent.Child.Playing)?.component }
            as FakePlaying

        fun result(): FakeResult = assertIs<RootComponent.Child.Result>(
            root.stack.value.active.instance,
        ).component as FakeResult

        fun destroy() = lifecycle.destroy()
    }

    private class RecordingGameFactory : FallingBlocksComponent.Factory {
        val startFreshRequests = mutableListOf<Boolean>()

        override fun create(
            componentContext: ComponentContext,
            startFresh: Boolean,
            onToppedOut: (Long) -> Unit,
        ): FallingBlocksComponent {
            startFreshRequests += startFresh
            return FakePlaying(onToppedOut)
        }
    }

    private class FakePlaying(
        private val onToppedOut: (Long) -> Unit,
    ) : FallingBlocksComponent {
        private val mutableModel = MutableValue(
            FallingBlocksComponent.Model(
                game = gameFixture(score = 100L),
                loading = false,
                tutorialSeen = true,
                tutorialProgress = null,
                bestScore = 200L,
                active = true,
            ),
        )
        override val model: Value<FallingBlocksComponent.Model> = mutableModel

        fun topOut() {
            val terminal = requireNotNull(mutableModel.value.game).copy(phase = GamePhase.TERMINAL)
            mutableModel.value = mutableModel.value.copy(game = terminal)
            onToppedOut(terminal.runId)
        }

        override fun revive() {
            val game = requireNotNull(mutableModel.value.game)
            mutableModel.value = mutableModel.value.copy(game = game.copy(phase = GamePhase.PLAYING))
        }

        override fun rotate() = Unit
        override fun move(cells: Int) = Unit
        override fun softDrop(cells: Int) = Unit
        override fun hardDrop() = Unit
        override fun newGame() = Unit
    }

    private class RecordingResultFactory : ResultComponent.Factory {
        override fun create(
            componentContext: ComponentContext,
            snapshot: FallingBlocksResultSnapshot,
            canContinue: Boolean,
            onContinueRequested: () -> Unit,
            onNewGameRequested: () -> Unit,
        ): ResultComponent = FakeResult(snapshot, canContinue, onContinueRequested, onNewGameRequested)
    }

    private class FakeResult(
        snapshot: FallingBlocksResultSnapshot,
        canContinue: Boolean,
        private val onContinueRequested: () -> Unit,
        private val onNewGameRequested: () -> Unit,
    ) : ResultComponent {
        override val model: Value<ResultComponent.Model> = MutableValue(
            ResultComponent.Model(snapshot, canContinue, if (canContinue) 5 else 0),
        )

        fun continueRequested() = onContinueRequested()
        fun newGameRequested() = onNewGameRequested()
        override fun onPrimaryClicked(requestContinue: (onApproved: () -> Unit) -> Unit) = Unit
        override fun onContinueFailed() = Unit
    }
}
