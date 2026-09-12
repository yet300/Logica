package ge.yet.blockblast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.retainedComponent
import com.google.firebase.Firebase
import com.google.firebase.initialize
import ge.yet.game.feature.root.RootComponent
import ge.yet.game.screen.App
import ge.yet.game.screen.miniapp.LocalSystemChromeReporter
import ge.yet.game.screen.miniapp.shouldUseDarkSystemIcons

class MainActivity : ComponentActivity() {
    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        Firebase.initialize(this)

        val appGraph = (application as BlockBlastApp).appGraph
        rootComponent = retainedComponent(
            key = "LogicaRoot",
            handleBackButton = true,
            isStateSavingAllowed = { true },
        ) { componentContext ->
            appGraph.rootFactory.create(componentContext = componentContext)
        }

        setContent {
            // Single native appearance owner: the active session's resolved semantic
            // background selects dark/light system icons while enableEdgeToEdge keeps
            // both system bars transparent over the host-rendered content.
            CompositionLocalProvider(
                LocalSystemChromeReporter provides { background ->
                    val darkIcons = shouldUseDarkSystemIcons(background)
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = darkIcons
                        isAppearanceLightNavigationBars = darkIcons
                    }
                },
            ) {
                App(rootComponent = rootComponent)
            }
        }
    }
}
