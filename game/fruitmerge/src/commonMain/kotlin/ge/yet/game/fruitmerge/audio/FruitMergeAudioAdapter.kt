package ge.yet.game.fruitmerge.audio

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStore
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.consumeSilently
import ge.yet.game.miniapp.metro.MiniAppSessionScope

@SingleIn(MiniAppSessionScope::class)
internal class FruitMergeAudioAdapter @Inject constructor(
    private val audio: MiniAppAudio,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        audio.playMusic(FruitMergeAudio.program).consumeSilently()
    }

    fun play(label: FruitMergeStore.Label) {
        val name = when (label) {
            is FruitMergeStore.Label.DropReleased -> FruitMergeAudio.Release
            is FruitMergeStore.Label.FruitLanded -> landingSfx(label.level)
            is FruitMergeStore.Label.MergeResolved -> mergeSfx(label.level)
            is FruitMergeStore.Label.ClearApplied -> FruitMergeAudio.ClearSlice
            FruitMergeStore.Label.ShakeStarted -> FruitMergeAudio.ShakeLeft
            is FruitMergeStore.Label.ShakePulse -> if (label.index % 2 == 0) {
                FruitMergeAudio.ShakeRight
            } else {
                FruitMergeAudio.ShakeLeft
            }
            FruitMergeStore.Label.DangerEntered -> FruitMergeAudio.DangerEnter
            FruitMergeStore.Label.ResultReached -> FruitMergeAudio.GameOver
        }
        playSfx(name)
    }

    private fun landingSfx(level: FruitLevel): SfxName = when (level) {
        FruitLevel.BLUEBERRY,
        FruitLevel.RASPBERRY,
        FruitLevel.STRAWBERRY,
        FruitLevel.LIME,
        -> FruitMergeAudio.LandingSmall
        FruitLevel.MANDARIN,
        FruitLevel.APPLE,
        FruitLevel.PEAR,
        -> FruitMergeAudio.LandingMedium
        FruitLevel.PEACH,
        FruitLevel.PINEAPPLE,
        FruitLevel.WATERMELON,
        -> FruitMergeAudio.LandingHeavy
    }

    private fun mergeSfx(level: FruitLevel): SfxName = when (level) {
        FruitLevel.BLUEBERRY,
        FruitLevel.RASPBERRY,
        FruitLevel.STRAWBERRY,
        -> FruitMergeAudio.MergeLow
        FruitLevel.LIME,
        FruitLevel.MANDARIN,
        FruitLevel.APPLE,
        FruitLevel.PEAR,
        -> FruitMergeAudio.MergeMid
        FruitLevel.PEACH,
        FruitLevel.PINEAPPLE,
        FruitLevel.WATERMELON,
        -> FruitMergeAudio.MergeHigh
    }

    private fun playSfx(name: SfxName) {
        audio.playSfx(FruitMergeAudio.program, name).consumeSilently()
    }
}
