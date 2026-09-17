package ge.yet.game.miniapp.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import ge.yet.game.miniapp.api.MiniAppCategoryId
import ge.yet.game.miniapp.api.MiniAppId
import ge.yet.game.miniapp.api.MiniAppSessionHost
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource

@OptIn(InternalResourceApi::class)
class MiniAppContractsTest {

    @Test
    fun `sessions use the standard host frame by default`() {
        val session = object : MiniAppSession {
            @Composable
            override fun Content(modifier: Modifier) = Unit
        }

        assertEquals(MiniAppFrameMode.Standard, session.frameMode.value)
    }

    @Test
    fun `sessions do not consume Back by default`() {
        val session = object : MiniAppSession {
            @Composable
            override fun Content(modifier: Modifier) = Unit
        }

        assertFalse(session.handleBack())
    }

    @Test
    fun `reading plugin metadata does not create a session`() {
        val plugin = FakePlugin()

        assertEquals(MiniAppId("game.fake"), plugin.manifest.id)

        assertEquals(1, plugin.manifestReadCount)
        assertEquals(0, plugin.sessionCreateCount)
    }

    @Test
    fun `interstitial gate request completes immediately when no ad will show`() {
        var completed = false

        MiniAppAdGate(willShowAd = false) { onComplete -> onComplete() }
            .request { completed = true }

        assertTrue(completed)
    }

    @Test
    fun `ad kinds stay generic without game-specific subtypes`() {
        assertEquals(
            MiniAppAdKind.Fullscreen("continue_after_game_over"),
            MiniAppAdKind.Fullscreen("continue_after_game_over"),
        )
        assertEquals(MiniAppAdKind.Banner, MiniAppAdKind.Banner)
    }

    @Test
    fun `sessions opt out of the host banner by default`() {
        val session = object : MiniAppSession {
            @Composable
            override fun Content(modifier: Modifier) = Unit
        }

        assertFalse(session.wantsBanner)
    }

    @Test
    fun `delegating session forwards frame mode banner opt-in and back`() {
        var backCalls = 0
        val session = object : DelegatingMiniAppSession(
            frameMode = MutableValue(MiniAppFrameMode.ContentOnly),
            wantsBanner = true,
            onBack = { backCalls += 1; true },
        ) {
            @Composable
            override fun Content(modifier: Modifier) = Unit
        }

        assertEquals(MiniAppFrameMode.ContentOnly, session.frameMode.value)
        assertTrue(session.wantsBanner)
        assertTrue(session.handleBack())
        assertEquals(1, backCalls)
    }

    @Test
    fun `delegating session does not consume Back by default`() {
        val session = object : DelegatingMiniAppSession(
            frameMode = MutableValue(MiniAppFrameMode.Standard),
            wantsBanner = false,
        ) {
            @Composable
            override fun Content(modifier: Modifier) = Unit
        }

        assertFalse(session.wantsBanner)
        assertFalse(session.handleBack())
    }

    private class FakePlugin : MiniAppPlugin {
        var manifestReadCount = 0
        var sessionCreateCount = 0

        override val manifest: MiniAppManifest
            get() {
                manifestReadCount += 1
                return MiniAppManifest(
                    id = MiniAppId("game.fake"),
                    title = StringResource("fake_title", "fake_title", emptySet()),
                    description = StringResource("fake_description", "fake_description", emptySet()),
                    icon = DrawableResource("fake_icon", emptySet()),
                    category = MiniAppCategoryId("game"),
                    sortPriority = 0,
                )
            }

        override fun createSession(context: MiniAppSessionContext): MiniAppSession {
            sessionCreateCount += 1
            error("Session creation is outside metadata access")
        }
    }
}
