# Expressive Procedural Audio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Do not create commits: the user explicitly prohibited them.

**Goal:** Deliver the approved expressive procedural-audio API and prove it with deterministic realtime renders and an original Block Blast `grove_marimba` track.

**Architecture:** Extend immutable public declarations first, compile them into bounded primitive runtime metadata, and keep all mutable DSP state inside `RealtimeAudioRenderer`. Route the public test renderer through that same realtime implementation so author-facing tests cannot pass against semantics absent from platform playback.

**Tech Stack:** Kotlin Multiplatform common code, `:core:pattern`, `:miniapp:audio`, `:miniapp:audio-presets`, `:game:blockblast`, Kotlin test, Gradle wrapper.

---

## File map

- `core/pattern/src/commonMain/kotlin/ge/yet/game/pattern/PatternTimingTransforms.kt`: seeded timing jitter and swing only.
- `core/pattern/src/commonMain/kotlin/ge/yet/game/pattern/PatternRandom.kt`: stable stateless seed mixing reused by timing transforms.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/AudioDeclarations.kt`: immutable public note, parameter, partial, dynamics, and section declarations.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/AudioProgramDsl.kt`: author DSL builders and validation local to construction.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/Tonality.kt`: small bounded scale helper, isolated from the DSP API.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/AudioValidation.kt`: public-program validation and immutable compiled indices/routing plan.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/AudioScheduler.kt`: section selection and primitive scheduled event data.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/dsp/Voice.kt`: velocity, note-follow parameter context, partial-envelope states.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/dsp/BusEffects.kt`: allocation-free delay/reverb/compressor/limiter processor slots.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/RealtimeAudioRenderer.kt`: the sole render semantics and preallocated track/bus buffers.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/testing/MiniAppAudioTestRenderer.kt`: thin offline driver over `RealtimeAudioRenderer`.
- `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/DefaultMiniAppAudio.kt`: last-program identity compilation cache.
- `game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/audio/BlockBlastAudio.kt`: original `grove_marimba` declaration.
- `game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/data/audio/BlockBlastAudio.kt`: idempotent music start seam.
- audio documentation and skill references: author contract and budgets.

### Task 1: Deterministic timing transforms

**Files:**
- Create: `core/pattern/src/commonMain/kotlin/ge/yet/game/pattern/PatternTimingTransforms.kt`
- Create: `core/pattern/src/commonTest/kotlin/ge/yet/game/pattern/PatternTimingTransformsTest.kt`
- Modify: `core/pattern/src/commonMain/kotlin/ge/yet/game/pattern/PatternRandom.kt`

- [ ] **Step 1: Add failing deterministic and boundary tests**

Add tests which query the same source over `TimeArc.unit` and assert: identical seeds produce identical whole/active arcs; seed `41L` differs from `42L`; every delta is within `maxOffset`; an event moved across the query edge is neither lost nor duplicated; swing delays only odd grid cells. Use exact calls:

```kotlin
val shifted = sequence(listOf("a", "b", "c", "d"))
    .humanize(maxOffset = CycleTime.of(1, 64), seed = 41L)
val swung = sequence(listOf(0, 1, 2, 3))
    .swing(subdivisions = 4, amount = CycleTime.of(1, 16))
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew :core:pattern:allTests`

Expected: compilation fails because `humanize` and `swing` do not exist.

- [ ] **Step 3: Implement stable, allocation-free query transforms**

Expose:

```kotlin
fun <T> Pattern<T>.humanize(maxOffset: CycleTime, seed: Long): Pattern<T>
fun <T> Pattern<T>.swing(subdivisions: Int, amount: CycleTime): Pattern<T>
```

Validate `0 <= offset < 1 cycle` and `subdivisions in 2..64`. Query the source into the caller-owned reusable event buffer over the expanded arc, derive jitter from `(seed, event.whole.start numerator/denominator, event index)`, mutate copied event slots, and clip to the original requested arc. Charge exactly one operation through `PatternQueryBudget`; emit no extra event.

- [ ] **Step 4: Verify GREEN and budgets**

Run: `./gradlew :core:pattern:allTests :core:pattern:compileAndroidMain :core:pattern:compileKotlinIosSimulatorArm64`

Expected: all tests pass; existing operation-limit tests remain green.

- [ ] **Step 5: Record checkpoint without committing**

Run: `git diff --check -- core/pattern`

### Task 2: Velocity and declaration-time phrase humanization

**Files:**
- Modify: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/AudioDeclarations.kt`
- Modify: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/AudioProgramDsl.kt`
- Modify: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/AudioScheduler.kt`
- Modify: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/VoiceAllocator.kt`
- Modify: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/dsp/Voice.kt`
- Create: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/HumanizedNotes.kt`
- Modify tests: `AudioProgramDslTest.kt`, `AudioSchedulerTest.kt`, `RealtimeAudioRendererTest.kt`

- [ ] **Step 1: Add failing velocity tests**

Assert construction rejects NaN and values outside `0f..1f`; scheduler retains `0.37f`; two renders at velocities `1f` and `0.25f` share pitch/start frame while the second RMS is lower; identical `humanizedNotes` seeds return equal values and another seed changes at least one velocity.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :miniapp:audio:allTests`

Expected: new constructor/helper/scheduled velocity references fail to compile.

- [ ] **Step 3: Add the source-compatible API**

Implement:

```kotlin
data class Pitched(val midi: MidiNote, val velocity: Float = 1f) : AudioNote {
    init { require(velocity.isFinite() && velocity in 0f..1f) }
}

fun humanizedNotes(
    notes: List<MidiNote>,
    velocity: ClosedFloatingPointRange<Float>,
    seed: Long,
): Pattern<AudioNote>
```

The helper snapshots a non-empty finite list and uses a local deterministic integer mixer at declaration time. It produces `sequence(List<AudioNote>)`; it does not capture a mutable RNG in a query closure.

- [ ] **Step 4: Carry velocity through primitive runtime state**

Add a `FloatArray` beside scheduled MIDI/start/duration arrays. Extend the allocator start call with `velocity: Float`; store it in `VoiceState`; multiply the final pre-pan voice sample by velocity. Do not store `AudioNote` in a voice or allocate during rendering.

- [ ] **Step 5: Verify GREEN**

Run: `./gradlew :miniapp:audio:allTests :miniapp:audio:testAndroidHostTest`

Expected: deterministic tests pass and low-velocity RMS is strictly lower.

### Task 3: Tonality helpers

**Files:**
- Create: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/Tonality.kt`
- Create: `miniapp/audio/src/commonTest/kotlin/ge/yet/game/miniapp/audio/TonalityTest.kt`

- [ ] **Step 1: Add failing degree arithmetic tests**

Cover every mode, degree `0`, degree above an octave, degree `-1`, octave offset, velocity forwarding, and MIDI under/overflow rejection.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :miniapp:audio:allTests`

Expected: `ScaleMode`, `TonalScale`, and `tonalScale` are unresolved.

- [ ] **Step 3: Implement the bounded helper**

```kotlin
enum class ScaleMode { MAJOR, NATURAL_MINOR, DORIAN, MIXOLYDIAN, MAJOR_PENTATONIC, MINOR_PENTATONIC }

fun tonalScale(root: MidiNote, mode: ScaleMode): TonalScale

class TonalScale internal constructor(root: MidiNote, intervals: IntArray) {
    fun midi(degree: Int, octave: Int = 0): MidiNote
    fun note(degree: Int, octave: Int = 0, velocity: Float = 1f): AudioNote.Pitched
}
```

Store a private copied `IntArray`. Use floor division/modulo for negative degrees and validate the final MIDI integer through the existing `MidiNote` factory.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew :miniapp:audio:allTests`

### Task 4: Note-following filter parameters and block refresh

**Files:**
- Modify: `AudioDeclarations.kt`, `AudioValidation.kt`
- Modify: `internal/dsp/AudioParameterEvaluator.kt`, `internal/dsp/Voice.kt`, `internal/dsp/Filter.kt`
- Modify tests: `AudioValidationTest.kt`, `RealtimeAudioRendererTest.kt`

- [ ] **Step 1: Add failing declaration and acoustic tests**

Declare `lowPass(noteFrequency(ratio = 2.2f, offsetHz = 80f))`. Assert its calculated range stays positive, invalid ratios are rejected, a low MIDI note and high MIDI note have measurably different high-band energy, and changing an existing control becomes audible on the next block without restarting the voice.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :miniapp:audio:allTests`

- [ ] **Step 3: Add parameter node and evaluation context**

```kotlin
data class NoteFrequency internal constructor(
    val ratio: Float,
    val offsetHz: Float,
    override val outputRange: ClosedFloatingPointRange<Float>,
) : AudioParameter

fun noteFrequency(ratio: Float = 1f, offsetHz: Float = 0f): AudioParameter
```

Include this leaf in depth/control traversal. Evaluate it as `midiFrequency * ratio + offsetHz`; clamp only at the filter's safe Nyquist boundary, not in the public node.

- [ ] **Step 4: Refresh filter coefficients once per block**

Pass MIDI frequency, block-start frame, and controls into `VoiceState.beginBlock(blockStartFrame, controls)`. Re-evaluate every filter parameter and update coefficients there. Keep the sample loop coefficient-only and allocation-free.

- [ ] **Step 5: Verify GREEN**

Run: `./gradlew :miniapp:audio:allTests :miniapp:audio:testAndroidHostTest`

### Task 5: Independent partial envelopes

**Files:**
- Modify: `AudioDeclarations.kt`, `AudioProgramDsl.kt`, `AudioValidation.kt`
- Modify: `internal/dsp/Envelope.kt`, `internal/dsp/Voice.kt`
- Modify tests: `AudioProgramDslTest.kt`, `AudioValidationTest.kt`, `RealtimeAudioRendererTest.kt`

- [ ] **Step 1: Add failing structural and spectral tests**

Build one fundamental plus `partial(3.7f, 0.12f) { envelope(0.ms, 95.ms, 0f, 20.ms) }`. Assert the partial declaration snapshots its ADSR, the upper-band RMS decays before fundamental RMS, note-off reaches every partial, and voice completion waits for the longest remaining partial release.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :miniapp:audio:allTests`

- [ ] **Step 3: Add focused builder and declaration**

```kotlin
data class AdditivePartialDeclaration internal constructor(
    val ratio: Float,
    val gain: Gain,
    val envelope: EnvelopeDeclaration?,
)

fun partial(ratio: Float, gain: Float = 1f, block: PartialBuilder.() -> Unit)
```

`PartialBuilder` exposes only `envelope`; the old overload stores `null` and retains current sound.

- [ ] **Step 4: Preallocate partial envelope state**

Give every `VoiceState` fixed arrays sized `MAX_ADDITIVE_PARTIALS` for stage, level, and stage frame. Reset active slots at note start; tick only declared local envelopes; propagate note-off. The voice is finished only when the main envelope and all local envelopes are finished.

- [ ] **Step 5: Verify GREEN**

Run: `./gradlew :miniapp:audio:allTests`

### Task 6: Sections and bounded transposition

**Files:**
- Modify: `AudioDeclarations.kt`, `AudioProgramDsl.kt`, `AudioValidation.kt`
- Modify: `internal/AudioScheduler.kt`
- Modify tests: `AudioProgramDslTest.kt`, `AudioValidationTest.kt`, `AudioSchedulerTest.kt`, `RealtimeAudioRendererTest.kt`

- [ ] **Step 1: Add failing section tests**

Assert one-cycle compatibility for `notes(pattern)`, exact silence in muted sections, +5 semitone transposition only in the declared section, exact wrap at the arrangement end, positive whole-cycle validation, transposed MIDI bounds, and rejection at 17 sections.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :miniapp:audio:allTests`

- [ ] **Step 3: Add section declarations and DSL**

```kotlin
data class MusicSectionDeclaration internal constructor(
    val cycles: Int,
    val pattern: Pattern<AudioNote>?,
    val transposeSemitones: Int,
)

fun arrangement(block: ArrangementBuilder.() -> Unit)
fun ArrangementBuilder.section(
    cycles: Int,
    notes: Pattern<AudioNote>? = null,
    transposeSemitones: Int = 0,
    muted: Boolean = false,
)
```

Compile section cycle starts/lengths/transposition into primitive arrays and add `MAX_SECTIONS_PER_TRACK = 16`.

- [ ] **Step 4: Map absolute time without a second scheduler**

In the existing scheduler, locate the current section by a bounded linear scan of at most 16 entries, query its prebuilt pattern in local time, and write shifted absolute frames/MIDI/velocity into the existing scheduled-event arrays. Split a block only when it crosses a section boundary.

- [ ] **Step 5: Verify GREEN**

Run: `./gradlew :miniapp:audio:allTests`

### Task 7: Realtime routing plan and bus dynamics

**Files:**
- Modify: `AudioDeclarations.kt`, `AudioProgramDsl.kt`, `AudioValidation.kt`
- Create: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/dsp/BusEffects.kt`
- Modify: `internal/dsp/BusMixer.kt`, `internal/RealtimeAudioRenderer.kt`
- Modify tests: `AudioProgramDslTest.kt`, `AudioValidationTest.kt`, `RealtimeAudioRendererTest.kt`

- [ ] **Step 1: Add failing API, budget, and acoustic tests**

Assert bus-only compressor/limiter declarations; reject dynamics on tracks; stable diagnostics for exact delay/reverb/dynamics processor overflow; delay/reverb differs from dry output; repeated renders match; compressor lowers over-threshold crest; limiter peak never exceeds its ceiling plus `1e-4f`.

- [ ] **Step 2: Verify RED and expose the existing semantic defect**

Run: `./gradlew :miniapp:audio:allTests`

Expected: current realtime delay/reverb comparison fails even though the legacy offline renderer applies it.

- [ ] **Step 3: Extend bus declarations**

```kotlin
sealed interface BusEffectDeclaration {
    data class Delay(val time: AudioDuration, val feedback: Float): BusEffectDeclaration
    data class Reverb(val send: Float): BusEffectDeclaration
    data class Compressor(
        val threshold: Float,
        val ratio: Float,
        val attack: AudioDuration,
        val release: AudioDuration,
        val makeupGain: Float,
    ): BusEffectDeclaration
    data class Limiter(val ceiling: Float, val release: AudioDuration): BusEffectDeclaration
}
```

Keep track effects typed as send effects only. Add exact budgets: `MAX_RUNTIME_EFFECTS=16`, `MAX_DELAY_PROCESSORS=4`, `MAX_REVERB_PROCESSORS=8`, `MAX_DYNAMICS_PROCESSORS=4`.

- [ ] **Step 4: Compile primitive routing slots**

Resolve track indices, processor kinds, and state-slot indices during `compile()`. A mono track delay costs one delay slot and a stereo bus delay costs two. Reject the first exceeded global count with sorted stable path/code diagnostics.

- [ ] **Step 5: Implement fixed DSP slots and preallocated buffers**

Allocate renderer-owned storage only in the renderer constructor/program-install path: `MAX_TRACKS` mono buffers, stereo music/SFX buses, four delay pools, eight reverb states, four dynamics states. `renderBlock` clears/reuses arrays, renders music to track buffers, processes track chains, pans into music bus, processes music bus, renders/processes SFX bus, sums, and calls the existing final safety limiter.

- [ ] **Step 6: Verify GREEN and no hot-loop growth**

Run: `./gradlew :miniapp:audio:allTests :miniapp:audio:testAndroidHostTest`

Expected: repeated-block allocation counters and processor identities remain unchanged.

### Task 8: One renderer semantics for tests and platforms

**Files:**
- Modify: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/testing/MiniAppAudioTestRenderer.kt`
- Delete after migration: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/OfflineAudioRenderer.kt`
- Modify tests: `MiniAppAudioTestRendererTest.kt`, `RealtimeAudioRendererTest.kt`

- [ ] **Step 1: Add failing equivalence test**

Render the same compiled program, controls, duration, and SFX schedule through the public test helper and a direct realtime block driver. Compare frames with tolerance `1e-6f`, including delay, reverb, compressor, and limiter.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :miniapp:audio:allTests`

- [ ] **Step 3: Replace legacy DSP with an offline driver**

Make `MiniAppAudioTestRenderer` compile once, instantiate `RealtimeAudioRenderer`, enqueue the same public commands, and repeatedly render fixed blocks into its result buffer. Keep scheduling and DSP in the realtime classes; the test helper owns only duration/block iteration and result copying.

- [ ] **Step 4: Remove dead duplicate renderer**

Delete `OfflineAudioRenderer.kt` only after all call sites use the shared path. Search for its symbol and require zero production/test references.

- [ ] **Step 5: Verify GREEN**

Run: `./gradlew :miniapp:audio:allTests :miniapp:audio:testAndroidHostTest`

### Task 9: Avoid repeated compilation for singleton programs

**Files:**
- Modify: `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/internal/DefaultMiniAppAudio.kt`
- Modify: `miniapp/audio/src/commonTest/kotlin/ge/yet/game/miniapp/audio/internal/DefaultMiniAppAudioTest.kt`

- [ ] **Step 1: Add failing compile-count test**

Call `playMusic(program)` once and `playSfx(program, name)` repeatedly with the same object; assert one compilation. Pass an equal but distinct program object and assert it is compiled independently. Assert sessions do not share renderer/DSP state.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :miniapp:audio:allTests`

- [ ] **Step 3: Add a last-program identity cache**

Store only `lastSource: AudioProgram?` and `lastCompiled: CompiledAudioProgram?` per facade. Reuse when `lastSource === source`; compile and replace otherwise. Never cache `RealtimeAudioRenderer`, effect state, or voice state globally.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew :miniapp:audio:allTests`

### Task 10: Original Block Blast `grove_marimba`

**Files:**
- Modify: `game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/audio/BlockBlastAudio.kt`
- Modify: `game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/data/audio/BlockBlastAudio.kt`
- Modify: `game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/component/game/store/GameStoreFactory.kt`
- Modify tests: `BlockBlastAudioProgramTest.kt`, `BlockBlastAudioRenderTest.kt`, `data/audio/BlockBlastAudioTest.kt`

- [ ] **Step 1: Add failing contract and acoustic tests**

Assert the existing SFX name set is unchanged; one music track/instrument is named `grove_marimba`; `start()` calls `playMusic` once and never retries a rejected start; two full arrangement renders match; the rest section is silent; response section pitch is transposed; upper-band decay is faster than fundamental decay; music plus rapid SFX remains finite and inside final ceiling.

- [ ] **Step 2: Verify RED**

Run: `./gradlew :game:blockblast:allTests`

- [ ] **Step 3: Author the original declaration**

Use `tonalScale(MidiNote.of(57), ScaleMode.MINOR_PENTATONIC)`, `humanizedNotes`, `.humanize`, `.degrade`, `smoothNoise` gain/pan, partial-local envelopes, `noteFrequency`, and an eight-cycle bounded arrangement with a transposed response and mute section. Choose all notes, rhythms, ratios, envelopes, seeds, and effect values from scratch; do not consult or transcribe Klang/Strudel/demo material.

- [ ] **Step 4: Add idempotent session start**

Extend `BlockBlastAudioPlayer` with `fun start()`. Implement it through the existing `SessionAudioProgram.start()` helper and invoke it from `GameStoreFactory.ExecutorImpl.initialize()` before asynchronous state loading. Store recreation is safe because the session-scoped helper is idempotent; Compose rendering is not involved.

- [ ] **Step 5: Verify GREEN**

Run: `./gradlew :game:blockblast:allTests :game:blockblast:validateMiniAppDependencies`

### Task 11: Shared preset acoustic coverage

**Files:**
- Modify: `miniapp/audio-presets/src/commonTest/kotlin/ge/yet/game/miniapp/audio/presets/InstrumentsTest.kt`
- Modify: `miniapp/audio-presets/src/commonTest/kotlin/ge/yet/game/miniapp/audio/presets/SoundEffectsTest.kt`
- Create: `miniapp/audio-presets/src/commonTest/kotlin/ge/yet/game/miniapp/audio/presets/AcousticAssertions.kt`

- [ ] **Step 1: Add parameterized render assertions**

For every exported instrument and SFX declaration, render twice and assert deterministic channel frames, finite samples, non-zero RMS, and peak headroom. Keep shared FFT/RMS helpers in `AcousticAssertions.kt`; do not copy helpers between game tests.

- [ ] **Step 2: Run and tune only failing owned declarations**

Run: `./gradlew :miniapp:audio-presets:allTests`

Expected: all exported presets meet deterministic audibility/headroom bounds. Do not weaken a bound to conceal NaN, silence, or clipping.

### Task 12: Author documentation and compile examples

**Files:**
- Modify: `docs/miniapp/audio/kotlin-dsl.md`
- Modify: `docs/miniapp/audio/instruments.md`
- Modify: `docs/miniapp/audio/patterns.md`
- Modify: `docs/miniapp/audio/effects.md`
- Modify: `docs/miniapp/audio/adaptive-music.md`
- Modify: `docs/miniapp/audio/performance-budgets.md`
- Modify: `docs/miniapp/audio/troubleshooting.md`
- Modify: `.agents/skills/miniapp-procedural-audio/references/public-api.md`
- Modify: `.agents/skills/miniapp-procedural-audio/references/music-recipes.md`
- Modify: `.agents/skills/miniapp-procedural-audio/references/review-checklist.md`
- Modify: `miniapp/audio-presets/src/commonTest/kotlin/ge/yet/game/miniapp/audio/presets/AuthorDocumentationSnippetTest.kt`

- [ ] **Step 1: Document exact APIs and budgets**

Add velocity, seeded timing, partial envelopes, note following, bus dynamics, tonality, arrangements, processor-count accounting, and the prohibition on arbitrary realtime callbacks. State that seeds are explicit and examples are original.

- [ ] **Step 2: Compile every new example**

Mirror each documentation snippet in the existing common test compilation fixture, including one full bus chain and one sectioned tonal phrase.

- [ ] **Step 3: Verify docs contract**

Run: `./gradlew :miniapp:audio:allTests :miniapp:audio-presets:allTests`

### Task 13: Platform and boundary verification

**Files:**
- Modify only if a failing platform test identifies a real regression; no speculative platform edits.

- [ ] **Step 1: Run focused common verification**

Run: `./gradlew :core:pattern:allTests :miniapp:audio:allTests :miniapp:audio-presets:allTests :game:blockblast:allTests`

- [ ] **Step 2: Run Android host and compilation verification**

Run: `./gradlew :miniapp:audio:testAndroidHostTest :miniapp:audio:compileAndroidMain :miniapp:audio-presets:compileAndroidMain :game:blockblast:compileAndroidMain`

- [ ] **Step 3: Run iOS producer/callback and compilation verification**

Run: `./gradlew :miniapp:audio:compileKotlinIosSimulatorArm64 :miniapp:audio-presets:compileKotlinIosSimulatorArm64 :game:blockblast:compileKotlinIosSimulatorArm64`

Expected: the iOS callback remains ring-drain/copy/atomic-diagnostics only; all new scheduling and DSP stay producer-side.

- [ ] **Step 4: Verify dependency and shipping boundaries**

Run: `./gradlew :game:blockblast:validateMiniAppDependencies :miniapp:bundle:verifyMiniAppBundle verifyMiniApp`

- [ ] **Step 5: Inspect final worktree without committing**

Run: `git diff --check` and `git status --short`

Expected: only planned audio/docs files plus the user's pre-existing unrelated changes appear. No commit is created.

## Explicitly deferred work

- Look-ahead or multiband dynamics: latency/state complexity is disproportionate for mobile game audio.
- FFT/convolution reverb: memory and CPU budgets conflict with fixed low-latency blocks.
- Per-sample filter coefficient rebuild: unnecessary cost; block-rate refresh is sufficient.
- Arbitrary author callbacks or mutable RNGs: they defeat validation, determinism, and realtime allocation guarantees.
- Dynamic tempo maps and sample-accurate automation graphs: scheduler redesign with little immediate musical gain.
- General chord/voice-leading composition framework: outside the small tonal helper's responsibility.
- Sample playback or external audio engines: new dependencies, asset lifecycle, and licensing surface are unjustified.
