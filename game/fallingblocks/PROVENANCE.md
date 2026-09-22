# Falling Blocks provenance

**Shipping status:** NOT ALLOWLISTED

## Authorship

The Kotlin, Compose, procedural visuals and procedural audio for this MiniApp
are authored for the Funfolio repository by its contributors with implementation
assistance from OpenAI Codex.

## References

- <https://github.com/yet300/TetrisLite> was inspected for falling-tetromino
  domain behavior. Its repository license file is Apache-2.0. No source file,
  UI, artwork or audio is copied verbatim; the new engine is independently
  implemented and corrects the issues recorded in the approved design.
- <https://www.sinasamaki.com/creating-a-crt-screen-effect-in-jetpack-compose/>
  is a technical reference for layer recording, scanlines and glow. The final
  Compose implementation is original, theme-derived and omits continuous
  jitter.

- The user-provided tutorial screenshots are interaction-staging references.
  The photographed/3D hand is not imported; the game uses Material Symbols and
  original Compose-drawn gesture trails.
- The user-provided Sprudel text is an aesthetic and layering reference only.
  Its recognizable melody, rhythm and parameter sequence are excluded. Music
  and effects are declared through Funfolio's procedural-audio API.

## Assets and limitations

No third-party image, font or bundled audio asset is introduced. Initial UI
copy is English only. The generated catalog icon is scaffold infrastructure,
not a claim of a final product icon. Production shipping requires a separate
maintainer review and explicit allowlist decision.

## Procedural visual authorship

The hard-drop trail beam with halftone fade and speed-line hatching,
landing/border pulse, exact-row flash, shock line, per-row cascade ripple,
lean shimmer, shockwave rings and deterministic square-particle declarations
are original Compose drawing code authored for this MiniApp. They use captured
engine facts and normalized 10×20 board geometry; no shader, artwork,
animation sequence or effect implementation was copied verbatim from Block
Blast or the CRT article. Block Blast's particle-burst/shockwave language
informed the visual goals and was reimplemented with deterministic
theme-derived parameters, a single board-level animation controller and
reduced-motion fallbacks. A previously explored slice-glitch overlay was
removed per playtest feedback in favor of the piece-colored drop beam.

## Procedural audio authorship

The 126 BPM program uses an original bass, pulse and degraded lead motif with
fixed game-owned seeds. It composes the repository's public `AnalogBass`,
`ChipLead`, `WoodenPlacementThock`, `SuccessSweep` and `PowerUp` fragments with
original Falling Blocks SFX declarations. The supplied Sprudel notes, rhythmic
sequence and parameter values were not transcribed.

Deterministic render tests cover base/high intensity, every typed SFX, line-clear
families, headroom, finite PCM, DC offset and a bounded mobile declaration
budget. These automated renders were not exported or represented as a human
listening review.
