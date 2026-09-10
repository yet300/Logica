# Visual, motion and sound direction

## UI/UX

Inspect current host theme/components and representative real app screens; prefer codebase graph discovery for source. Do not assume a generic “canonical” web skill, marketing page or another game's entire theme matches Logica. Document host constraints versus game-local decisions. The board is the primary content; remove repeated titles, explanatory paragraphs, debug values and decorative panels from the play screen. Use contextual instruction and an interactive first move where appropriate. Keep labels where an icon alone would be ambiguous; accessible names are mandatory even for visually icon-only controls.

Use one coherent, approved source family for interface icons. Prefer Google Material Symbols from https://fonts.google.com/icons or an explicitly approved alternative. Verify the actual asset page/license and repository-compatible import path. Record source URL, icon name/variant, license and conversion. Do not draw replacement interface icons with paths, Canvas, AI images, emoji or Unicode glyphs. Missing icon/tool access means a sourced alternative or an explicit unresolved asset, not silent invention. Game geometry such as arrow paths is game content, not an excuse to hand-draw toolbar symbols. The user's ban also applies to product/catalog icon generation; do not invoke the contributor guide's Quiver workflow without explicit user authorization superseding that ban.

Create a screen/state inventory and a small token sheet: colors, text roles, spacing, shape, icon size/stroke and board treatment. Compare compact and wide samples side by side with the agreed references. Inspect actual target-device sizes, contrast, text scaling, hit targets, safe areas and host banner variants. Do not turn mobile puzzle UI into website cards and giant headings. Do not duplicate host chrome.

## Motion

Specify each transition as trigger → visible change → duration/easing → interruption/input rule → reduced-motion alternative. Cover selection, valid move, blocked move, hint, completion and next puzzle as applicable. Use video timestamps for observed motion; label invented timings as proposed. Motion should reveal causality, remain responsive under rapid input and never create accidental double moves. Show a recording or interactive sample; a static screenshot cannot validate motion. Match audio transients to semantic events, avoiding replay on recomposition.

## Procedural sound

Use `miniapp-procedural-audio` for exact API and preset authoring order. This document specifies experience, not new DSP capabilities. Confirm controls actually exist before proposing implementation.

Before final implementation, write an audio palette with separate music and SFX rows:

| Role | Trigger/state | Timbre/envelope | Rhythm/register | Variation/control | Repetition policy |
|---|---|---|---|---|---|

Choose roles that the game really uses: movement, blocked action, hint, undo, success, failure and ambient/music states. Do not add events just to fill the table. Critical semantic roles should remain distinguishable at low volume; success and error must not be the same beep at different pitches by default.

Reuse presets first, tune public controls, compose renamed presets, then write original declarations only for unmet roles. Variety can come from instrumentation, rhythmic density, register, envelope, melodic contour, space and intentional silence. Seed changes alone are not proof of audible variety. Avoid unconstrained randomness and incompatible layers. Use deterministic seeds and original motifs. Do not copy reference soundtracks.

Offer two meaningfully different short original audio directions when sound preference is unresolved. Present playable previews and ask which characteristics to keep. After selection, render/listen to a representative loop through its boundary and rapid SFX over music, plus enough sustained playback to assess fatigue. Compare with existing shipped game audio when accessible to catch reuse of the same sonic identity. Document what was actually listened to, by whom/tool, and what remains subjective. Do not claim listening from waveform inspection.

Compile declarations in common tests. Test rejection handling, repetition limits where relevant, non-silent output when expected and finite/clipping-safe output using supported render tooling. Use deterministic acoustic assertions for new voices/presets. Numerical checks cannot establish that the music is pleasant. Host settings/visibility/teardown own suppression; no new game-level audio player, native sink or redundant Settings control.
