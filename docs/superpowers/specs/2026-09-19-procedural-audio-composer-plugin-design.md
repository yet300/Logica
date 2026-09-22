# Procedural Audio Composer Plugin Design

**Date:** 2026-09-19  
**Status:** approved architecture, pending implementation review  
**Scope:** repository-local agent guidance for composing original procedural music and SFX through the public MiniApp audio API

## Goal

Give any agent working in BlockBlast a discoverable, repeatable creative process for turning a game's emotional and interaction brief into original, deterministic Kotlin audio declarations. The guidance must aim beyond technically valid beeps: it should help an agent design motifs, timbral identities, contrast, pacing, section form, adaptive variation, mix hierarchy, and readable game feedback.

The plugin does not add another synthesis engine, parser, scheduler, or musical notation language. It teaches agents to use the existing public `:miniapp:audio`, `:core:pattern`, and `:miniapp:audio-presets` surfaces well.

## Hard originality boundary

Klang, Strudel, Sprudel, commercial recordings, game soundtracks, public-domain melodies with recognizable arrangements, and third-party demos may be studied only at the level of general concepts. An agent must not copy or closely translate their:

- notes, scale-degree strings, chords, rhythms, section timing, orchestration, or arrangement;
- synthesis values, effect settings, automation curves, seeds, or preset recipes;
- source code, DSL chains, comments, or naming structure.

The supplied Klang examples establish the desired expressive range only: layered voices, evolving form, role-specific processing, humanized timing and velocity, seeded modulation, and coherent bus treatment. Plugin examples and exercises must be authored from a blank musical brief and use independently chosen material.

## Repository layout

```text
.agents/
├── skills/
│   └── procedural-audio-composer/
│       └── SKILL.md                       # universal repository entrypoint
└── plugins/
    ├── marketplace.json                  # repository-local Codex catalog
    └── plugins/
        └── funfolio-procedural-audio-composer/
            ├── .codex-plugin/plugin.json
            └── skills/
                └── funfolio-procedural-audio-composer/
                    ├── SKILL.md           # canonical workflow
                    ├── agents/openai.yaml
                    └── references/
                        ├── composition-workflow.md
                        ├── music-direction.md
                        ├── sfx-direction.md
                        └── evaluation-rubric.md
```

The plugin skill is the canonical source. Its skill name is `funfolio-procedural-audio-composer`. The short `.agents/skills/procedural-audio-composer` entrypoint uses a distinct name to avoid a duplicate-name collision when the plugin is installed, and explicitly loads the canonical skill by repository-relative path. This lets agents that understand the generic `.agents/skills` convention use the workflow without installing a Codex plugin, while Codex can install and present the complete plugin through the repository marketplace.

The plugin contains no MCP server, app connector, hook, executable script, network dependency, or copied audio asset. Its only capability is maintained agent guidance.

## Skill routing

The entry description triggers for requests to compose, substantially redesign, art-direct, or evaluate original procedural music and SFX for a MiniApp. It does not trigger for low-level DSP, platform sink, lifecycle, or audio-engine work.

The canonical skill requires the existing `miniapp-procedural-audio` skill for repository architecture, public API constraints, budgets, lifecycle, deterministic testing, and review. The composer skill owns creative decisions and the iterative listening/rendering process; it does not duplicate the API manual.

References are loaded progressively:

- `composition-workflow.md` for any new soundtrack or substantial redesign;
- `music-direction.md` only for music, ambience, motifs, harmony, or arrangement;
- `sfx-direction.md` only for interaction cues and sonic feedback systems;
- `evaluation-rubric.md` before accepting either kind of work.

## Creative workflow

### 1. Convert product intent into an audio brief

Record the gameplay context, emotional arc, session length, repetition rate, important game states, required semantic cues, desired density, and what must remain audible on small mobile speakers. References are translated into abstract adjectives and functional observations, never musical material.

### 2. Establish an original identity

Choose a compact sound world: tonal center or non-tonal rule, interval vocabulary, rhythmic behavior, timbral materials, stereo character, and a deliberate contrast strategy. Create original motifs and rhythms before selecting transforms. Seeds are treated as authored constants, not hidden randomness.

### 3. Design roles before tracks

Each layer must have a job such as pulse, bass foundation, harmonic color, foreground motif, environmental bed, or transition accent. Layers without a distinct perceptual or gameplay function are removed. The role plan must fit mobile voice and processor budgets before Kotlin is written.

### 4. Author timbres and material

Try reusable presets first. When a game-owned voice is justified, build it from a small number of complementary oscillator/noise layers, envelope each spectral region deliberately, use note-following filters where register consistency matters, and leave headroom. Musical material uses the pattern and tonal helpers rather than embedded third-party notation.

### 5. Compose macro-form and adaptation

Use section mute/transposition, density changes, seeded degradation, velocity/microtiming humanization, controls, and slow parameter motion to produce an arc. Variation must preserve identity; randomization is not a substitute for composition. Scheduler redesign is outside scope.

### 6. Integrate SFX as part of the score

Create a semantic cue matrix before individual sounds. Related cues share a timbral family, while success, failure, placement, warning, reward, and destructive actions remain distinguishable by transient, register, duration, brightness, and tail. Test cues over the densest music section and under rapid repetition.

### 7. Render, judge, and iterate

Compile through the public API and render through `MiniAppAudioTestRenderer`. Check determinism, finite PCM, audibility, peak ceiling, processor/voice budgets, and loop behavior. Then apply the perceptual rubric: role clarity, motif identity, emotional fit, contrast, fatigue, repetition tolerance, mobile translation, and SFX intelligibility. Change one musical axis per iteration so improvements remain attributable.

## Output contract for an agent

A completed composition task should produce:

1. a short creative brief and role/section map;
2. immutable game-owned Kotlin declarations using only public API and presets;
3. explicit, original seeds and bounded parameters;
4. session integration through `MiniAppAudio`, with rejection handling;
5. deterministic offline-render tests with acoustic assertions;
6. a concise listening/evaluation note recording what was verified and any remaining perceptual risk.

The agent must implement and verify the work, not stop after suggesting pseudocode or a mood board unless the user explicitly requests ideation only.

## Failure handling

- If the desired sound cannot be expressed publicly, document the exact authoring gap and stop before importing `internal` APIs or adding an external engine.
- If a render exceeds budgets or clips, simplify layers/effects and rebalance before proposing a budget increase.
- If a reference is recognizable in the result, discard the affected musical material and regenerate it from the abstract brief.
- If listening playback is unavailable, report that limitation explicitly and rely on render invariants plus structural analysis; do not claim subjective sonic quality was auditioned.
- If user intent leaves genre or emotional direction genuinely ambiguous, ask one focused question before composing.

## Validation strategy

### Behavioral skill test

Use an isolated temporary workspace and a realistic request for an adaptive puzzle-game score plus a small SFX family.

1. Run a baseline agent without the composer skill and record concrete omissions or unsafe choices.
2. Run the same request with the skill available.
3. Verify that the result uses an original brief, explicit roles and sections, public APIs only, deterministic seeds, bounded effects/voices, render assertions, and an evaluation note.
4. Verify that it does not translate any supplied Klang musical or parameter material.

This is a process test, not a contest to imitate the reference track.

### Static validation

- Run `quick_validate.py` for the canonical skill and repository entrypoint.
- Run `validate_plugin.py` for the plugin root.
- Resolve every repository-relative link from the entrypoint and every skill-relative reference.
- Confirm the plugin manifest declares only `skills` and valid interface metadata.
- Confirm the marketplace entry uses a relative local source, `AVAILABLE`, `ON_INSTALL`, and category `Developer Tools`.

### Repository validation

- Compile any Kotlin declaration shown in maintained author documentation through `commonTest`.
- Run `:core:pattern:allTests`, `:miniapp:audio:allTests`, `:miniapp:audio-presets:allTests`, and the affected game tests.
- Compile the affected Android and iOS simulator targets and run dependency-boundary validation.

## Commit structure

The implementation lands separately from the already committed engine work:

1. plugin scaffold, manifest, marketplace entry, and universal repo entrypoint;
2. canonical creative workflow and music/SFX/evaluation references;
3. behavioral/static validation fixes and any documentation links required for discoverability.

No unrelated staged or untracked files enter these commits.
