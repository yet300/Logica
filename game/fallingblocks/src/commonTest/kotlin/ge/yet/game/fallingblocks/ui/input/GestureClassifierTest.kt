package ge.yet.game.fallingblocks.ui.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GestureClassifierTest {
    @Test fun `tap rotates only below slop`() {
        val subject = classifier()
        subject.down(100f, 100f, 0)
        subject.move(104f, 103f, 40)
        assertEquals(listOf(GestureEvent.Rotate), subject.up(104f, 103f, 80))
    }

    @Test fun `horizontal overshoot is retained`() {
        val subject = classifier()
        subject.down(0f, 0f, 0)
        assertEquals(listOf(GestureEvent.MoveHorizontal(1)), subject.move(64f, 0f, 20))
        assertEquals(listOf(GestureEvent.MoveHorizontal(1)), subject.move(80f, 0f, 40))
    }

    @Test fun `slow downward drag emits soft drop`() {
        val subject = classifier()
        subject.down(0f, 0f, 0)
        assertEquals(listOf(GestureEvent.SoftDrop(2)), subject.move(0f, 80f, 400))
        assertTrue(subject.up(0f, 80f, 500).none { it is GestureEvent.HardDrop })
    }

    @Test fun `fast downward release emits one hard drop`() {
        val subject = classifier()
        subject.down(0f, 0f, 0)
        subject.move(0f, 120f, 50)
        assertEquals(listOf(GestureEvent.HardDrop), subject.up(0f, 180f, 75))
    }

    @Test fun `additional pointer cancels the sequence`() {
        val subject = classifier()
        subject.down(0f, 0f, 0)
        assertTrue(subject.cancelForAdditionalPointer().isEmpty())
        assertTrue(subject.up(0f, 0f, 50).isEmpty())
    }

    private fun classifier() = GestureClassifier(40f, 8f, 1_200f)
}
