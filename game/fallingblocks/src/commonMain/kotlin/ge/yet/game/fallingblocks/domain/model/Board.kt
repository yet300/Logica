package ge.yet.game.fallingblocks.domain.model

data class Board(
    val cells: List<Tetromino?>,
) {
    init {
        require(cells.size == WIDTH * TOTAL_HEIGHT) {
            "Board requires ${WIDTH * TOTAL_HEIGHT} cells, but received ${cells.size}"
        }
    }

    operator fun get(cell: Cell): Tetromino? = cells[cell.y * WIDTH + cell.x]

    companion object {
        const val WIDTH: Int = 10
        const val VISIBLE_HEIGHT: Int = 20
        const val HIDDEN_ROWS: Int = 2
        const val TOTAL_HEIGHT: Int = VISIBLE_HEIGHT + HIDDEN_ROWS

        fun empty(): Board = Board(List(WIDTH * TOTAL_HEIGHT) { null })
    }
}
