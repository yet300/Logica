package ge.yet.game.fallingblocks.domain.model

data class ActivePiece(
    val type: Tetromino,
    val rotation: Rotation,
    val origin: Cell,
)
