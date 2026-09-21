package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.engine.ClearKind
import ge.yet.game.fallingblocks.domain.engine.score
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoringTest {
    @Test
    fun `normal line scores use the level multiplier`() {
        val level = 3

        assertEquals(100L * level, score(ClearKind.SINGLE, level).linePoints)
        assertEquals(300L * level, score(ClearKind.DOUBLE, level).linePoints)
        assertEquals(500L * level, score(ClearKind.TRIPLE, level).linePoints)
        assertEquals(800L * level, score(ClearKind.FOUR, level).linePoints)
    }

    @Test
    fun `t spin scores cover zero through three lines`() {
        val level = 2

        assertEquals(400L * level, score(ClearKind.T_SPIN, level).linePoints)
        assertEquals(800L * level, score(ClearKind.T_SPIN_SINGLE, level).linePoints)
        assertEquals(1_200L * level, score(ClearKind.T_SPIN_DOUBLE, level).linePoints)
        assertEquals(1_600L * level, score(ClearKind.T_SPIN_TRIPLE, level).linePoints)
    }

    @Test
    fun `back to back combo and perfect clear bonuses are explicit`() {
        val result = score(
            kind = ClearKind.FOUR,
            level = 4,
            comboIndex = 2,
            backToBack = true,
            perfect = true,
        )

        assertEquals(4_800, result.linePoints)
        assertEquals(400, result.comboPoints)
        assertEquals(8_000, result.perfectClearPoints)
        assertEquals(13_200, result.total)
    }

    @Test
    fun `score arithmetic saturates instead of overflowing`() {
        assertEquals(Long.MAX_VALUE, score(ClearKind.FOUR, Int.MAX_VALUE, comboIndex = Int.MAX_VALUE).total)
    }
}
