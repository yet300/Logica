package ge.yet.game.fallingblocks.domain.model

enum class Tetromino {
    I,
    J,
    L,
    O,
    S,
    T,
    Z,
}

enum class Rotation {
    SPAWN,
    RIGHT,
    REVERSE,
    LEFT,
}

data class Cell(
    val x: Int,
    val y: Int,
)
