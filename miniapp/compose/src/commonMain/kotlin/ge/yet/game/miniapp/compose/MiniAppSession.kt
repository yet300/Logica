package ge.yet.game.miniapp.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.value.Value

enum class MiniAppFrameMode {
    Standard,
    ContentOnly,
}

private object StandardMiniAppFrameMode : Value<MiniAppFrameMode>() {
    override val value: MiniAppFrameMode = MiniAppFrameMode.Standard

    override fun subscribe(observer: (MiniAppFrameMode) -> Unit): Cancellation {
        observer(value)
        return Cancellation {}
    }
}

interface MiniAppSession {

    @Composable
    fun colorScheme(): ColorScheme = MaterialTheme.colorScheme

    val frameMode: Value<MiniAppFrameMode>
        get() = StandardMiniAppFrameMode

    /**
     * Whether the host may mount its banner under this session.
     *
     * This is only an opt-in declaration: rendering, sizing, safe-area handling
     * and the zero-space-when-empty policy stay host-owned. Defaults to false,
     * so games without ads stay banner-free without any extra work.
     */
    val wantsBanner: Boolean
        get() = false

    /**
     * Offers a Back press to the session before the host closes it.
     *
     * Return `true` only when the session performed internal navigation
     * (for example dismissing a Playing overlay) and the host must stay
     * on the running MiniApp. Return `false` to let the host close the
     * session and navigate to Catalog.
     *
     * In particular, a terminal Result screen must return `false`: the host
     * owns Back there. Returning `true` without internal navigation leaves
     * the session active, so session audio keeps playing and the catalog
     * stays locked because the runtime still treats the MiniApp as running.
     *
     * Canonical Playing/Result root:
     * ```
     * override fun handleBack(): Boolean =
     *     (stack.value.active.instance as? Child.Playing)
     *         ?.component?.handleBack()
     *         ?: false
     * ```
     */
    fun handleBack(): Boolean = false

    @Composable
    fun TopBarContent() = Unit

    @Composable
    fun Background(modifier: Modifier) = Unit

    @Composable
    fun Content(modifier: Modifier)
}
