package ge.yet.game.fallingblocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.component.result.FallingBlocksResultSnapshot
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.ui.board.BoardGeometry
import ge.yet.game.fallingblocks.ui.result.FallingBlocksResultContent
import ge.yet.game.fallingblocks.ui.result.FallingBlocksResultTags
import ge.yet.game.fallingblocks.ui.screen.root.FallingBlocksScoreHeader
import ge.yet.game.fallingblocks.ui.tutorial.TutorialOverlay
import ge.yet.game.fallingblocks.ui.tutorial.TutorialProgress
import ge.yet.game.fallingblocks.ui.tutorial.TutorialStep
import ge.yet.game.uikit.theme.LogicaTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FallingblocksThemeIntegrationTest {
    @Test
    fun `board remains mathematically centered across adaptive viewport classes`() {
        val viewports = listOf(
            360f to 720f,
            960f to 540f,
            1_200f to 1_600f,
            720f to 360f,
        )

        viewports.forEach { (width, height) ->
            val geometry = BoardGeometry.fit(width, height, 24f, 48f, 340f)
            assertEquals(width / 2f, geometry.centerX, absoluteTolerance = 0.001f)
            assertEquals(height / 2f, geometry.centerY, absoluteTolerance = 0.001f)
            assertEquals(Board.WIDTH.toFloat() / Board.VISIBLE_HEIGHT, geometry.width / geometry.height)
            assertTrue(geometry.left >= 0f && geometry.top >= 0f)
            assertTrue(geometry.right <= width && geometry.bottom <= height)
        }
    }

    @Test
    fun `toolbar score and best use the shared compact score card`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = false) {
                FallingBlocksScoreHeader(score = 1_250, bestScore = 2_500)
            }
        }

        onNodeWithContentDescription("Score 1250").assertExists()
        onNodeWithContentDescription("Best 2500").assertExists()
        onNodeWithText("Level").assertDoesNotExist()
        onNodeWithText("Lines").assertDoesNotExist()
    }

    @Test
    fun `all English tutorial steps render with no Hold or Skip affordance`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = false) {
                Column {
                    TutorialStep.entries.forEach { step ->
                        Box(Modifier.size(240.dp, 180.dp)) {
                            TutorialOverlay(
                                progress = TutorialProgress(step),
                                reducedMotion = true,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
                }
            }
        }

        onNodeWithText("Tap the field to rotate").assertExists()
        onNodeWithText("Drag sideways to move").assertExists()
        onNodeWithText("Drag down slowly to soft drop").assertExists()
        onNodeWithText("Fling down to hard drop").assertExists()
        onNodeWithText("Hold").assertDoesNotExist()
        onNodeWithText("Skip").assertDoesNotExist()
    }

    @Test
    fun `result action keeps the Material minimum touch target without a scrim`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = true) {
                Box(Modifier.size(390.dp, 760.dp)) {
                    FallingBlocksResultContent(
                        model = ResultComponent.Model(
                            snapshot = FallingBlocksResultSnapshot.from(
                                gameFixture(score = 1_250),
                                bestScore = 2_500,
                            ),
                            canContinue = true,
                            continueSecondsRemaining = 5,
                        ),
                        advertisementExpected = true,
                        onPrimary = {},
                    )
                }
            }
        }

        onNodeWithTag(FallingBlocksResultTags.Primary).assertHeightIsAtLeast(48.dp)
        onNodeWithTag(FallingBlocksResultTags.Board).assertExists()
    }
}
