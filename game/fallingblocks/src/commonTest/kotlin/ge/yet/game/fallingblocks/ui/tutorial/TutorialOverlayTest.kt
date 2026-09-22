package ge.yet.game.fallingblocks.ui.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import ge.yet.game.uikit.theme.FunfolioTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TutorialOverlayTest {
    @Test
    fun `mandatory tutorial renders instruction without a skip action`() = runComposeUiTest {
        setContent {
            FunfolioTheme(darkTheme = false) {
                Box(Modifier.size(360.dp, 720.dp)) {
                    TutorialOverlay(
                        progress = TutorialProgress(TutorialStep.HARD_DROP),
                        reducedMotion = true,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }

        onNodeWithTag(TutorialOverlayTag).assertIsDisplayed()
        onNodeWithText("Fling down to hard drop").assertIsDisplayed()
        onNodeWithText("Skip").assertDoesNotExist()
    }
}
