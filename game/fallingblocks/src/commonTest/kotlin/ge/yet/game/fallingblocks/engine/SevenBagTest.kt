package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.engine.RandomState
import ge.yet.game.fallingblocks.domain.engine.SevenBag
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertEquals

class SevenBagTest {
    @Test
    fun `each bag contains each tetromino exactly once`() {
        val draw = SevenBag.initial(RandomState(42)).draw(7)

        assertEquals(7, draw.items.size)
        assertEquals(Tetromino.entries.toSet(), draw.items.toSet())
    }

    @Test
    fun `restored random and bag state continue identically`() {
        val original = SevenBag.initial(RandomState(91)).draw(19).next

        assertEquals(original.draw(30), original.copy().draw(30))
    }

    @Test
    fun `drawing across refills preserves complete bags`() {
        val items = SevenBag.initial(RandomState(7)).draw(21).items

        items.chunked(7).forEach { bag ->
            assertEquals(Tetromino.entries.toSet(), bag.toSet())
        }
    }
}
