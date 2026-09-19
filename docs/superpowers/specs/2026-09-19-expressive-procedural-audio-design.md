# Expressive Procedural Audio Design

Date: 2026-09-19

## Goal

Raise the public MiniApp audio authoring surface from a small deterministic
synthesizer to an expressive, still-bounded mobile instrument suitable for
original game music and high-quality SFX. The implementation must remain
Kotlin Multiplatform common code, deterministic from explicit seeds, free of
heap allocation in PCM rendering and pattern scheduling, and within the
existing `AudioMobileBudget` voice, track, control, oscillator, filter,
partial, and parameter-depth limits.

Klang is inspiration for generic ideas only. No Klang source, preset values,
notes, rhythm, arrangement, or seeds are inputs to this design.

## Current correctness issue

The public DSL exposes track delay/reverb sends and `musicBus`/`sfxBus`, but
the realtime renderer does not process them. Only the legacy offline renderer
does. The two test-render overloads therefore have different audio semantics.
New dynamics or master effects must not be added until realtime and test
rendering share one implementation.

## Scope

This change contains seven independently tested capabilities:

1. Per-note velocity and deterministic velocity variation.
2. Seeded pattern microtiming and swing.
3. Note-following filters with block-rate parameter refresh.
4. Independent envelopes for additive partials.
5. Realtime track/bus delay and reverb plus bus compressor and limiter.
6. Small scale/tonality helpers.
7. Bounded track sections with muting and transposition.

The first production proof is a new, original Block Blast track named
`grove_marimba`. Block Blast currently contains no music and must gain an
explicit session-owned start path.

## Public API

### Notes and velocity

Existing source remains valid because velocity defaults to unity:

```kotlin
sealed interface AudioNote {
    data class Pitched(
        val midi: MidiNote,
        val velocity: Float = 1f,
    ) : AudioNote

    data object Rest : AudioNote
}
```

`velocity` must be finite and in `0f..1f`. The scheduler carries it in a
preallocated `FloatArray`; `VoiceState` multiplies the voice signal by it.

A finite phrase can be built with deterministic declaration-time variation:

```kotlin
fun humanizedNotes(
    notes: List<MidiNote>,
    velocity: ClosedFloatingPointRange<Float>,
    seed: Long,
): Pattern<AudioNote>
```

The helper snapshots and randomizes the finite phrase while the program is
constructed. It never creates notes during a realtime query.

### Pattern timing

Generic transforms belong in `:core:pattern`:

```kotlin
fun <T> Pattern<T>.humanize(
    maxOffset: CycleTime,
    seed: Long,
): Pattern<T>

fun <T> Pattern<T>.swing(
    subdivisions: Int,
    amount: CycleTime,
): Pattern<T>
```

`humanize` uses each event's stable original time key. It expands the source
query arc by `maxOffset`, shifts whole and active arcs in the reusable event
buffer, clips only at the caller's requested arc, and emits no additional
events. `swing` delays odd subdivisions and is deterministic without a seed.
Both consume one pattern operation per query. `maxOffset` and `amount` must be
non-negative and less than one cycle; `subdivisions` is bounded to `2..64`.

### Note-following parameters

```kotlin
fun noteFrequency(
    ratio: Float = 1f,
    offsetHz: Float = 0f,
): AudioParameter
```

This creates `AudioParameter.NoteFrequency`. `offsetHz` is finite and may be
negative; the resulting MIDI 0 through MIDI 127 range must remain positive.
Its declared range is computed
from MIDI 0 through MIDI 127 and validated like every other filter parameter.
It can be multiplied by controls/LFO/noise through the existing product node.
Filter coefficients are refreshed once per render block from the current note,
control positions, and absolute voice frame. This also makes existing filter
LFO/smooth-noise/control parameters audible after note start without adding a
per-sample coefficient calculation.

### Partial envelopes

The existing overload remains unchanged. An expressive overload adds a
partial-local ADSR:

```kotlin
partial(ratio = 3.7f, gain = 0.12f) {
    envelope(
        attack = 0.ms,
        decay = 95.ms,
        sustain = 0f,
        release = 20.ms,
    )
}
```

`VoiceState` owns exactly `MAX_ADDITIVE_PARTIALS` preallocated partial envelope
states. A partial without its own envelope retains existing behavior. Note-off
is forwarded to every active partial envelope, and an SFX voice remains alive
until the main envelope and all partial envelopes finish.

### Track and bus effects

Existing delay and reverb source syntax remains valid. `musicBus` and `sfxBus`
also accept dynamics:

```kotlin
musicBus {
    reverb(send = 0.18f)
    compressor(
        threshold = 0.68f,
        ratio = 3f,
        attack = 8.ms,
        release = 110.ms,
        makeupGain = 1.05f,
    )
    limiter(ceiling = 0.92f, release = 70.ms)
}
```

Compressor and limiter are bus-only. Track declarations continue to accept
delay/reverb only. `musicBus` is processed before host policy gain so
visibility ducking does not change compressor behaviour. `sfxBus` is processed
independently before music/SFX summing. The existing non-configurable final
safety limiter remains the last protection after summing.

Realtime routing uses preallocated buffers:

- `MAX_TRACKS` mono track buffers of `blockCapacity` samples;
- one stereo music bus and one stereo SFX bus;
- fixed effect-state slots assigned when a compiled program is installed;
- fixed delay sample pools sized from the platform sample rate;
- fixed compressor, limiter, and reverb state arrays.

New global runtime budgets:

```kotlin
MAX_SECTIONS_PER_TRACK = 16
MAX_RUNTIME_EFFECTS = 16
MAX_DELAY_PROCESSORS = 4
MAX_REVERB_PROCESSORS = 8
MAX_DYNAMICS_PROCESSORS = 4
```

A mono track delay consumes one delay processor. A stereo bus delay consumes
two. Compilation rejects a program whose exact routing plan exceeds a global
processor budget. This avoids preallocating the theoretical cross-product of
16 tracks × 4 effects × 4 delay seconds.

Effect objects, routing indices, instrument indices, and state slots are
resolved into primitive arrays during compilation. The PCM loop performs no
name lookups, collection construction, or state allocation.

### Tonality

The small music-theory API is intentionally not a composition framework:

```kotlin
enum class ScaleMode {
    MAJOR,
    NATURAL_MINOR,
    DORIAN,
    MIXOLYDIAN,
    MAJOR_PENTATONIC,
    MINOR_PENTATONIC,
}

fun tonalScale(root: MidiNote, mode: ScaleMode): TonalScale

class TonalScale {
    fun midi(degree: Int, octave: Int = 0): MidiNote
    fun note(degree: Int, octave: Int = 0, velocity: Float = 1f): AudioNote.Pitched
}
```

Intervals are standard music-theory facts, not imported implementation or
preset data. Degree arithmetic supports negative degrees and rejects final
MIDI values outside `0..127`. All arrays are created during declaration.

### Sections and transposition

Sections are part of a music track, not a second scheduler:

```kotlin
musicTrack("grove_marimba") {
    instrument("grove_marimba")
    arrangement {
        section(cycles = 4, notes = phrase)
        section(cycles = 2, notes = phrase, transposeSemitones = 5)
        section(cycles = 2, muted = true)
    }
}
```

Each track has at most `MAX_SECTIONS_PER_TRACK`. Section lengths are positive
whole cycles. The scheduler maps the absolute cycle into the repeating section
timeline, queries the selected prebuilt pattern in section-local time, shifts
events back to absolute time, and applies bounded MIDI transposition while
writing the existing primitive scheduled-event buffer. A muted section emits
nothing. No runtime note object is created.

Calling the existing `notes(pattern)` creates the current one-cycle repeating
arrangement and remains source compatible.

## Compiled representation and lifecycle

`CompiledAudioProgram` will contain:

- instrument index per track and SFX;
- immutable section metadata in primitive arrays;
- a fixed effect-routing plan with state-slot indices;
- validated processor counts;
- the original public declaration snapshot for inspection.

`DefaultMiniAppAudio` will keep a session-local identity cache for the most
recent `AudioProgram` and compiled representation. Repeated SFX commands for a
game-owned singleton program no longer rebuild lists, sets, and runtime
instruments. Cache races may duplicate compilation but must never share mutable
DSP state across MiniApp sessions.

Mutable DSP state remains owned by `RealtimeAudioRenderer`, not by the public
program and not by a globally cached compiled object. Installing a program
resets preallocated routing states without allocation.

## Realtime render order

For each PCM block:

1. Consume a bounded number of commands outside the native callback.
2. Query bounded patterns/sections into reusable primitive buffers.
3. Render music voices into their preallocated track buffers.
4. Process each active track delay/reverb chain.
5. Apply track gain/pan while mixing into the music bus.
6. Process music bus effects and host policy/stop fades.
7. Render SFX voices into the SFX bus.
8. Process SFX bus effects.
9. Sum both buses and apply the final safety limiter.

iOS continues to render this pipeline only on the producer thread. The
`AVAudioSourceNode` callback continues to drain the SPSC ring into preallocated
bridge arrays and update atomics only.

## Block Blast production proof

Block Blast will gain an original `grove_marimba` instrument and track:

- a restrained sine/triangle fundamental;
- original inharmonic partial ratios with individually damped envelopes;
- note-following low-pass filtering;
- an original minor-pentatonic phrase;
- seeded velocity and microtiming variation;
- seeded degradation for phrase breathing;
- seeded smooth-noise gain and panorama;
- bounded sections that introduce a transposed response and a rest section;
- conservative music-bus reverb, compressor, and limiter.

Seeds, notes, rhythm, ratios, envelopes, and arrangement will be authored from
scratch. The existing SFX contract remains intact. `BlockBlastAudioPlayer`
gains idempotent `start()`, backed by `SessionAudioProgram.start()`, and the
game component starts it once per session. Host settings, visibility, ads, and
session teardown retain authority.

## TDD and acoustic verification

Tests are added before implementation and observed failing for the missing
behaviour.

### `:core:pattern`

- same humanize seed produces identical event arcs;
- a different seed changes at least one eligible event;
- offsets never exceed the declared bound;
- expanded queries do not lose or duplicate boundary events;
- swing moves only odd subdivisions;
- operation/event budgets still reject excessive patterns.

### `:miniapp:audio`

- velocity changes RMS but not scheduled pitch or start frame;
- scheduler stores velocity without object snapshots;
- note-frequency filter tracks low and high notes acoustically;
- active filter controls refresh at block boundaries;
- upper partial envelope decays faster than the fundamental;
- partial release tails complete deterministically;
- realtime delay/reverb differ from dry PCM and remain deterministic;
- compressor reduces over-threshold crest while preserving audibility;
- limiter respects its ceiling;
- realtime and the public test renderer have one semantic path;
- declared processor-budget overflow returns stable diagnostics;
- renderer allocation counters remain constant across repeated blocks;
- section mute/transposition and loop boundaries are exact.

### `:miniapp:audio-presets`

- every shared instrument gets a deterministic acoustic render assertion;
- every shared SFX gets deterministic finite/audible/headroom assertions.

### `:game:blockblast`

- program exposes `grove_marimba` plus the unchanged SFX names;
- start is idempotent and rejection is not retried in a loop;
- two complete renders have identical PCM/hash;
- velocity and humanize seeds materially affect the intended dimensions;
- section silence and transposed response occur in their declared windows;
- spectral centroid and decay tests prove marimba-like damping without
  snapshotting one platform's floating-point PCM;
- rapid SFX over music stays finite and below the final ceiling.

Verification runs common tests first, then Android host tests, Android
compilation, iOS simulator compilation/tests, dependency-boundary validation,
and the production MiniApp bundle verification.

## Documentation updates

Update:

- `docs/miniapp/audio/kotlin-dsl.md`
- `docs/miniapp/audio/instruments.md`
- `docs/miniapp/audio/patterns.md`
- `docs/miniapp/audio/effects.md`
- `docs/miniapp/audio/adaptive-music.md`
- `docs/miniapp/audio/performance-budgets.md`
- `docs/miniapp/audio/troubleshooting.md`
- skill references whose public contract changed

Examples must compile in `commonTest` and use original phrases and seeds.

## Explicit non-goals

- No copied Klang/Strudel code, parameters, presets, melody, rhythm, or seeds.
- No sample playback, external synth engine, or new module dependency.
- No arbitrary author callback in a PCM or pattern-query hot path.
- No per-sample filter-coefficient rebuild.
- No look-ahead dynamics, FFT convolution reverb, or multiband compression.
- No dynamic tempo map or sample-accurate automation graph.
- No unbounded chord/voice-leading/composition framework.
- No platform-specific author API.
- No removal of the final safety limiter.

## Delivery discipline

Implementation follows red-green-refactor per capability. Existing unrelated
working-tree changes are not touched. No Git commit is created, per the user
instruction.
