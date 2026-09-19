package ge.yet.game.blockblast.audio

import ge.yet.game.blockblast.domain.model.FeedbackType
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.ScaleMode
import ge.yet.game.miniapp.audio.audioProgram
import ge.yet.game.miniapp.audio.humanizedNotes
import ge.yet.game.miniapp.audio.hz
import ge.yet.game.miniapp.audio.ms
import ge.yet.game.miniapp.audio.noteFrequency
import ge.yet.game.miniapp.audio.presets.PowerUp
import ge.yet.game.miniapp.audio.presets.SuccessSweep
import ge.yet.game.miniapp.audio.smoothNoise
import ge.yet.game.miniapp.audio.tonalScale
import ge.yet.game.pattern.CycleTime
import ge.yet.game.pattern.degrade
import ge.yet.game.pattern.humanize

internal object BlockBlastAudio {
    val Place = SfxName("place")
    val ClearPop = SfxName("clear_pop")
    val GameOver = SfxName("game_over")
    val Revive = SfxName("revive")
    val NewBest = SfxName("new_best")

    val VoiceGood = SfxName("voice_good")
    val VoiceGreat = SfxName("voice_great")
    val VoiceAmazing = SfxName("voice_amazing")
    val VoiceExcellent = SfxName("voice_excellent")
    val VoiceUnbelievable = SfxName("voice_unbelievable")

    fun voice(type: FeedbackType): SfxName = when (type) {
        FeedbackType.GOOD -> VoiceGood
        FeedbackType.GREAT -> VoiceGreat
        FeedbackType.AMAZING -> VoiceAmazing
        FeedbackType.EXCELLENT -> VoiceExcellent
        FeedbackType.UNBELIEVABLE -> VoiceUnbelievable
    }

    val program = audioProgram {
        include(PowerUp(name = Revive.value, gain = 0.26f))
        include(SuccessSweep(name = NewBest.value, gain = 0.30f))
        include(BlockBlastSfx.fragment)

        tempo(104f)
        instrument("grove_marimba") {
            oscillator(ge.yet.game.miniapp.audio.OscillatorShape.SINE, gain = 0.46f)
            oscillator(ge.yet.game.miniapp.audio.OscillatorShape.TRIANGLE, gain = 0.18f, detuneCents = -3f)
            partial(ratio = 2.63f, gain = 0.19f) {
                envelope(attack = 1.ms, decay = 95.ms, sustain = 0f, release = 45.ms)
            }
            partial(ratio = 4.17f, gain = 0.09f) {
                envelope(attack = 1.ms, decay = 58.ms, sustain = 0f, release = 30.ms)
            }
            envelope(attack = 2.ms, decay = 310.ms, sustain = 0.08f, release = 170.ms)
            lowPass(noteFrequency(ratio = 7.2f, offsetHz = 180f), resonance = 0.08f)
        }

        val grove = tonalScale(ge.yet.game.miniapp.audio.MidiNote.of(57), ScaleMode.MINOR_PENTATONIC)
        val phrase = humanizedNotes(
            notes = listOf(0, 2, 1, 4, 2, 5, 3, 1).map(grove::midi),
            velocity = 0.68f..0.94f,
            seed = 0x4B1A57L,
        ).degrade(probability = 0.11f, seed = 0x71D93L)
            .humanize(maxOffset = CycleTime.of(1, 192), seed = 0x38C2FL)

        musicTrack("grove_marimba") {
            instrument("grove_marimba")
            gain(smoothNoise(seed = 0x61E2L, rate = 0.12.hz, range = 0.38f..0.52f))
            pan(smoothNoise(seed = 0x2C9DL, rate = 0.18.hz, range = -0.28f..0.28f))
            arrangement {
                section(cycles = 4, notes = phrase)
                section(cycles = 2, notes = phrase, transposeSemitones = 5)
                section(cycles = 2, muted = true)
            }
        }
        musicBus {
            reverb(send = 0.14f)
            compressor(threshold = 0.64f, ratio = 2.6f, attack = 9.ms, release = 120.ms, makeupGain = 1.04f)
            limiter(ceiling = 0.91f, release = 75.ms)
        }
        sfxBus {
            limiter(ceiling = 0.96f, release = 55.ms)
        }
    }
}
