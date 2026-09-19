# Original music recipes

## Menu ambience

Start with `OceanBreeze`, `SoftRain`, `ForestNight`, or `DeepSpace`. Set a stable seed, conservative gain, low density and suitable stereo width. Rename every included instance.

## Adaptive intensity

Declare one `control("intensity", default, 0f..1f)`. Map it to a few audible layer ranges with `reference.map`; update it only when game state meaningfully changes. Do not rebuild or swap programs every frame.

## Tonal layer

Add `SoftPad`, `AnalogBass`, `ChipLead`, or `GlassBell`, then define an original short `Pattern<AudioNote>`. `tonalScale` can map original scale degrees to checked MIDI notes; it does not supply rhythm or melody. Prefer transforms, seeded degradation, bounded velocity/microtiming humanization, and explicit arrangement sections to a long copied melody.

For an original struck voice, give upper partials short local envelopes and use `noteFrequency` for register-consistent filtering. Finish a dense music bus conservatively with reverb, linked compressor, then limiter; every processor consumes the documented fixed pool.

## Deterministic variation

Use stable per-game constants for `seed`. Different roles should use different seeds. Never derive seeds from time, UI recomposition, or platform randomness when reproducibility matters.

## Retro aesthetic

Use pulse/square voices, short envelopes, restrained polyphony, and optional bit crush. “8-bit” describes an aesthetic here; the platform sink remains high-quality PCM.

See `docs/miniapp/audio/adaptive-music.md`, `patterns.md`, and `effects.md`.
