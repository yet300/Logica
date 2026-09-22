package ge.yet.game.screen.settings.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import funfolio.composeapp.generated.resources.Res
import funfolio.composeapp.generated.resources.dark_theme
import funfolio.composeapp.generated.resources.dark_theme_subtitle
import funfolio.composeapp.generated.resources.more
import funfolio.composeapp.generated.resources.more_subtitle
import funfolio.composeapp.generated.resources.music
import funfolio.composeapp.generated.resources.music_subtitle
import funfolio.composeapp.generated.resources.settings
import funfolio.composeapp.generated.resources.sfx
import funfolio.composeapp.generated.resources.sfx_subtitle
import funfolio.composeapp.generated.resources.vibration
import funfolio.composeapp.generated.resources.vibration_subtitle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.feature.settings.main.MainSettingsComponent
import ge.yet.game.uikit.components.icon.DarkMode
import ge.yet.game.uikit.components.icon.NotificationsActive
import ge.yet.game.uikit.components.icon.Settings
import ge.yet.game.uikit.components.icon.Vibration
import ge.yet.game.screen.settings.SettingsDivider
import ge.yet.game.screen.settings.SettingsLinkRow
import ge.yet.game.screen.settings.SettingsToggleRow
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainSettingsContent(component: MainSettingsComponent) {
    val model by component.model.subscribeAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(20.dp))

        SettingsToggleRow(
            icon = NotificationsActive,
            title = stringResource(Res.string.music),
            subtitle = stringResource(Res.string.music_subtitle),
            checked = model.musicEnabled,
            onCheckedChange = component::onMusicToggled,
        )

        SettingsDivider()

        SettingsToggleRow(
            icon = NotificationsActive,
            title = stringResource(Res.string.sfx),
            subtitle = stringResource(Res.string.sfx_subtitle),
            checked = model.sfxEnabled,
            onCheckedChange = component::onSfxToggled,
        )

        SettingsDivider()

        SettingsToggleRow(
            icon = Vibration,
            title = stringResource(Res.string.vibration),
            subtitle = stringResource(Res.string.vibration_subtitle),
            checked = model.vibrationEnabled,
            onCheckedChange = component::onVibrationToggled,
        )

        SettingsDivider()

        SettingsToggleRow(
            icon = DarkMode,
            title = stringResource(Res.string.dark_theme),
            subtitle = stringResource(Res.string.dark_theme_subtitle),
            checked = model.darkTheme,
            onCheckedChange = component::onDarkThemeToggled,
        )

        SettingsDivider()

        SettingsLinkRow(
            icon = Settings,
            title = stringResource(Res.string.more),
            subtitle = stringResource(Res.string.more_subtitle),
            external = false,
            onClick = component::onMoreClicked,
        )

        Spacer(Modifier.height(28.dp))
    }
}
