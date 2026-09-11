package ge.yet.game.twentyfortyeight.component.playing.store

internal fun interface NewGameSeedSource {
    fun nextSeed(): Long
}
