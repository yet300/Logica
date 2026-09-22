package ge.yet.game.fallingblocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
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
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.ui.board.BoardGeometry
import ge.yet.game.fallingblocks.ui.board.contrastRatio
import ge.yet.game.fallingblocks.ui.board.ghostStyle
import ge.yet.game.fallingblocks.ui.screen.result.FallingBlocksResultContent
import ge.yet.game.fallingblocks.ui.screen.result.FallingBlocksResultTags
import ge.yet.game.fallingblocks.ui.screen.root.FallingBlocksScoreHeader
import ge.yet.game.fallingblocks.ui.tutorial.TutorialOverlay
import ge.yet.game.fallingblocks.ui.tutorial.TutorialProgress
import ge.yet.game.fallingblocks.ui.tutorial.TutorialStep
import ge.yet.game.uikit.theme.FunfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FallingblocksThemeIntegrationTest {
    @Test
    fun `light and dark theme matrix keeps the board centered capped and ghost readable`() {
        val viewports = listOf(
            Triple(320f, 568f, 236f),
            Triple(360f, 640f, 272f),
            Triple(400f, 800f, 336f),
            Triple(800f, 400f, 152f),
            Triple(1_200f, 800f, 340f),
        )
        val schemes = listOf(lightColorScheme(), darkColorScheme())

        schemes.forEach { scheme ->
            viewports.forEach { (width, height, expectedBoardWidth) ->
                val supportReserve = if (height < 700f) 48f else 64f
                val geometry = BoardGeometry.fit(width, height, 24f, supportReserve, 340f)
                assertEquals(width / 2f, geometry.centerX, absoluteTolerance = 0.001f)
                assertEquals(height / 2f, geometry.centerY, absoluteTolerance = 0.001f)
                assertEquals(expectedBoardWidth, geometry.width, absoluteTolerance = 0.001f)
                assertEquals(Board.WIDTH.toFloat() / Board.VISIBLE_HEIGHT, geometry.width / geometry.height)
                assertTrue(geometry.left >= 0f && geometry.top >= 0f)
                assertTrue(geometry.right <= width && geometry.bottom <= height)

                Tetromino.entries.forEach { type ->
                    val style = ghostStyle(type, scheme)
                    val boardColor = scheme.surfaceContainerLowest
                    assertTrue(contrastRatio(style.outline.compositeOver(boardColor), boardColor) >= 3.0)
                }
            }
        }
    }

    @Test
    fun `toolbar score and best use block blast style pills`() = runComposeUiTest {
        setContent {
            FunfolioTheme(darkTheme = false) {
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
            FunfolioTheme(darkTheme = false) {
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
            FunfolioTheme(darkTheme = true) {
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
