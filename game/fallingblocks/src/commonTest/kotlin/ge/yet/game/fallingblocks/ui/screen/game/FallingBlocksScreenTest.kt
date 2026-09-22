package ge.yet.game.fallingblocks.ui.screen.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.game.FallingBlocksVisualEvent
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.uikit.theme.FunfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FallingBlocksScreenTest {
    @Test
    fun `board is centered with next piece moved to app bar and no gameplay metrics`() = runComposeUiTest {
        setContent {
            FunfolioTheme(darkTheme = false) {
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
        // Next preview now lives in the host app bar, not in the viewport.
        onNodeWithTag(FallingBlocksTestTags.Preview).assertDoesNotExist()
        onAllNodesWithTag(FallingBlocksTestTags.PreviewPiece).assertCountEquals(0)
        onNodeWithText("Next").assertDoesNotExist()
        onNodeWithTag("falling_blocks_level").assertDoesNotExist()
        onNodeWithTag("falling_blocks_lines").assertDoesNotExist()
        onNodeWithText("Score").assertDoesNotExist()
        onNodeWithText("Hold").assertDoesNotExist()
    }

    @Test
    fun `visual event mounts one board effect layer`() = runComposeUiTest {
        setContent {
            FunfolioTheme(darkTheme = true) {
                Box(Modifier.size(400.dp, 800.dp)) {
                    FallingBlocksScreen(
                        FakeComponent(
                            visualEvent = FallingBlocksVisualEvent.HardDrop(
                                id = 1,
                                type = Tetromino.I,
                                from = listOf(Cell(3, 3)),
                                to = listOf(Cell(3, 20)),
                            ),
                        ),
                    )
                }
            }
        }

        onAllNodesWithTag(FallingBlocksTestTags.Effects).assertCountEquals(1)
    }
}

private class FakeComponent(
    visualEvent: FallingBlocksVisualEvent? = null,
) : FallingBlocksComponent {
    override val model: Value<FallingBlocksComponent.Model> = MutableValue(
        FallingBlocksComponent.Model(
            game = DefaultFallingBlocksEngine.initial(seed = 1L, runId = 1L),
            loading = false,
            tutorialSeen = true,
            tutorialProgress = null,
            bestScore = 0L,
            active = true,
            visualEvent = visualEvent,
        ),
    )

    override fun rotate() = Unit
    override fun move(cells: Int) = Unit
    override fun softDrop(cells: Int) = Unit
    override fun hardDrop() = Unit
    override fun revive() = Unit
    override fun newGame() = Unit
}
