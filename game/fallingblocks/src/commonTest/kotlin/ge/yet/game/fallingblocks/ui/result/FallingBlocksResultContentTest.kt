package ge.yet.game.fallingblocks.ui.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import ge.yet.game.fallingblocks.component.result.FallingBlocksResultSnapshot
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.fallingblocks.ui.screen.result.FallingBlocksResultContent
import ge.yet.game.fallingblocks.ui.screen.result.FallingBlocksResultTags
import ge.yet.game.uikit.theme.FunfolioTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FallingBlocksResultContentTest {
    @Test
    fun `continue result is a full destination with final board and advertisement semantics`() =
        runComposeUiTest {
            setContent {
                FunfolioTheme(darkTheme = false) {
                    Box(Modifier.size(390.dp, 760.dp)) {
                        FallingBlocksResultContent(
                            model = resultModel(seconds = 5),
                            advertisementExpected = true,
                            onPrimary = {},
                        )
                    }
                }
            }

            onNodeWithTag(FallingBlocksResultTags.Root).assertIsDisplayed()
            onNodeWithTag(FallingBlocksResultTags.Board).assertIsDisplayed()
            onNodeWithContentDescription("Final board with 0 occupied cells.").assertIsDisplayed()
            onNodeWithText("Game over").assertIsDisplayed()
            onNodeWithText("1,250").assertIsDisplayed()
            onNodeWithText("Best · 2,500").assertIsDisplayed()
            onNodeWithText("Try Again (5)").assertIsDisplayed()
            onNodeWithContentDescription("Try Again (5). Advertisement").assertIsDisplayed()
            onNodeWithTag(FallingBlocksResultTags.Primary).assertHeightIsAtLeast(48.dp)
            onNodeWithTag("falling_blocks_result_overlay").assertDoesNotExist()
            onNodeWithTag("falling_blocks_result_panel").assertDoesNotExist()
            onNodeWithTag("falling_blocks_result_scrim").assertDoesNotExist()
            onNodeWithTag("falling_blocks_result_blur").assertDoesNotExist()
        }

    @Test
    fun `new game replaces timed continuation`() = runComposeUiTest {
        setContent {
            FunfolioTheme(darkTheme = true) {
                Box(Modifier.size(800.dp, 400.dp)) {
                    FallingBlocksResultContent(
                        model = resultModel(seconds = 0),
                        advertisementExpected = true,
                        onPrimary = {},
                    )
                }
            }
        }

        onNodeWithText("New Game").assertIsDisplayed()
        onNodeWithText("Try Again (0)").assertDoesNotExist()
        onNodeWithContentDescription("New Game").assertIsDisplayed()
        onNodeWithTag(FallingBlocksResultTags.Primary).assertHeightIsAtLeast(48.dp)
    }

    private fun resultModel(seconds: Int) = ResultComponent.Model(
        snapshot = FallingBlocksResultSnapshot.from(
            gameFixture(score = 1_250),
            bestScore = 2_500,
        ),
        canContinue = true,
        continueSecondsRemaining = seconds,
    )
}
