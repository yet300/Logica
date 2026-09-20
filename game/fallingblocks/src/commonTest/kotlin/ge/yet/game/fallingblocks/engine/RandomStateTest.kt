package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.engine.RandomState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RandomStateTest {
    @Test
    fun `same random state produces the same value and successor`() {
        val first = RandomState(42).nextInt(7)
        val second = RandomState(42).nextInt(7)

        assertEquals(first, second)
        assertTrue(first.value in 0 until 7)
        assertTrue(first.next != RandomState(42))
    }

    @Test
    fun `random bounds must be positive`() {
        assertFailsWith<IllegalArgumentException> { RandomState(1).nextInt(0) }
    }
}
