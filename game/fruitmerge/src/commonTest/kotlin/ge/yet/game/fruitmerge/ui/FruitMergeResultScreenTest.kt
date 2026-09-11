package ge.yet.game.fruitmerge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import ge.yet.game.fruitmerge.component.result.FruitMergeResultComponent
import ge.yet.game.fruitmerge.component.result.FruitMergeResultSnapshot
import ge.yet.game.fruitmerge.domain.model.FruitBody
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.Vec2
import ge.yet.game.uikit.theme.LogicaTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FruitMergeResultScreenTest {
    @Test
    fun `result snapshot drives board score best and new game`() = runComposeUiTest {
        val component = FakeResultComponent(
            FruitMergeResultSnapshot(
                score = 12_500L,
                bestScore = 20_000L,
                bestImprovedInRun = false,
                runOrdinal = 7L,
                bodies = listOf(
                    FruitBody(
                        id = 1L,
                        level = FruitLevel.APPLE,
                        position = Vec2(0.5f, 0.8f),
                        hasJoinedPile = true,
                    ),
                ),
                dangerSeconds = 1.5f,
            ),
        )
        setContent {
            LogicaTheme(darkTheme = false) {
                Box(Modifier.size(390.dp, 760.dp)) {
                    FruitMergeResultScreen(component)
                }
            }
        }

        onNodeWithTag(FruitMergeTestTags.Result).assertIsDisplayed()
        onNodeWithTag(FruitMergeTestTags.Board).assertIsDisplayed()
        onNodeWithTag(FruitMergeTestTags.ResultScore).assertIsDisplayed()
        onNodeWithTag(FruitMergeTestTags.ResultBest).assertIsDisplayed()
        onNodeWithTag(FruitMergeTestTags.NewGame).performClick()

        assertEquals(1, component.newGameCalls)
    }
}

private class FakeResultComponent(
    snapshot: FruitMergeResultSnapshot,
) : FruitMergeResultComponent {
    override val model: Value<FruitMergeResultComponent.Model> =
        MutableValue(FruitMergeResultComponent.Model(snapshot))
    var newGameCalls = 0

    override fun onNewGame() {
        newGameCalls += 1
    }
}
