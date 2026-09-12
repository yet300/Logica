package ge.yet.game.screen.root

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.feature.catalog.ui.CatalogContent
import ge.yet.game.feature.root.RootComponent
import ge.yet.game.monetization.ads.rememberBannerContent
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.screen.miniapp.LocalSystemChromeReporter
import ge.yet.game.screen.miniapp.MiniAppFrame
import ge.yet.game.screen.miniapp.MiniAppUnavailableContent
import ge.yet.game.screen.miniapp.opaqueBackground
import ge.yet.game.utils.cupertinoPredictiveBackAnimation

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    val childStack by component.stack.subscribeAsState()
    Children(
        modifier = modifier.fillMaxSize(),
        stack = childStack,
        animation = cupertinoPredictiveBackAnimation(
            backHandler = component.backHandler,
            onBack = component::onBackClicked,
        ),
    ) { child ->
        RootChildContent(
            child = child.instance,
            onBack = component::onBackClicked,
            onSettings = component::onSettingsClicked,
            isActive = childStack.active.instance == child.instance,
        )
    }
    RootSheet(component = component)
}

@Composable
internal fun RootChildContent(
    child: RootComponent.Child,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    isActive: Boolean = true,
) {
    when (child) {
        is RootComponent.Child.Catalog -> {
            val background = MaterialTheme.colorScheme.background
            val chromeReporter = LocalSystemChromeReporter.current
            SideEffect {
                if (isActive) chromeReporter?.invoke(background)
            }
            CatalogContent(
                component = child.component,
                modifier = modifier.fillMaxSize(),
            )
        }

        is RootComponent.Child.RunningMiniApp -> {
            val session = (child.state as? RootComponent.MiniAppState.Content)?.session
            val frameMode = session?.frameMode?.subscribeAsState()?.value
                ?: MiniAppFrameMode.Standard
            // Resolved under the outer Logica theme, before installing the session theme.
            val baseBackground = MaterialTheme.colorScheme.background
            val declaredScheme = session?.colorScheme() ?: MaterialTheme.colorScheme
            val scheme = declaredScheme.copy(
                background = opaqueBackground(declaredScheme.background, baseBackground),
            )
            val bannerContent = rememberBannerContent()
            val chromeReporter = LocalSystemChromeReporter.current
            // Only the active route drives native system-icon appearance; outgoing
            // entries during transitions must not race it.
            SideEffect {
                if (isActive) chromeReporter?.invoke(scheme.background)
            }

            MaterialTheme(colorScheme = scheme) {
                MiniAppFrame(
                    onBack = onBack,
                    onSettings = onSettings,
                    frameMode = frameMode,
                    modifier = modifier,
                    background = session?.let { currentSession ->
                        { backgroundModifier -> currentSession.Background(backgroundModifier) }
                    },
                    topBar = { session?.TopBarContent() },
                    bottomBar = bottomBar ?: bannerContent,
                ) { viewport ->
                    when (val state = child.state) {
                        is RootComponent.MiniAppState.Content -> state.session.Content(viewport)
                        is RootComponent.MiniAppState.Unavailable -> MiniAppUnavailableContent(
                            id = state.id,
                            onBack = onBack,
                            modifier = viewport,
                        )
                    }
                }
            }
        }
    }
}
