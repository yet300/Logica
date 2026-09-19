package ge.yet.game.blockblast.audio

import ge.yet.game.miniapp.audio.NoiseColor
import ge.yet.game.miniapp.audio.OscillatorShape
import ge.yet.game.miniapp.audio.audioProgramFragment
import ge.yet.game.miniapp.audio.hz
import ge.yet.game.miniapp.audio.ms

/**
 * Original game-owned voices. Five ascending two-tone chimes replace the
 * legacy bundled `voice_*.mp3` files tier by tier: higher tiers sweep higher
 * with slightly longer tails, mirroring the old GOOD → UNBELIEVABLE
 * progression with synthesis instead of samples. Kept deliberately warm:
 * a single soft octave partial, no shimmer tails, low filter resonance —
 * spectral centroids still rise tier by tier, without glassy ringing.
 *
 * Plus two game-owned UI roles that no preset expresses: `clear_pop`
 * (pitched blip plus a seeded-noise transient, short release, safe under
 * rapid repetition) and `game_over` (single descending motif, ~300ms).
 */
internal object BlockBlastSfx {
    val fragment = audioProgramFragment {
        // Wooden placement thock: low sine knock plus a yarn-mallet tick.
        // Short and soft enough to survive rapid repetition on every move.
        sfx(BlockBlastAudio.Place.value) {
            oscillator(OscillatorShape.SINE, gain = 0.22f)
            noise(NoiseColor.PINK, gain = 0.06f, seed = 8_048_402L)
            pitch(from = 170.hz, to = 85.hz, duration = 70.ms)
            envelope(attack = 1.ms, decay = 40.ms, sustain = 0.10f, release = 60.ms)
            lowPass(cutoff = 900.hz, resonance = 0.08f)
        }
        sfx(BlockBlastAudio.VoiceGood.value) {
            oscillator(OscillatorShape.SINE, gain = 0.16f)
            oscillator(OscillatorShape.TRIANGLE, gain = 0.06f, detuneCents = 5f)
            partial(ratio = 2.01f, gain = 0.015f)
            pitch(from = 330.hz, to = 494.hz, duration = 150.ms)
            envelope(attack = 2.ms, decay = 30.ms, sustain = 0.26f, release = 90.ms)
            lowPass(cutoff = 1_600.hz, resonance = 0.08f)
        }
        sfx(BlockBlastAudio.VoiceGreat.value) {
            oscillator(OscillatorShape.SINE, gain = 0.16f)
            oscillator(OscillatorShape.TRIANGLE, gain = 0.07f, detuneCents = 6f)
            partial(ratio = 2.01f, gain = 0.018f)
            pitch(from = 392.hz, to = 587.hz, duration = 170.ms)
            envelope(attack = 2.ms, decay = 36.ms, sustain = 0.27f, release = 110.ms)
            lowPass(cutoff = 1_900.hz, resonance = 0.08f)
        }
        sfx(BlockBlastAudio.VoiceAmazing.value) {
            oscillator(OscillatorShape.SINE, gain = 0.16f)
            oscillator(OscillatorShape.TRIANGLE, gain = 0.07f, detuneCents = 7f)
            partial(ratio = 2.01f, gain = 0.020f)
            pitch(from = 440.hz, to = 659.hz, duration = 200.ms)
            envelope(attack = 3.ms, decay = 44.ms, sustain = 0.28f, release = 130.ms)
            lowPass(cutoff = 2_300.hz, resonance = 0.09f)
        }
        sfx(BlockBlastAudio.VoiceExcellent.value) {
            oscillator(OscillatorShape.SINE, gain = 0.17f)
            oscillator(OscillatorShape.TRIANGLE, gain = 0.08f, detuneCents = 8f)
            partial(ratio = 2.01f, gain = 0.022f)
            pitch(from = 523.hz, to = 784.hz, duration = 240.ms)
            envelope(attack = 3.ms, decay = 55.ms, sustain = 0.30f, release = 160.ms)
            lowPass(cutoff = 2_600.hz, resonance = 0.09f)
        }
        sfx(BlockBlastAudio.VoiceUnbelievable.value) {
            oscillator(OscillatorShape.SINE, gain = 0.17f)
            oscillator(OscillatorShape.TRIANGLE, gain = 0.08f, detuneCents = 10f)
            partial(ratio = 2.01f, gain = 0.025f)
            partial(ratio = 3.02f, gain = 0.012f)
            pitch(from = 523.hz, to = 1_046.hz, duration = 340.ms)
            envelope(attack = 4.ms, decay = 70.ms, sustain = 0.32f, release = 240.ms)
            lowPass(cutoff = 3_200.hz, resonance = 0.10f)
        }
        sfx(BlockBlastAudio.ClearPop.value) {
            oscillator(OscillatorShape.SINE, gain = 0.16f)
            oscillator(OscillatorShape.TRIANGLE, gain = 0.06f, detuneCents = 6f)
            noise(NoiseColor.WHITE, gain = 0.025f, seed = 8_048_401L)
            pitch(from = 660.hz, to = 990.hz, duration = 90.ms)
            envelope(attack = 1.ms, decay = 25.ms, sustain = 0.25f, release = 70.ms)
            highPass(cutoff = 200.hz)
        }
        sfx(BlockBlastAudio.GameOver.value) {
            oscillator(OscillatorShape.SINE, gain = 0.16f)
            oscillator(OscillatorShape.TRIANGLE, gain = 0.06f, detuneCents = -8f)
            pitch(from = 330.hz, to = 131.hz, duration = 300.ms)
            envelope(attack = 4.ms, decay = 80.ms, sustain = 0.40f, release = 240.ms)
            lowPass(cutoff = 1_200.hz, resonance = 0.10f)
        }
    }
}
