package ge.yet.game.fruitmerge.domain.model

internal enum class ActionRejection {
    GAME_OVER,
    BOARD_BUSY,
    BODY_NOT_FOUND,
    NO_FREE_USE,
    BODY_LIMIT,
    DROP_COOLDOWN,
    SHAKE_ACTIVE,
}

internal data class ActionResult(
    val state: FruitMergeState,
    val rejection: ActionRejection? = null,
)

internal data class EngineDiagnostics(
    val maxCandidatePairs: Int = 0,
)
