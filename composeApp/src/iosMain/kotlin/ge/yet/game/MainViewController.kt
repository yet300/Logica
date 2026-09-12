package ge.yet.game

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import ge.yet.game.feature.root.RootComponent
import ge.yet.game.screen.App
import ge.yet.game.screen.miniapp.LocalSystemChromeReporter
import ge.yet.game.screen.miniapp.shouldUseDarkSystemIcons

@OptIn(ExperimentalDecomposeApi::class)
fun MainViewController(
    root: RootComponent,
    backDispatcher: BackDispatcher,
    onSystemChromeChanged: (darkIcons: Boolean) -> Unit,
) = ComposeUIViewController {
    CompositionLocalProvider(
        LocalSystemChromeReporter provides { background ->
            onSystemChromeChanged(shouldUseDarkSystemIcons(background))
        },
    ) {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = { _, _ -> },
        ) {
            App(root)
        }
    }
}
