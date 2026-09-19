package ge.yet.game.pattern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PatternTimingTransformsTest {
    @Test
    fun `humanize is deterministic for one seed and changes for another`() {
        val source = sequence("a", "b", "c", "d")

        val first = source.humanize(CycleTime.of(1, 64), seed = 41L).query(TimeArc.unit)
        val repeated = source.humanize(CycleTime.of(1, 64), seed = 41L).query(TimeArc.unit)
        val changed = source.humanize(CycleTime.of(1, 64), seed = 42L).query(TimeArc.unit)

        assertEquals(first, repeated)
        assertNotEquals(first.map { it.whole }, changed.map { it.whole })
    }

    @Test
    fun `humanize keeps every shift within the declared bound`() {
        val bound = CycleTime.of(1, 32)
        val original = sequence(0, 1, 2, 3, 4, 5, 6, 7).query(TimeArc.unit)
        val shifted = sequence(0, 1, 2, 3, 4, 5, 6, 7)
            .humanize(bound, seed = 912L)
            .query(TimeArc(CycleTime.of(-1), CycleTime.of(2)))

        original.forEach { event ->
            val matching = shifted.single {
                it.value == event.value &&
                    it.whole.start >= event.whole.start - bound &&
                    it.whole.start <= event.whole.start + bound
            }
            val delta = matching.whole.start - event.whole.start
            assertTrue(delta >= -bound && delta <= bound, "delta=$delta for ${event.value}")
        }
    }

    @Test
    fun `expanded humanize query retains events shifted over either boundary`() {
        val pattern = sequence("first", "second")
            .humanize(CycleTime.of(1, 4), seed = 73L)
        val wide = pattern.query(TimeArc(CycleTime.of(-1), CycleTime.of(2)))
        val unit = pattern.query(TimeArc.unit)

        val expected = wide.filter { it.active.intersection(TimeArc.unit) != null }
        assertEquals(expected.map { it.whole }, unit.map { it.whole })
        assertEquals(expected.map { it.value }, unit.map { it.value })
        assertEquals(unit.map { it.whole }.distinct().size, unit.size)
    }

    @Test
    fun `swing delays only odd subdivisions`() {
        val events = sequence(0, 1, 2, 3)
            .swing(subdivisions = 4, amount = CycleTime.of(1, 16))
            .query(TimeArc.unit)
            .filter { it.whole.start >= CycleTime.ZERO }

        assertEquals(CycleTime.ZERO, events.single { it.value == 0 }.whole.start)
        assertEquals(CycleTime.of(5, 16), events.single { it.value == 1 }.whole.start)
        assertEquals(CycleTime.of(1, 2), events.single { it.value == 2 }.whole.start)
        assertEquals(CycleTime.of(13, 16), events.single { it.value == 3 }.whole.start)
    }

    @Test
    fun `timing transforms consume one operation and preserve event budgets`() {
        val error = assertFailsWith<PatternQueryException> {
            sequence(0, 1)
                .humanize(CycleTime.ZERO, seed = 1L)
                .swing(2, CycleTime.ZERO)
                .query(TimeArc.unit, PatternQueryBudget(maxOperations = 2, maxEvents = 2))
        }

        assertEquals(PatternQueryLimit.OPERATIONS, error.limit)
    }
}
