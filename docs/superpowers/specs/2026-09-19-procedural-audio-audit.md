# Procedural audio audit — 2026-09-19

Confidence: high. Every claim below is tied to repository source or a test. This audit describes the tree after the approved expressive-audio implementation; items marked “closed” were gaps in the pre-change tree and are now covered by the cited code.

## DSL coverage against the allowed Klang-level concepts

Klang is used only as a vocabulary of general synthesis/composition ideas. No Klang source, preset values, notes, rhythms, arrangements, or seeds were used.

| Technique | Status | Repository evidence |
|---|---|---|
| sine/triangle/saw/square/pulse | present | `miniapp/audio/src/commonMain/kotlin/ge/yet/game/miniapp/audio/AudioDeclarations.kt:6`, author entry at `AudioProgramDsl.kt:99` |
| white/pink/brown seeded noise | present | `AudioDeclarations.kt:7`, `AudioProgramDsl.kt:104` |
| additive partials | present | `AudioProgramDsl.kt:108`; local partial ADSR added at `AudioProgramDsl.kt:113` |
| FM and vibrato | present | `AudioProgramDsl.kt:122`, `AudioProgramDsl.kt:127` |
| voice ADSR | present | `AudioProgramDsl.kt:163` |
| low/high/band-pass | present | `AudioProgramDsl.kt:132`, `AudioProgramDsl.kt:139`, `AudioProgramDsl.kt:146` |
| distortion / bit crush | present | `AudioProgramDsl.kt:153`, `AudioProgramDsl.kt:158` |
| delay / reverb sends | present | `AudioProgramDsl.kt:279`, `AudioProgramDsl.kt:284` |
| `pure/sequence/stack/euclidean/choose` | present | implementations under `core/pattern/src/commonMain/kotlin/ge/yet/game/pattern/Patterns.kt`, `EuclideanPattern.kt`, and `PatternRandom.kt` |
| `degrade/shift/slow/fast/repeat/every` | present | `PatternRandom.kt:21`, `PatternTransforms.kt:3`, `PatternTransforms.kt:19`, `PatternTransforms.kt:40`, `PatternTransforms.kt:61`, `PatternTransforms.kt:66` |
| seeded microtiming / swing | present, closed in this change | `PatternTimingTransforms.kt:3`, `PatternTimingTransforms.kt:35` |
| per-note velocity | present, closed | `AudioDeclarations.kt:9`; declaration helper `HumanizedNotes.kt:6`; scheduler primitive storage in `internal/AudioScheduler.kt:24` |
| note-following filters | present, closed | `AudioDeclarations.kt:52`, public constructor at `AudioDeclarations.kt:86`; block-rate evaluation in `internal/dsp/Voice.kt` |
| separately damped upper modes | present, closed | partial ADSR at `AudioProgramDsl.kt:113`; preallocated partial envelopes in `internal/dsp/Voice.kt` |
| master compressor / limiter | present, closed | declarations at `AudioDeclarations.kt:204`; author DSL at `AudioProgramDsl.kt:292`; linked-stereo DSP in `internal/dsp/Effects.kt` |
| tonal helpers | present, closed | `Tonality.kt` (`ScaleMode`, `tonalScale`, checked signed degree mapping) |
| section mute / timed transposition | present, closed | author API `AudioProgramDsl.kt:217` and `AudioProgramDsl.kt:244`; existing-scheduler mapping in `internal/AudioScheduler.kt:171` |

## Performance and realtime boundaries

- The renderer owns fixed voice slots, per-voice scratch, stereo bus buffers, 16 track buffers, scheduler, and bounded effect-state pools before `render`: `internal/RealtimeAudioRenderer.kt:31-58`. Its block method clears/reuses those arrays and traverses lists by index: `RealtimeAudioRenderer.kt:90-124`, `RealtimeAudioRenderer.kt:218-321`.
- `VoiceAllocator.allocateRealtime` returns primitives and searches fixed primitive arrays. Its victim selection is O(`MAX_VOICES`) and deterministic: `internal/VoiceAllocator.kt:71-100`, `VoiceAllocator.kt:142-175`. The allocating snapshot/result API remains outside the renderer hot path.
- Scheduling uses one reusable `ScheduledAudioEventBuffer`, one `PatternEventBuffer`, and one `PatternQueryBudget`: `internal/AudioScheduler.kt:128-132`. Sorting is bounded insertion sort over the fixed event cap: `AudioScheduler.kt:73-100`. The worst case is quadratic in scheduled events, but the event budget caps it at 256 per track query; replacing it is not currently justified.
- `DefaultMiniAppAudio` previously compiled the same immutable program on every `playMusic` and `playSfx`. It now caches the most recent successful compilation by referential identity in `internal/DefaultMiniAppAudio.kt` (`compileCached`), proven by `MiniAppAudioEngineTest`.
- Android owns rendering on its writer thread. It consumes a bounded command batch under `tryLock`, renders into preallocated channel arrays, interleaves into preallocated float/PCM16 arrays, and uses blocking `AudioTrack.write`: `androidMain/.../AndroidAudioSink.kt:173-186`, `AndroidAudioSink.kt:262-281`, `AndroidAudioSink.kt:297-305`, framework blocking write at `AndroidAudioSink.kt:408-411`.
- Android loss pauses and flushes output; policy pause also abandons focus. Ducking changes track volume, focus denial retries on a timer, and full inactivity releases focus: `AndroidAudioSink.kt:187-252`.
- iOS DSP and command consumption remain producer-side: `iosMain/.../IosPcmProducer.kt:188-230`. The producer writes rendered PCM into the fixed stereo ring at `IosPcmProducer.kt:210-214`.
- The native `AVAudioSourceNode` callback delegates only to the callback adapter: `iosMain/.../IosAudioSink.kt:469-476`. The adapter uses preallocated left/right arrays and calls the ring-backed source, then copies samples to native output: `IosAudioSink.kt:512-553`. The callback source only performs `ring.readOrSilence` plus atomic diagnostics: `IosPcmCallbackSource.kt:14-35`. It does not acquire lifecycle/producer locks and does not call the scheduler or renderer.
- iOS route/media/interruption reconciliation is generation-based and serial: `IosAudioSink.kt:233-274`; start waits for producer prefill before starting native output at `IosAudioSink.kt:277-285`.

## Determinism and tests

- All random pattern/parameter APIs require an explicit seed (`noise`, `choose`, `degrade`, `smoothNoise`, `humanizedNotes`, `humanize`). Production programs and presets found by the audit pass explicit or deterministically derived seeds.
- The public test renderer now uses `RealtimeAudioRenderer`; the obsolete second DSP implementation was removed. Equality of the convenience and typed request paths is asserted in `MiniAppAudioTestRendererTest`.
- Deterministic tests cover timing transforms, velocity scheduling/rendering, tonal mapping, note-follow filtering, local partial damping, sections, realtime delay, linked dynamics, processor overflow, and compilation caching. Block Blast renders a complete eight-cycle `grove_marimba` arrangement twice and checks hash, finiteness, audibility, and ceiling in `game/blockblast/.../BlockBlastAudioTest.kt`.
- Tests deliberately prefer acoustic invariants over exact cross-platform float snapshots. Exact hashes are used only to compare repeated renders from the same target.

## Legacy and duplication

- The unused `OfflineAudioRenderer` and its separate effect semantics were dead/duplicated engine code and have been removed. `MiniAppAudioTestRenderer` is now the single offline entry into the production realtime mixer.
- No runtime `AudioRepository` or bundled-file player remains in Block Blast; repository search finds that name only in test fake class names. The `voice_*.mp3` occurrence in `BlockBlastSfx.kt` is historical prose, not a file dependency.
- Game-owned audio declarations are distinct and game-semantic. Reusable `PowerUp` and `SuccessSweep` are composed from `:miniapp:audio-presets` in `game/blockblast/.../BlockBlastAudio.kt`; there is no copied implementation of those presets in the game.
- Some game SFX use similar elementary oscillator/envelope recipes. Moving every small two-line voice into presets would reduce local clarity and create parameterized abstractions without a demonstrated second consumer; this is not a DRY violation worth “fixing”.

## Remaining gaps / explicitly not doing

- No convolution reverb, FFT spectral processors, oversampled nonlinear chain, sample streaming, arbitrary routing graph, per-sample automation graph, or unbounded voice/effect creation. These multiply memory/CPU risk and platform-realtime complexity without improving this puzzle game's core musical result proportionally.
- No general DAW-like timeline or scheduler rewrite. The bounded section list already supplies mute/transposition form inside the existing exact scheduler.
- No attempt to reproduce Klang's `tones` API surface. The local tonal API is intentionally small and original.
- No look-ahead limiter. The linked zero-look-ahead limiter plus final safety limiter is predictable and bounded; true look-ahead would add latency and another delay buffer per bus.
- SFX effect tails do not keep the platform output alive after the last SFX voice by themselves. Current game SFX are short and the final samples remain bounded. If long standalone SFX reverb tails become a product requirement, add an explicit bounded tail-active counter rather than making the sink run forever.
