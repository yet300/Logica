package ge.yet.game.fallingblocks

import dev.zacsweers.metro.createGraph
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.data.FallingBlocksPersistence
import ge.yet.game.fallingblocks.data.TUTORIAL_SEEN_KEY
import ge.yet.game.fallingblocks.di.InspectableFallingblocksAppGraph
import ge.yet.game.fallingblocks.di.InspectableFallingblocksSessionGraph
import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.audio.AudioCommandRejection
import ge.yet.game.miniapp.audio.AudioCommandResult
import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.AudioDuration
import ge.yet.game.miniapp.audio.AudioProgram
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.miniapp.testkit.MutableMiniAppStorage
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import ge.yet.game.miniapp.testkit.RecordingMiniAppSessionHost
import ge.yet.game.miniapp.testkit.TestMiniAppSessionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class FallingblocksLifecycleIntegrationTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `visibility suspends ticks and leaving active play checkpoints an exact session`() = runTest(dispatcher) {
        val app = createGraph<InspectableFallingblocksAppGraph>()
        val storage = MutableMiniAppStorage().also { it.putBoolean(TUTORIAL_SEEN_KEY, true) }
        val firstLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val visibility = MutableMiniAppVisibilitySource(MiniAppVisibility.OBSCURED)
        val first = app.sessionGraph(firstLifecycle, storage, visibility)
        advanceUntilIdle()

        val obscured = assertNotNull(first.component.playing().model.value.game)
        advanceTimeBy(1_600)
        runCurrent()
        assertEquals(obscured, first.component.playing().model.value.game)

        visibility.set(MiniAppVisibility.ACTIVE)
        runCurrent()
        advanceTimeBy(32)
        runCurrent()
        val ticking = assertNotNull(first.component.playing().model.value.game)
        assertNotEquals(obscured.gravityRemainingMillis, ticking.gravityRemainingMillis)

        first.component.playing().move(1)
        visibility.set(MiniAppVisibility.OBSCURED)
        advanceUntilIdle()
        val checkpoint = assertNotNull(first.component.playing().model.value.game)
        firstLifecycle.destroy()

        val secondLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val restored = app.sessionGraph(
            lifecycle = secondLifecycle,
            storage = storage,
            visibility = MutableMiniAppVisibilitySource(MiniAppVisibility.OBSCURED),
        )
        advanceUntilIdle()

        assertEquals(checkpoint, restored.component.playing().model.value.game)
        secondLifecycle.destroy()
    }

    @Test
    fun `destroyed result approval is stale and cannot mutate a successor`() = runTest(dispatcher) {
        val app = createGraph<InspectableFallingblocksAppGraph>()
        val storage = MutableMiniAppStorage()
        storage.putBoolean(TUTORIAL_SEEN_KEY, true)
        FallingBlocksPersistence(storage).write(terminalFixture())

        val firstLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val first = app.sessionGraph(
            firstLifecycle,
            storage,
            MutableMiniAppVisibilitySource(MiniAppVisibility.OBSCURED),
        )
        advanceUntilIdle()
        val staleResult = assertNotNull(first.component.result())
        var staleApproval: (() -> Unit)? = null
        staleResult.onPrimaryClicked { approve -> staleApproval = approve }
        firstLifecycle.destroy()

        val secondLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val second = app.sessionGraph(
            secondLifecycle,
            storage,
            MutableMiniAppVisibilitySource(MiniAppVisibility.OBSCURED),
        )
        advanceUntilIdle()
        val successor = second.component.playing().model.value.game

        assertNotNull(staleApproval).invoke()
        advanceUntilIdle()

        assertEquals(successor, second.component.playing().model.value.game)
        secondLifecycle.destroy()
    }

    @Test
    fun `host rejected audio remains single shot across visibility transitions`() = runTest(dispatcher) {
        val app = createGraph<InspectableFallingblocksAppGraph>()
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val visibility = MutableMiniAppVisibilitySource(MiniAppVisibility.OBSCURED)
        val audio = VisibilityAwareAudio(visibility)
        val graph = app.sessionGraph(
            lifecycle = lifecycle,
            storage = MutableMiniAppStorage().also { it.putBoolean(TUTORIAL_SEEN_KEY, true) },
            visibility = visibility,
            audio = audio,
        )
        advanceUntilIdle()

        assertEquals(1, audio.musicAttempts)
        assertEquals(2, audio.rejectedAttempts) // music start and initial intensity control

        visibility.set(MiniAppVisibility.ACTIVE)
        runCurrent()
        graph.component.playing().rotate()
        runCurrent()
        visibility.set(MiniAppVisibility.OBSCURED)
        runCurrent()

        assertEquals(1, audio.musicAttempts)
        lifecycle.destroy()
    }

    private fun InspectableFallingblocksAppGraph.sessionGraph(
        lifecycle: MiniAppLifecycleHarness,
        storage: MutableMiniAppStorage,
        visibility: MutableMiniAppVisibilitySource,
        audio: MiniAppAudio = VisibilityAwareAudio(visibility),
    ): InspectableFallingblocksSessionGraph = factory.createInspectableFallingblocksSessionGraph(
        TestMiniAppSessionContext(
            componentContext = lifecycle.componentContext,
            visibility = visibility,
            host = RecordingMiniAppSessionHost(),
            storage = storage,
            audio = audio,
        ),
    ).also { it.component }

    private fun RootComponent.playing(): FallingBlocksComponent = stack.value.items
        .firstNotNullOf { (it.instance as? RootComponent.Child.Playing)?.component }

    private fun RootComponent.result(): ResultComponent? =
        (stack.value.active.instance as? RootComponent.Child.Result)?.component

    private class VisibilityAwareAudio(
        private val visibility: MutableMiniAppVisibilitySource,
    ) : MiniAppAudio {
        var musicAttempts = 0
        var rejectedAttempts = 0

        override fun playMusic(program: AudioProgram): AudioCommandResult {
            musicAttempts += 1
            return result()
        }

        override fun stopMusic(fadeOut: AudioDuration): AudioCommandResult = result()
        override fun playSfx(program: AudioProgram, name: SfxName): AudioCommandResult = result()
        override fun setControl(name: AudioControlName, value: Float): AudioCommandResult = result()

        private fun result(): AudioCommandResult = if (visibility.visibility.value == MiniAppVisibility.ACTIVE) {
            AudioCommandResult.Accepted
        } else {
            rejectedAttempts += 1
            AudioCommandResult.Rejected(AudioCommandRejection.PLAYBACK_SUPPRESSED)
        }
    }
}
