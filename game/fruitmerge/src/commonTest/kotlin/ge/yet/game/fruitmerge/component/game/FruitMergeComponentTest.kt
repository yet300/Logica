package ge.yet.game.fruitmerge.component.game

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.fruitmerge.TestFruitMergeRules
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStore
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStoreFactory
import ge.yet.game.fruitmerge.component.result.FruitMergeResultSnapshot
import ge.yet.game.fruitmerge.data.FruitMergePersistence
import ge.yet.game.fruitmerge.domain.model.FruitBody
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.TargetingMode
import ge.yet.game.fruitmerge.domain.model.Vec2
import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.miniapp.testkit.MutableMiniAppStorage
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import ge.yet.game.miniapp.testkit.NoopMiniAppAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FruitMergeComponentTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stale paid action token cannot mutate a new run`() = runTest {
        val harness = componentHarness(FruitMergeState(freeClears = 0))
        val token = assertNotNull(harness.component.requestClearGate())

        harness.component.store.accept(FruitMergeStore.Intent.NewGame)
        advanceUntilIdle()
        harness.component.completePaidAction(token)

        assertEquals(TargetingMode.NONE, harness.component.model.value.game.targetingMode)
        harness.lifecycle.destroy()
        harness.component.store.dispose()
    }

    @Test
    fun `duplicate completion applies a paid action once`() = runTest {
        val rules = TestFruitMergeRules()
        val harness = componentHarness(
            initial = FruitMergeState(
                bodies = listOf(FruitBody(1, FruitLevel.APPLE, Vec2(0.5f, 0.8f))),
                nextBodyId = 2,
                freeShakes = 0,
            ),
            rules = rules,
        )
        val token = assertNotNull(harness.component.requestShakeGate())

        harness.component.completePaidAction(token)
        harness.component.completePaidAction(token)

        assertEquals(1, rules.paidShakeCalls)
        harness.lifecycle.destroy()
        harness.component.store.dispose()
    }

    @Test
    fun `terminal restore reports completion with a detached snapshot`() = runTest {
        val completed = mutableListOf<FruitMergeResultSnapshot>()
        val harness = componentHarness(
            initial = FruitMergeState(
                bodies = listOf(FruitBody(1, FruitLevel.APPLE, Vec2(0.5f, 0.8f))),
                nextBodyId = 2,
                score = 900L,
                bestScore = 1_000L,
                runOrdinal = 7L,
                phase = ge.yet.game.fruitmerge.domain.model.RunPhase.RESULT,
            ),
            onGameCompleted = completed::add,
        )

        assertEquals(1, completed.size)
        assertEquals(900L, completed.single().score)
        assertEquals(7L, completed.single().runOrdinal)
        harness.lifecycle.destroy()
        harness.component.store.dispose()
    }

    @Test
    fun `fresh store from the factory starts a playing run`() = runTest {
        val harness = componentHarness(
            initial = FruitMergeState(
                score = 900L,
                bestScore = 1_000L,
                runOrdinal = 7L,
                phase = ge.yet.game.fruitmerge.domain.model.RunPhase.RESULT,
            ),
            isNewGame = true,
        )

        assertNull(harness.completions.singleOrNull())
        assertEquals(ge.yet.game.fruitmerge.domain.model.RunPhase.PLAYING, harness.component.model.value.game.phase)
        assertEquals(1_000L, harness.component.model.value.game.bestScore)
        harness.lifecycle.destroy()
        harness.component.store.dispose()
    }

    private suspend fun TestScope.componentHarness(
        initial: FruitMergeState,
        rules: TestFruitMergeRules = TestFruitMergeRules(),
        isNewGame: Boolean = false,
        onGameCompleted: (FruitMergeResultSnapshot) -> Unit = {},
    ): Harness {
        val storage = MutableMiniAppStorage()
        val persistence = FruitMergePersistence(storage)
        persistence.checkpoint(initial)
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val completions = mutableListOf<FruitMergeResultSnapshot>()
        val component = DefaultFruitMergeComponentFactory(
            gameStoreFactory = FruitMergeStoreFactory(
                storeFactory = DefaultStoreFactory(),
                rules = rules,
                snapshotLoader = persistence,
                commitWriter = persistence,
            ),
            audio = FruitMergeAudioAdapter(NoopMiniAppAudio),
            tutorial = persistence,
            visibility = MutableMiniAppVisibilitySource(),
        ).create(
            componentContext = lifecycle.componentContext,
            isNewGame = isNewGame,
            onGameCompleted = {
                completions += it
                onGameCompleted(it)
            },
        ) as DefaultFruitMergeComponent
        advanceUntilIdle()
        return Harness(component, lifecycle, completions)
    }

    private data class Harness(
        val component: DefaultFruitMergeComponent,
        val lifecycle: MiniAppLifecycleHarness,
        val completions: List<FruitMergeResultSnapshot>,
    )
}
