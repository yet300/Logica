package ge.yet.game.fallingblocks.audio

import ge.yet.game.miniapp.audio.AudioNote
import ge.yet.game.miniapp.audio.MidiNote
import ge.yet.game.miniapp.audio.NoiseColor
import ge.yet.game.miniapp.audio.OscillatorShape
import ge.yet.game.miniapp.audio.audioProgram
import ge.yet.game.miniapp.audio.hz
import ge.yet.game.miniapp.audio.ms
import ge.yet.game.miniapp.audio.presets.AnalogBass
import ge.yet.game.miniapp.audio.presets.ChipLead
import ge.yet.game.miniapp.audio.presets.PowerUp
import ge.yet.game.miniapp.audio.presets.SuccessSweep
import ge.yet.game.miniapp.audio.presets.WoodenPlacementThock
import ge.yet.game.miniapp.audio.smoothNoise
import ge.yet.game.miniapp.audio.times
import ge.yet.game.pattern.degrade
import ge.yet.game.pattern.sequence

internal val FallingBlocksProgram = audioProgram {
    tempo(126f)
    val intensity = control(FallingBlocksAudio.Intensity.value, 0.18f, 0f..1f)

    include(AnalogBass(name = "falling_bass", gain = 0.34f))
    include(ChipLead(name = "falling_lead", gain = 0.22f))
    instrument("falling_pulse") {
        oscillator(OscillatorShape.PULSE, gain = 0.18f)
        noise(NoiseColor.PINK, seed = 0xFA11_126L, gain = 0.055f)
        envelope(attack = 1.ms, decay = 38.ms, sustain = 0.05f, release = 48.ms)
        bandPass(center = 1_050.hz, resonance = 0.18f)
        bitCrush(bitDepth = 11, sampleRateReduction = 2)
    }

    val bassPhrase = sequence(
        listOf(38, 38, 45, 41, 43, 50, 36, 43).map { AudioNote.Pitched(MidiNote.of(it)) },
    )
    val leadPhrase = sequence(
        listOf(74, 81, 77, 86, 79, 84, 72, 82).map { AudioNote.Pitched(MidiNote.of(it)) },
    ).degrade(probability = 0.24f, seed = 0x126_B10CL)
    val pulsePhrase = sequence(
        listOf(55, 43, 50, 46, 55, 48, 52, 41).map { AudioNote.Pitched(MidiNote.of(it)) },
    )

    musicTrack("bass_grid") {
        instrument("falling_bass")
        notes(bassPhrase)
        gain(intensity.map(0.12f, 0.24f))
        pan(-0.08f)
    }
    musicTrack("pulse_grid") {
        instrument("falling_pulse")
        notes(pulsePhrase)
        gain(intensity.map(0.05f, 0.16f))
        pan(smoothNoise(seed = 0xFA11_22L, rate = 0.38.hz, range = -0.26f..0.26f))
    }
    musicTrack("lead_fragments") {
        instrument("falling_lead")
        notes(leadPhrase)
        gain(intensity.map(0.005f, 0.15f) * intensity.map(0.15f, 1f))
        pan(smoothNoise(seed = 0xFA11_33L, rate = 0.24.hz, range = -0.58f..0.58f))
        delay(time = 95.ms, feedback = 0.16f)
    }

    include(WoodenPlacementThock(name = FallingBlocksAudio.Lock.value, gain = 0.88f))
    include(SuccessSweep(name = FallingBlocksAudio.Line4.value, gain = 0.28f))
    include(SuccessSweep(name = FallingBlocksAudio.Perfect.value, gain = 0.36f))
    include(PowerUp(name = FallingBlocksAudio.Revive.value, gain = 0.30f))

    sfx(FallingBlocksAudio.Move.value) {
        oscillator(OscillatorShape.PULSE, gain = 0.10f)
        pitch(330.hz, 275.hz, 34.ms)
        envelope(attack = 1.ms, decay = 18.ms, sustain = 0.08f, release = 24.ms)
        highPass(180.hz)
    }
    sfx(FallingBlocksAudio.Rotate.value) {
        oscillator(OscillatorShape.SQUARE, gain = 0.13f)
        oscillator(OscillatorShape.SINE, gain = 0.08f, detuneCents = 9f)
        pitch(410.hz, 690.hz, 75.ms)
        envelope(attack = 1.ms, decay = 32.ms, sustain = 0.18f, release = 55.ms)
        bitCrush(bitDepth = 12, sampleRateReduction = 2)
    }
    sfx(FallingBlocksAudio.SoftDrop.value) {
        oscillator(OscillatorShape.TRIANGLE, gain = 0.09f)
        pitch(260.hz, 210.hz, 42.ms)
        envelope(attack = 1.ms, decay = 20.ms, sustain = 0.06f, release = 28.ms)
    }
    sfx(FallingBlocksAudio.HardDrop.value) {
        oscillator(OscillatorShape.SINE, gain = 0.24f)
        noise(NoiseColor.BROWN, seed = 0xFA11_D0L, gain = 0.08f)
        pitch(155.hz, 58.hz, 115.ms)
        envelope(attack = 1.ms, decay = 54.ms, sustain = 0.12f, release = 95.ms)
        lowPass(1_000.hz, resonance = 0.12f)
    }
    sfx(FallingBlocksAudio.Line1.value) {
        oscillator(OscillatorShape.PULSE, gain = 0.15f)
        pitch(360.hz, 540.hz, 120.ms)
        envelope(attack = 2.ms, decay = 45.ms, sustain = 0.30f, release = 100.ms)
    }
    sfx(FallingBlocksAudio.Line2.value) {
        oscillator(OscillatorShape.PULSE, gain = 0.17f)
        pitch(390.hz, 720.hz, 150.ms)
        envelope(attack = 2.ms, decay = 55.ms, sustain = 0.34f, release = 125.ms)
    }
    sfx(FallingBlocksAudio.Line3.value) {
        oscillator(OscillatorShape.PULSE, gain = 0.18f)
        oscillator(OscillatorShape.TRIANGLE, gain = 0.08f, detuneCents = 7f)
        pitch(420.hz, 940.hz, 190.ms)
        envelope(attack = 2.ms, decay = 65.ms, sustain = 0.38f, release = 155.ms)
    }
    sfx(FallingBlocksAudio.LevelUp.value) {
        oscillator(OscillatorShape.SQUARE, gain = 0.16f)
        pitch(520.hz, 1_040.hz, 230.ms)
        envelope(attack = 2.ms, decay = 70.ms, sustain = 0.42f, release = 180.ms)
        bitCrush(bitDepth = 11, sampleRateReduction = 2)
    }
    sfx(FallingBlocksAudio.GameOver.value) {
        oscillator(OscillatorShape.SAW, gain = 0.16f)
        oscillator(OscillatorShape.SINE, gain = 0.10f)
        pitch(330.hz, 72.hz, 520.ms)
        envelope(attack = 3.ms, decay = 140.ms, sustain = 0.40f, release = 420.ms)
        lowPass(1_200.hz, resonance = 0.20f)
        bitCrush(bitDepth = 10, sampleRateReduction = 3)
    }
    musicBus {
        reverb(send = 0.10f)
        compressor(threshold = 0.62f, ratio = 2.4f, attack = 8.ms, release = 110.ms, makeupGain = 1.02f)
        limiter(ceiling = 0.90f, release = 70.ms)
    }
    sfxBus {
        limiter(ceiling = 0.95f, release = 55.ms)
    }
}
