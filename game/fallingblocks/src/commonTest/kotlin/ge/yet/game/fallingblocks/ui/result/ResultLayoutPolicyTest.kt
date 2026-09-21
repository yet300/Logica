package ge.yet.game.fallingblocks.ui.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultLayoutPolicyTest {
    @Test
    fun `portrait sizes stack board and card without clipping the cta`() {
        listOf(
            320f to 568f,
            360f to 640f,
            400f to 800f,
        ).forEach { (width, height) ->
            val budget = resultLayoutBudget(widthDp = width, heightDp = height)

            assertFalse(budget.usesTwoPanes)
            assertTrue(budget.completeContentFits, "$width x $height does not fit")
            assertEquals(budget.boardWidthDp * 2f, budget.boardHeightDp, 0.01f)
            assertTrue(budget.boardWidthDp <= 280f)
            assertTrue(budget.boardHeightDp <= 560f)
            assertTrue(budget.policy.buttonHeightDp >= 48f)
        }
    }

    @Test
    fun `landscape and expanded sizes use two panes with a capped board`() {
        listOf(
            800f to 400f,
            1200f to 800f,
        ).forEach { (width, height) ->
            val budget = resultLayoutBudget(widthDp = width, heightDp = height)

            assertTrue(budget.usesTwoPanes)
            assertTrue(budget.completeContentFits, "$width x $height does not fit")
            assertEquals(budget.boardWidthDp * 2f, budget.boardHeightDp, 0.01f)
            assertTrue(budget.boardWidthDp <= 280f)
            assertTrue(budget.boardHeightDp <= 560f)
            assertTrue(budget.policy.buttonHeightDp >= 48f)
        }
    }
}
