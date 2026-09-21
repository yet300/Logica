package ge.yet.game.fallingblocks.ui.screen.result

import kotlin.math.min

internal data class ResultLayoutPolicy(
    val isCompact: Boolean,
    val isUltraCompact: Boolean,
    val horizontalPaddingDp: Float,
    val verticalPaddingDp: Float,
    val sectionSpacingDp: Float,
    val cardHorizontalPaddingDp: Float,
    val cardVerticalPaddingDp: Float,
    val scoreSpacingDp: Float,
    val buttonHeightDp: Float,
    val titleGuardrailHeightDp: Float,
    val cardGuardrailHeightDp: Float,
)

internal data class ResultLayoutBudget(
    val policy: ResultLayoutPolicy,
    val usesTwoPanes: Boolean,
    val boardWidthDp: Float,
    val boardHeightDp: Float,
    val completeContentFits: Boolean,
)

internal fun resultLayoutBudget(widthDp: Float, heightDp: Float): ResultLayoutBudget {
    val policy = resultLayoutPolicy(widthDp, heightDp)
    val usesTwoPanes = widthDp > heightDp || widthDp >= EXPANDED_WIDTH_DP
    val availableWidth = (widthDp - policy.horizontalPaddingDp * 2f).coerceAtLeast(0f)
    val availableHeight = (heightDp - policy.verticalPaddingDp * 2f).coerceAtLeast(0f)
    val boardWidth = if (usesTwoPanes) {
        val boardPaneWidth = (availableWidth - policy.sectionSpacingDp).coerceAtLeast(0f) / 2f
        min(MAX_BOARD_WIDTH_DP, min(boardPaneWidth, availableHeight / BOARD_HEIGHT_RATIO))
    } else {
        val boardHeightBudget = (
            availableHeight -
                policy.titleGuardrailHeightDp -
                policy.cardGuardrailHeightDp -
                policy.sectionSpacingDp * 2f
            ).coerceAtLeast(0f)
        min(MAX_BOARD_WIDTH_DP, min(availableWidth, boardHeightBudget / BOARD_HEIGHT_RATIO))
    }.coerceAtLeast(0f)
    val boardHeight = boardWidth * BOARD_HEIGHT_RATIO
    val requiredHeight = if (usesTwoPanes) {
        maxOf(
            boardHeight,
            policy.titleGuardrailHeightDp +
                policy.sectionSpacingDp +
                policy.cardGuardrailHeightDp,
        ) + policy.verticalPaddingDp * 2f
    } else {
        boardHeight +
            policy.titleGuardrailHeightDp +
            policy.cardGuardrailHeightDp +
            policy.sectionSpacingDp * 2f +
            policy.verticalPaddingDp * 2f
    }

    return ResultLayoutBudget(
        policy = policy,
        usesTwoPanes = usesTwoPanes,
        boardWidthDp = boardWidth,
        boardHeightDp = boardHeight,
        completeContentFits = boardWidth > 0f && requiredHeight <= heightDp + 0.01f,
    )
}

internal fun resultLayoutPolicy(widthDp: Float, heightDp: Float): ResultLayoutPolicy = when {
    widthDp > heightDp && heightDp < 360f -> UltraCompactResultLayoutPolicy
    heightDp < 720f || widthDp > heightDp -> CompactResultLayoutPolicy
    else -> RegularResultLayoutPolicy
}

private val UltraCompactResultLayoutPolicy = ResultLayoutPolicy(
    isCompact = true,
    isUltraCompact = true,
    horizontalPaddingDp = 8f,
    verticalPaddingDp = 4f,
    sectionSpacingDp = 4f,
    cardHorizontalPaddingDp = 12f,
    cardVerticalPaddingDp = 6f,
    scoreSpacingDp = 2f,
    buttonHeightDp = 48f,
    titleGuardrailHeightDp = 32f,
    cardGuardrailHeightDp = 148f,
)

private val CompactResultLayoutPolicy = ResultLayoutPolicy(
    isCompact = true,
    isUltraCompact = false,
    horizontalPaddingDp = 12f,
    verticalPaddingDp = 8f,
    sectionSpacingDp = 8f,
    cardHorizontalPaddingDp = 16f,
    cardVerticalPaddingDp = 12f,
    scoreSpacingDp = 4f,
    buttonHeightDp = 48f,
    titleGuardrailHeightDp = 40f,
    cardGuardrailHeightDp = 170f,
)

private val RegularResultLayoutPolicy = ResultLayoutPolicy(
    isCompact = false,
    isUltraCompact = false,
    horizontalPaddingDp = 24f,
    verticalPaddingDp = 24f,
    sectionSpacingDp = 16f,
    cardHorizontalPaddingDp = 24f,
    cardVerticalPaddingDp = 20f,
    scoreSpacingDp = 8f,
    buttonHeightDp = 56f,
    titleGuardrailHeightDp = 52f,
    cardGuardrailHeightDp = 220f,
)

private const val BOARD_HEIGHT_RATIO = 2f
private const val MAX_BOARD_WIDTH_DP = 280f
private const val EXPANDED_WIDTH_DP = 840f
