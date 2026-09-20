# Falling Blocks provenance

**Shipping status:** NOT ALLOWLISTED

## Authorship

The Kotlin, Compose, procedural visuals and procedural audio for this MiniApp
are authored for the Logica repository by its contributors with implementation
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
  and effects are declared through Logica's procedural-audio API.

## Assets and limitations

No third-party image, font or bundled audio asset is introduced. Initial UI
copy is English only. The generated catalog icon is scaffold infrastructure,
not a claim of a final product icon. Production shipping requires a separate
maintainer review and explicit allowlist decision.
