package ge.yet.game.fruitmerge.component.session

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.fruitmerge.TestFruitMergeRules
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.component.game.DefaultFruitMergeComponentFactory
import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.game.TutorialStep
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStore
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStoreFactory
import ge.yet.game.fruitmerge.component.result.DefaultFruitMergeResultComponentFactory
import ge.yet.game.fruitmerge.data.FruitMergePersistence
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.RunPhase
import ge.yet.game.fruitmerge.domain.model.Vec2
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.miniapp.testkit.MutableMiniAppStorage
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import ge.yet.game.miniapp.testkit.NoopMiniAppAudio
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FruitMergeSessionComponentTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `terminal restore opens the result child with a detached snapshot`() = runTest {
        val harness = sessionHarness(
            initial = FruitMergeState(
                score = 900L,
                bestScore = 1_000L,
                runOrdinal = 7L,
                phase = RunPhase.RESULT,
            ),
        )
        val component = harness.component

        val result = assertIs<FruitMergeSessionComponent.Child.Result>(component.stack.value.active.instance)
        assertEquals(900L, result.component.model.value.snapshot.score)
        assertEquals(7L, result.component.model.value.snapshot.runOrdinal)
        assertEquals(MiniAppFrameMode.ContentOnly, component.frameMode.value)
        assertEquals(false, component.handleBack())
        harness.lifecycle.destroy()
    }

    @Test
    fun `new game from result starts a fresh run on a new playing child`() = runTest {
        val harness = sessionHarness(
            initial = FruitMergeState(runOrdinal = 7L, phase = RunPhase.RESULT),
        )
        val component = harness.component
        val firstGame = component.game
        assertIs<FruitMergeSessionComponent.Child.Result>(component.stack.value.active.instance)

        val result = assertIs<FruitMergeSessionComponent.Child.Result>(component.stack.value.active.instance)
        result.component.onNewGame()
        advanceUntilIdle()

        val secondGame = component.game
        assertIs<FruitMergeSessionComponent.Child.Playing>(component.stack.value.active.instance)
        assertNotSame(firstGame, secondGame)
        assertEquals(RunPhase.PLAYING, secondGame.model.value.game.phase)
        assertEquals(8L, secondGame.model.value.game.runOrdinal)
        assertEquals(MiniAppFrameMode.Standard, component.frameMode.value)
        harness.lifecycle.destroy()
    }

    @Test
    fun `tutorial follows an accepted drop merge and finite trait reveal`() = runTest {
        val harness = sessionHarness(initial = FruitMergeState())
        val component = harness.component
        advanceUntilIdle()
        val playing = component.game

        assertIs<TutorialStep.Gesture>(playing.model.value.tutorialStep)
        playing.drop(dragged = false)
        advanceUntilIdle()
        assertIs<TutorialStep.Merge>(playing.model.value.tutorialStep)

        component.gameComponent.onStoreLabel(
            FruitMergeStore.Label.MergeResolved(FruitLevel.RASPBERRY, Vec2(0.5f, 0.6f)),
        )
        assertIs<TutorialStep.Traits>(playing.model.value.tutorialStep)
        playing.completeTutorial()
        advanceUntilIdle()

        kotlin.test.assertEquals(null, playing.model.value.tutorialStep)
        kotlin.test.assertTrue(FruitMergePersistence(harness.storage).isTutorialSeen())
        harness.lifecycle.destroy()
    }

    @Test
    fun `active shake blocks another free action at the component boundary`() = runTest {
        val rules = TestFruitMergeRules()
        val harness = sessionHarness(
            initial = FruitMergeState(
                bodies = listOf(
                    ge.yet.game.fruitmerge.domain.model.FruitBody(
                        id = 1L,
                        level = ge.yet.game.fruitmerge.domain.model.FruitLevel.APPLE,
                        position = ge.yet.game.fruitmerge.domain.model.Vec2(0.5f, 0.8f),
                    ),
                ),
                nextBodyId = 2L,
            ),
            rules = rules,
        )
        val component = harness.component
        advanceUntilIdle()
        val playing = component.game

        assertNull(playing.requestShakeGate())
        advanceUntilIdle()
        val active = playing.model.value.game
        assertNull(playing.requestShakeGate())

        assertEquals(FruitMergeState.FREE_SHAKE_COUNT - 1, active.freeShakes)
        assertEquals(active, playing.model.value.game)
        assertEquals(1, rules.shakeCalls)
        harness.lifecycle.destroy()
    }

    @Test
    fun `visible committed labels bridge to bounded presentation events`() = runTest {
        val harness = sessionHarness(initial = FruitMergeState())
        val component = harness.component
        advanceUntilIdle()
        assertTrue(component.game.model.value.visible)
        val collector = async(start = CoroutineStart.UNDISPATCHED) {
            component.game.presentationEvents.take(4).toList()
        }

        component.gameComponent.onStoreLabel(FruitMergeStore.Label.FruitLanded(FruitLevel.LIME, Vec2(0.2f, 0.8f)))
        component.gameComponent.onStoreLabel(FruitMergeStore.Label.MergeResolved(FruitLevel.MANDARIN, Vec2(0.4f, 0.7f)))
        component.gameComponent.onStoreLabel(FruitMergeStore.Label.ClearApplied(FruitLevel.APPLE, Vec2(0.6f, 0.7f)))
        component.gameComponent.onStoreLabel(FruitMergeStore.Label.ShakePulse(3))
        val received = collector.await()

        assertEquals(
            listOf(
                FruitMergeComponent.PresentationEvent.Landing(FruitLevel.LIME, Vec2(0.2f, 0.8f)),
                FruitMergeComponent.PresentationEvent.Merge(FruitLevel.MANDARIN, Vec2(0.4f, 0.7f)),
                FruitMergeComponent.PresentationEvent.Clear(FruitLevel.APPLE, Vec2(0.6f, 0.7f)),
                FruitMergeComponent.PresentationEvent.ShakePulse(3),
            ),
            received,
        )
        harness.lifecycle.destroy()
    }

    @Test
    fun `paid actions complete through the playing child`() = runTest {
        val harness = sessionHarness(initial = FruitMergeState(freeClears = 0))
        val component = harness.component
        advanceUntilIdle()

        val token = kotlin.test.assertNotNull(component.game.requestClearGate())
        component.completePaidAction(token)
        advanceUntilIdle()

        assertEquals(
            ge.yet.game.fruitmerge.domain.model.TargetingMode.CLEAR,
            component.game.model.value.game.targetingMode,
        )
        harness.lifecycle.destroy()
    }

    private suspend fun TestScope.sessionHarness(
        initial: FruitMergeState,
        rules: TestFruitMergeRules = TestFruitMergeRules(),
    ): Harness {
        val storage = MutableMiniAppStorage()
        val persistence = FruitMergePersistence(storage)
        persistence.checkpoint(initial)
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val visibility = MutableMiniAppVisibilitySource()
        val component = DefaultFruitMergeSessionComponentFactory(
            gameFactory = DefaultFruitMergeComponentFactory(
                gameStoreFactory = FruitMergeStoreFactory(
                    storeFactory = DefaultStoreFactory(),
                    rules = rules,
                    snapshotLoader = persistence,
                    commitWriter = persistence,
                ),
                audio = FruitMergeAudioAdapter(NoopMiniAppAudio),
                tutorial = persistence,
                visibility = visibility,
            ),
            resultFactory = DefaultFruitMergeResultComponentFactory(),
        ).create(lifecycle.componentContext) as DefaultFruitMergeSessionComponent
        advanceUntilIdle()
        return Harness(component, lifecycle, storage)
    }

    private data class Harness(
        val component: DefaultFruitMergeSessionComponent,
        val lifecycle: MiniAppLifecycleHarness,
        val storage: MutableMiniAppStorage,
    )
}
