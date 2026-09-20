package ge.yet.game.fallingblocks.ui.screen.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.uikit.theme.LogicaTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FallingBlocksScreenTest {
    @Test
    fun `board remains centered while hud and five piece preview are overlays`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = false) {
                Box(Modifier.size(400.dp, 800.dp).testTag("falling_blocks_test_host")) {
                    FallingBlocksScreen(FakeComponent())
                }
            }
        }

        val bounds = onNodeWithTag(FallingBlocksTestTags.Board).getUnclippedBoundsInRoot()
        val host = onNodeWithTag("falling_blocks_test_host").getUnclippedBoundsInRoot()
        assertEquals(
            ((host.left + host.right) / 2).value,
            ((bounds.left + bounds.right) / 2).value,
            absoluteTolerance = 0.5f,
        )
        assertEquals(
            ((host.top + host.bottom) / 2).value,
            ((bounds.top + bounds.bottom) / 2).value,
            absoluteTolerance = 0.5f,
        )
        onNodeWithTag(FallingBlocksTestTags.Preview).assertIsDisplayed()
        onNodeWithTag(FallingBlocksTestTags.Level).assertIsDisplayed()
        onNodeWithTag(FallingBlocksTestTags.Lines).assertIsDisplayed()
        onNodeWithText("Hold").assertDoesNotExist()
    }
}

private class FakeComponent : FallingBlocksComponent {
    override val model: Value<FallingBlocksComponent.Model> = MutableValue(
        FallingBlocksComponent.Model(
            game = DefaultFallingBlocksEngine.initial(seed = 1L, runId = 1L),
            loading = false,
            tutorialSeen = true,
            bestScore = 0L,
            active = true,
        ),
    )

    override fun rotate() = Unit
    override fun move(cells: Int) = Unit
    override fun softDrop(cells: Int) = Unit
    override fun hardDrop() = Unit
    override fun revive() = Unit
    override fun newGame() = Unit
    override fun completeTutorial() = Unit
}
