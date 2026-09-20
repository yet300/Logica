package ge.yet.game.fallingblocks.component.result

import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultResultComponentTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `countdown pauses outside active visibility`() = runTest {
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val visibility = MutableMiniAppVisibilitySource(MiniAppVisibility.OBSCURED)
        val component = DefaultResultComponent(
            lifecycle.componentContext, 10, 20, true, visibility, {}, {},
        )
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(5, component.model.value.continueSecondsRemaining)

        visibility.set(MiniAppVisibility.ACTIVE)
        advanceTimeBy(5_000)
        runCurrent()
        assertFalse(component.model.value.isContinuePhase)
        lifecycle.destroy()
    }

    @Test
    fun `primary action can only be claimed once`() = runTest {
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        var requests = 0
        val component = DefaultResultComponent(
            lifecycle.componentContext, 10, 20, true, MutableMiniAppVisibilitySource(), {}, {},
        )

        component.onPrimaryClicked { requests += 1 }
        component.onPrimaryClicked { requests += 1 }

        assertEquals(1, requests)
        lifecycle.destroy()
    }
}
