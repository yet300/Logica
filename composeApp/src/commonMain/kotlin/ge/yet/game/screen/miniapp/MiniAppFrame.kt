package ge.yet.game.screen.miniapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import logica.composeapp.generated.resources.Res
import logica.composeapp.generated.resources.cd_back
import logica.composeapp.generated.resources.cd_settings
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.uikit.components.button.IconCircleButton
import ge.yet.game.uikit.components.icon.ArrowBack
import ge.yet.game.uikit.components.icon.Settings
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppFrame(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    frameMode: MiniAppFrameMode = MiniAppFrameMode.Standard,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    modifier: Modifier = Modifier,
    background: (@Composable (Modifier) -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("miniapp_frame"),
    ) {
        // Resolved semantic base, always opaque and always under decorative art.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        background?.invoke(Modifier.fillMaxSize())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = contentWindowInsets,
            topBar = {
                AnimatedVisibility(
                    visible = frameMode == MiniAppFrameMode.Standard,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    CenterAlignedTopAppBar(
                        title = topBar,
                        navigationIcon = {
                            IconCircleButton(
                                icon = ArrowBack,
                                contentDescription = stringResource(Res.string.cd_back),
                                onClick = onBack,
                                modifier = Modifier.testTag("miniapp_back_control"),
                            )
                        },
                        actions = {
                            IconCircleButton(
                                icon = Settings,
                                contentDescription = stringResource(Res.string.cd_settings),
                                onClick = onSettings,
                                modifier = Modifier.testTag("miniapp_settings_control"),
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                }
            },
            bottomBar = {
                if (bottomBar != null) {
                    // No reserved height: measures only actually mounted banner content.
                    // Null banner mounts no container and consumes zero ad layout space.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                contentWindowInsets.only(
                                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                                ),
                            )
                            .testTag("miniapp_banner_container"),
                        contentAlignment = Alignment.Center,
                    ) {
                        bottomBar()
                    }
                }
            },
        ) { padding ->
            content(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize(),
            )
        }
    }
}
