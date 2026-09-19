# Effects and the retro-bit aesthetic

Use effects to support a sonic intention, not as a default stack.

- Low/high/band-pass filters shape spectral range; resonance is `0f..1f`.
- Distortion adds harmonics; begin below `0.15f` and verify peaks.
- Delay uses a positive time and feedback below `1f`; long/high-feedback tails consume headroom.
- Reverb is a send amount in `0f..1f`.
- `musicBus`/`sfxBus` compressor uses a linear threshold, ratio, attack/release and makeup gain; it links both channels to preserve stereo position.
- A bus limiter uses a linear ceiling and release and is distinct from the renderer's final safety limiter.
- `bitCrush(bitDepth, sampleRateReduction)` creates a deliberately quantized/aliased texture.

```kotlin
private val retroProgram = audioProgram {
    instrument("retro_lead") {
        oscillator(OscillatorShape.PULSE, gain = 0.38f)
        envelope(attack = 2.ms, decay = 40.ms, sustain = 0.65f, release = 80.ms)
        bitCrush(bitDepth = 8, sampleRateReduction = 4)
    }
    musicTrack("retro_theme") {
        instrument("retro_lead")
        notes(deterministicMelody)
        gain(0.32f)
    }
}
```

“8-bit”, “16-bit”, “32-bit”, or “64-bit music” is not an output-file setting in this engine. The runtime always produces normal high-quality floating-point PCM for the platform sink. Recreate the aesthetic through oscillator choice, envelopes, limited polyphony, `bitDepth` (supported range `2..24`) and sample-rate reduction (`1..64`). Values above the bit-depth limit are neither useful nor accepted.

Effects do not license clipping. Test overlapping music and SFX, not only isolated voices.

Track delay/reverb and bus effects execute in the same bounded realtime renderer used by Android, iOS and `MiniAppAudioTestRenderer`. Processor pools are fixed; declarations beyond their budgets fail compilation instead of allocating more DSP state while audio is running.
