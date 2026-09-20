package ge.yet.game.fallingblocks.ui.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.uikit.theme.LogicaTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class ResultOverlayTest {
    @Test
    fun `try again overlays the unchanged board and discloses advertisement`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = false) {
                Box(Modifier.size(390.dp, 760.dp)) {
                    Box(Modifier.fillMaxSize().testTag("terminal_board"))
                    ResultOverlay(
                        model = ResultComponent.Model(
                            score = 1_250,
                            bestScore = 2_500,
                            canContinue = true,
                            continueSecondsRemaining = 5,
                        ),
                        advertisementExpected = true,
                        onPrimary = {},
                    )
                }
            }
        }

        onNodeWithTag("terminal_board").assertIsDisplayed()
        onNodeWithTag(ResultOverlayTags.Panel).assertIsDisplayed()
        onNodeWithText("Try Again").assertIsDisplayed()
        onNodeWithText("5").assertIsDisplayed()
        onNodeWithContentDescription("Try Again. Advertisement").assertIsDisplayed()
        onNodeWithText("Dismiss").assertDoesNotExist()
        onNodeWithText("Close").assertDoesNotExist()
        assertEquals(0f, RESULT_SCRIM_ALPHA)
        assertFalse(RESULT_USES_BLUR)
    }

    @Test
    fun `new game replaces continuation after countdown expires`() = runComposeUiTest {
        setContent {
            LogicaTheme(darkTheme = true) {
                Box(Modifier.size(390.dp, 760.dp)) {
                    ResultOverlay(
                        model = ResultComponent.Model(
                            score = 1_250,
                            bestScore = 2_500,
                            canContinue = true,
                            continueSecondsRemaining = 0,
                        ),
                        advertisementExpected = true,
                        onPrimary = {},
                    )
                }
            }
        }

        onNodeWithText("New Game").assertIsDisplayed()
        onNodeWithText("Try Again").assertDoesNotExist()
        onNodeWithContentDescription("New Game").assertIsDisplayed()
    }
}
