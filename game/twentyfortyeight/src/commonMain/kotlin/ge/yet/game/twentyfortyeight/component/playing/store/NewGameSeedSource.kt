package ge.yet.game.twentyfortyeight.component.playing.store

import dev.zacsweers.metro.Inject
import kotlin.random.Random

internal fun interface NewGameSeedSource {
    fun nextSeed(): Long
}

internal class RandomNewGameSeedSource @Inject constructor() : NewGameSeedSource {
    override fun nextSeed(): Long = Random.nextLong()
}
