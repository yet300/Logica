package ge.yet.game.fallingblocks.component.game.store

import kotlin.test.Test
import kotlin.test.assertEquals

class TickPlannerTest {
    @Test
    fun `planner preserves sub-step remainder`() {
        val planner = TickPlanner()

        assertEquals(emptyList(), planner.consume(15))
        assertEquals(listOf(16), planner.consume(1))
        assertEquals(0, planner.remainderMillis)
    }

    @Test
    fun `planner clamps a frame and emits at most three advances`() {
        val planner = TickPlanner()

        assertEquals(listOf(16, 16, 16), planner.consume(1_000))
        assertEquals(202, planner.remainderMillis)
        assertEquals(listOf(16, 16, 16), planner.consume(0))
        assertEquals(154, planner.remainderMillis)
    }

    @Test
    fun `reset drops stale elapsed time`() {
        val planner = TickPlanner()
        planner.consume(250)

        planner.reset()

        assertEquals(0, planner.remainderMillis)
        assertEquals(emptyList(), planner.consume(0))
    }
}
