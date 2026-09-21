package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.ui.screen.game.FallingBlocksTestTags
import ge.yet.game.uikit.theme.LogicaTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RootTopBarContentTest {
    @Test
    fun `score best and next pills fit side by side in a phone title slot`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = false) {
                Box(Modifier.size(320.dp, 64.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FallingBlocksScoreHeader(
                            score = 1_250,
                            bestScore = 2_500,
                            compact = true,
                        )
                        NextPiecePreview(piece = Tetromino.T, compact = true)
                    }
                }
            }
        }

        onNodeWithContentDescription("Score 1250").assertIsDisplayed()
        onNodeWithContentDescription("Best 2500").assertIsDisplayed()
        onNodeWithTag(FallingBlocksTestTags.Preview).assertIsDisplayed()
        onNodeWithTag(FallingBlocksTestTags.PreviewPiece).assertIsDisplayed()
        onNodeWithText("Next").assertIsDisplayed()

        val headerBounds = onNodeWithTag("falling_blocks_score_header").getUnclippedBoundsInRoot()
        val previewBounds = onNodeWithTag(FallingBlocksTestTags.Preview).getUnclippedBoundsInRoot()
        val headerHeight = (headerBounds.bottom - headerBounds.top).value
        val previewHeight = (previewBounds.bottom - previewBounds.top).value
        assertTrue(headerHeight < 64f, "header height=$headerHeight")
        assertTrue(previewHeight < 64f, "preview height=$previewHeight")
        assertTrue(previewBounds.left >= headerBounds.right, "preview must be right of score")
        val combinedWidth = previewBounds.right.value - headerBounds.left.value
        assertTrue(combinedWidth <= 258f, "combined width=$combinedWidth")

        // Symmetry: every pill shares the same width and height.
        onAllNodesWithTag(ScorePillTag).assertCountEquals(2)
        val firstPill = onAllNodesWithTag(ScorePillTag)[0].getUnclippedBoundsInRoot()
        val secondPill = onAllNodesWithTag(ScorePillTag)[1].getUnclippedBoundsInRoot()
        val pillWidth = (firstPill.right - firstPill.left).value
        assertEquals(
            pillWidth,
            (secondPill.right - secondPill.left).value,
            absoluteTolerance = 0.5f,
        )
        assertEquals(
            pillWidth,
            (previewBounds.right - previewBounds.left).value,
            absoluteTolerance = 0.5f,
        )
        assertEquals(
            (firstPill.bottom - firstPill.top).value,
            previewHeight,
            absoluteTolerance = 0.5f,
        )
    }

    @Test
    fun `beaten best collapses to one highlighted pill`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = false) {
                Box(Modifier.size(320.dp, 64.dp)) {
                    FallingBlocksScoreHeader(
                        score = 2_500,
                        bestScore = 2_500,
                        compact = true,
                    )
                }
            }
        }

        onNodeWithContentDescription("Best 2500").assertIsDisplayed()
        onNodeWithContentDescription("Score 2500").assertDoesNotExist()
    }

    @Test
    fun `zero score keeps two pills and next preview keeps its slot`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = true) {
                Box(Modifier.size(320.dp, 64.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FallingBlocksScoreHeader(
                            score = 0,
                            bestScore = 0,
                            compact = true,
                        )
                        NextPiecePreview(piece = null, compact = true)
                    }
                }
            }
        }

        onNodeWithContentDescription("Score 0").assertIsDisplayed()
        onNodeWithContentDescription("Best 0").assertIsDisplayed()
        onNodeWithTag(FallingBlocksTestTags.Preview).assertIsDisplayed()
        onNodeWithText("Next").assertIsDisplayed()
    }
}
