# Public author API

## Construction

- `audioProgram { tempo; control; include; instrument; musicTrack; sfx; musicBus; sfxBus }`
- `audioProgramFragment { ... }` for reusable declarations
- Values: `Int.ms`, `Double.seconds`, `Int.hz`, `Double.hz`, `MidiNote.of(0..127)`
- Typed command names: `AudioControlName`, `SfxName`
- Names use lowercase snake case and begin with a letter.

## Voice sources and shaping

- Oscillators: sine, triangle, saw, square, pulse
- Noise: white, pink, brown with explicit `Long` seed
- Additive `partial`, optionally with its own ADSR, frequency modulation, vibrato
- ADSR `envelope`
- Low/high/band-pass filters, including `noteFrequency(ratio, offsetHz)` cutoff tracking
- Distortion and bit crush
- Delay and reverb sends; linked-stereo compressor and limiter on `musicBus`/`sfxBus`

Parameters are constants, mapped controls, sine LFO, seeded smooth noise, note frequency, or a product. Keep every output range legal for its destination. `AudioNote.Pitched` velocity is finite and in `0f..1f`.

## Patterns

Use `pure`, `sequence`, `stack`, `euclidean`, `choose`, `degrade`, `fast`, `slow`, `shift`, `repeat`, `every`, `swing`, and seeded `humanize`. `humanizedNotes` supplies deterministic velocity variation. `tonalScale` provides a small checked set of common modes. Track `arrangement` sections can repeat a pattern, transpose it, or mute whole cycles without replacing the scheduler. Randomized operations require explicit seeds and are deterministic.

## Runtime

`MiniAppAudio` has four commands: `playMusic`, `stopMusic`, `playSfx`, `setControl`. Handle `Accepted` or `Rejected(reason, diagnostics)`. Visibility, user Music/SFX preferences, session closure and backend availability can reject/suppress playback.

The handle belongs to one MiniApp session. The host closes it on lifecycle destruction. A game calls `stopMusic` only for its own intentional pause/transition.

Full author documentation: `docs/miniapp/audio/getting-started.md`.
