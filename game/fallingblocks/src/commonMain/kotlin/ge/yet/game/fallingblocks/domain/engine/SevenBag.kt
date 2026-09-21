package ge.yet.game.fallingblocks.domain.engine

import ge.yet.game.fallingblocks.domain.model.Tetromino

data class BagState(
    val remaining: List<Tetromino>,
    val random: RandomState,
) {
    fun draw(count: Int): BagDraw = SevenBag.draw(this, count)
}

data class BagDraw(
    val items: List<Tetromino>,
    val next: BagState,
)

object SevenBag {
    fun initial(random: RandomState): BagState = BagState(
        remaining = emptyList(),
        random = random,
    )

    fun draw(state: BagState, count: Int): BagDraw {
        require(count >= 0) { "Draw count cannot be negative" }

        var remaining = state.remaining
        var random = state.random
        val items = ArrayList<Tetromino>(count)
        repeat(count) {
            if (remaining.isEmpty()) {
                val refill = shuffledBag(random)
                remaining = refill.first
                random = refill.second
            }
            items += remaining.first()
            remaining = remaining.drop(1)
        }
        return BagDraw(
            items = items,
            next = BagState(remaining = remaining, random = random),
        )
    }

    private fun shuffledBag(initialRandom: RandomState): Pair<List<Tetromino>, RandomState> {
        val pieces = Tetromino.entries.toMutableList()
        var random = initialRandom
        for (index in pieces.lastIndex downTo 1) {
            val draw = random.nextInt(index + 1)
            random = draw.next
            val swap = pieces[index]
            pieces[index] = pieces[draw.value]
            pieces[draw.value] = swap
        }
        return pieces.toList() to random
    }
}
