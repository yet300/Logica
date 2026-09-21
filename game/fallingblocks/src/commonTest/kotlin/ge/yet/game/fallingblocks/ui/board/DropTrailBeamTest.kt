package ge.yet.game.fallingblocks.ui.board

import ge.yet.game.fallingblocks.domain.model.Cell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DropTrailBeamTest {
    @Test
    fun `beam spans source to landing per column and clips hidden rows`() {
        val beam = dropTrailBeam(
            from = listOf(Cell(4, 0), Cell(5, 1)),
            to = listOf(Cell(4, 20), Cell(5, 19)),
        )

        assertEquals(2, beam.size)
        val first = beam.first { it.x == 4 }
        assertEquals(0f, first.top)
        assertEquals(19f / 20, first.bottom, absoluteTolerance = 1e-4f)
        val second = beam.first { it.x == 5 }
        assertEquals(0f, second.top)
        assertEquals(18f / 20, second.bottom, absoluteTolerance = 1e-4f)
    }

    @Test
    fun `beam drops hidden landings and out of board columns`() {
        val beam = dropTrailBeam(
            from = listOf(Cell(4, 0), Cell(-1, 5)),
            to = listOf(Cell(4, 0), Cell(-1, 6)),
        )

        assertEquals(emptyList(), beam)
    }

    @Test
    fun `halftone dots are deterministic bounded and capped`() {
        val beam = dropTrailBeam(
            from = listOf(Cell(4, 2)),
            to = listOf(Cell(4, 21)),
        )
        assertEquals(1, beam.size)

        val first = trailHalftoneDots(eventId = 41L, columns = beam)
        val repeated = trailHalftoneDots(eventId = 41L, columns = beam)
        val other = trailHalftoneDots(eventId = 42L, columns = beam)

        assertEquals(first, repeated)
        assertNotEquals(first, other)
        assertTrue(first.isNotEmpty())
        assertTrue(first.size <= 240)
        first.forEach { dot ->
            assertTrue(dot.cx in 0f..1f, "cx=${dot.cx}")
            assertTrue(dot.cy in beam[0].top..beam[0].bottom, "cy=${dot.cy}")
            assertTrue(dot.radius in 0.002f..0.015f, "radius=${dot.radius}")
            assertTrue(dot.alpha in 0f..1f, "alpha=${dot.alpha}")
        }
    }

    @Test
    fun `halftone density fades toward the top`() {
        val beam = dropTrailBeam(
            from = listOf(Cell(4, 2)),
            to = listOf(Cell(4, 21)),
        )
        val dots = trailHalftoneDots(eventId = 41L, columns = beam)
        val span = beam[0].bottom - beam[0].top
        val topThird = dots.count { it.cy < beam[0].top + span / 3f }
        val bottomThird = dots.count { it.cy > beam[0].bottom - span / 3f }

        assertTrue(topThird < bottomThird, "top=$topThird bottom=$bottomThird")
    }
}
