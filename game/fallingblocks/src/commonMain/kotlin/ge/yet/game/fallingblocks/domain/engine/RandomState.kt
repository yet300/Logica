package ge.yet.game.fallingblocks.domain.engine

import kotlin.jvm.JvmInline

@JvmInline
value class RandomState(
    val bits: Long,
) {
    fun nextInt(bound: Int): RandomInt {
        require(bound > 0) { "Random bound must be positive" }
        val nextBits = bits * MULTIPLIER + INCREMENT
        val value = ((nextBits ushr 1) % bound.toLong()).toInt()
        return RandomInt(value = value, next = RandomState(nextBits))
    }

    private companion object {
        const val MULTIPLIER: Long = 6_364_136_223_846_793_005L
        const val INCREMENT: Long = 1_442_695_040_888_963_407L
    }
}

data class RandomInt(
    val value: Int,
    val next: RandomState,
)
