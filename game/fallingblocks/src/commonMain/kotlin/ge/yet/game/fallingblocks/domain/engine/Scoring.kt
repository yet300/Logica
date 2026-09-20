package ge.yet.game.fallingblocks.domain.engine

internal enum class ClearKind(
    val basePoints: Long,
    val backToBackEligible: Boolean,
) {
    NONE(0, false),
    SINGLE(100, false),
    DOUBLE(300, false),
    TRIPLE(500, false),
    FOUR(800, true),
    T_SPIN(400, false),
    T_SPIN_SINGLE(800, true),
    T_SPIN_DOUBLE(1_200, true),
    T_SPIN_TRIPLE(1_600, true),
}

internal data class ScoreBreakdown(
    val linePoints: Long,
    val comboPoints: Long,
    val perfectClearPoints: Long,
) {
    val total: Long = saturatingAdd(
        saturatingAdd(linePoints, comboPoints),
        perfectClearPoints,
    )
}

internal fun score(
    kind: ClearKind,
    level: Int,
    comboIndex: Int = -1,
    backToBack: Boolean = false,
    perfect: Boolean = false,
): ScoreBreakdown {
    require(level >= 1) { "Level must be positive" }
    val levelMultiplier = level.toLong()
    val baseLinePoints = saturatingMultiply(kind.basePoints, levelMultiplier)
    val linePoints = if (backToBack && kind.backToBackEligible) {
        saturatingAdd(baseLinePoints, baseLinePoints / 2)
    } else {
        baseLinePoints
    }
    val comboPoints = if (comboIndex > 0) {
        saturatingMultiply(
            saturatingMultiply(50, comboIndex.toLong()),
            levelMultiplier,
        )
    } else {
        0
    }
    val perfectClearPoints = if (perfect) saturatingMultiply(2_000, levelMultiplier) else 0
    return ScoreBreakdown(linePoints, comboPoints, perfectClearPoints)
}

internal fun saturatingAdd(left: Long, right: Long): Long =
    if (left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

internal fun saturatingMultiply(left: Long, right: Long): Long = when {
    left == 0L || right == 0L -> 0
    left > Long.MAX_VALUE / right -> Long.MAX_VALUE
    else -> left * right
}
