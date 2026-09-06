# Logica Super-App Store Screenshot Design

## Objective

Create a conversion-focused English screenshot system for the current Logica
super-app architecture. The assets must position Logica as a growing home for
thoughtful puzzle games rather than as a single Block Blast title, while
remaining accurate when more games are added later.

The deliverables cover:

- Apple App Store iPhone screenshots;
- Apple App Store iPad screenshots;
- Google Play phone screenshots;
- one Google Play Feature Graphic at 1024 x 500 pixels.

No asset will be uploaded to either store as part of this work.

## Product Context

Logica currently ships three games:

- Block Blast;
- 2048;
- Fruit Merge.

The screenshot story must not use a fixed game count in its positioning. The
catalog and the varied game screens provide evidence of breadth, while the copy
describes Logica as a growing puzzle collection.

## Audience

The primary audience is broad casual-puzzle players who want calm, polished,
thoughtful play without an aggressive arcade presentation. The creative should
make the collection feel approachable while preserving enough strategic depth
to interest experienced puzzle players.

## Positioning

The approved positioning is:

> A growing world of calm, thoughtful puzzles in one app.

The sequence communicates four benefits:

1. Logica is a growing puzzle destination, not one isolated game.
2. Players can choose between meaningfully different puzzle mechanics.
3. Each game is easy to enter but rewards thought and improvement.
4. The collection shares one restrained, polished product experience.

Avoid numerical claims about the number of games, downloads, ratings, awards,
or future release cadence.

## Visual Direction

Use the approved **Quiet Editorial** direction:

- near-black `#171715` as the dominant surface;
- warm cream typography and restrained light surfaces;
- terracotta and muted sage accents;
- large editorial serif headlines;
- compact uppercase sans-serif supporting labels;
- real application UI inside clean device frames or large frameless crops;
- subtle depth, soft shadows, and generous negative space;
- game-owned colors preserved inside screenshots;
- no fabricated, redrawn, or AI-generated application interface.

The deck should feel like one brand while allowing Block Blast, 2048, and Fruit
Merge to retain their individual visual identities.

## Seven-Slide Narrative

### Slide 1 - Collection Hero

- Headline: **A growing world of puzzles.**
- Screen: the Logica catalog with the current games visible.
- Layout: large hero composition with a prominent catalog device.
- Purpose: immediately reposition Logica as a puzzle collection.

### Slide 2 - Choice

- Headline: **Choose your next challenge.**
- Screen: a closer catalog view in which Block Blast, 2048, and Fruit Merge are
  recognizable.
- Layout: device-bottom composition with restrained supporting decoration.
- Purpose: show that moving between games is direct and understandable.

### Slide 3 - Block Blast

- Headline: **Clear lines. Find your flow.**
- Screen: an active Block Blast position immediately before or after a
  satisfying line clear.
- Layout: device-top or large frameless game crop.
- Purpose: sell calm spatial planning rather than document controls.

### Slide 4 - 2048

- Headline: **Merge numbers. Think ahead.**
- Screen: a developed 2048 board with a clearly visible higher-value tile.
- Layout: a lightly tilted or floating device with subtle shadow.
- Purpose: communicate familiar mechanics and strategic progression.

### Slide 5 - Fruit Merge

- Headline: **Drop fruit. Grow bigger.**
- Screen: a colorful mid-to-late Fruit Merge state with several fruit levels.
- Layout: device-bottom or large UI crop, distinct from Slide 4.
- Purpose: introduce a visually different, playful puzzle loop.

### Slide 6 - Breadth

- Headline: **Different games. One thoughtful home.**
- Screen: a controlled mosaic using real captures from all three games.
- Layout: phone/UI mosaic rather than another single-device hero.
- Purpose: unify the collection without anchoring the campaign to a fixed game
  count.

### Slide 7 - Close

- Headline: **Your next puzzle awaits.**
- Screen: app icon plus a calm catalog composition.
- Layout: sparse brand close with a clear visual endpoint.
- Purpose: finish with an invitation to open Logica and choose a game.

## Layout Rhythm and Connected Canvas

No two adjacent slides may repeat the same layout. The sequence should move
through hero, bottom-device, top-device or frameless crop, floating device,
large crop, mosaic, and sparse brand close.

Use one connected-canvas moment between Slides 5 and 6. Non-critical blocks,
number tiles, or fruit may bleed 10-30% across the boundary. Text, device UI,
faces, claims, and important game state must remain fully contained in one
export. Every exported image must still work as an independent advertisement.

## Capture Strategy

Capture current application UI directly from simulators:

- iPhone 11 Pro Max for the iPhone source deck;
- an available modern iPad simulator for the iPad source deck.

The minimum source set for each device is:

1. catalog with all current games visible;
2. strong Block Blast gameplay state;
3. developed 2048 gameplay state;
4. colorful Fruit Merge gameplay state.

Additional captures may be taken when needed for the catalog close, mosaic, or
brand close. The app must be captured without test-mode overlays, advertising
banners, permission dialogs, debug chrome, or unrelated sheets.

The user approved using the iPhone/iPad captures as the UI source for Google
Play because the product UI is identical. Google Play assets must nevertheless
be recomposed for their own aspect ratios; final Apple assets must not simply
be stretched or blindly cropped.

## Editor Architecture

Use the installed `app-store-screenshots` skill and its supplied Next.js editor
template. Keep the editor isolated under:

```text
store-assets/screenshot-editor/
```

This prevents JavaScript dependencies and configuration from polluting the
Kotlin Multiplatform repository root. The editor's canonical state is:

```text
store-assets/screenshot-editor/app-store-screenshots.json
```

Source captures and uploaded assets remain inside the editor's `public/`
hierarchy. Exported deliverables and review contact sheets remain under
`store-assets/` in clearly named platform/device directories.

The initial project state will seed:

- app name `Logica`;
- locale `en` only;
- the custom Quiet Editorial theme;
- connected canvas enabled for this new deck;
- seven slides for iPhone, iPad, and Google Play phone;
- the Play Store Feature Graphic deck.

## Platform Composition

### App Store iPhone

- Source UI captured on iPhone 11 Pro Max at 1242 x 2688 pixels.
- Seven portrait PNG files at each editor export size: 1320 x 2868 (6.9-inch),
  1284 x 2778 (6.5-inch), 1206 x 2622 (6.3-inch), and 1125 x 2436
  (6.1-inch).
- The 1320 x 2868 set is the primary App Store delivery. The remaining sets are
  compatibility exports and must preserve the same composition after scaling.

### App Store iPad

- Source UI captured on a modern 13-inch iPad simulator at 2064 x 2752 pixels.
- Seven portrait PNG files at 2064 x 2752 (13-inch) and 2048 x 2732
  (12.9-inch).
- Use the tablet's adaptive layout rather than enlarging the phone UI.

### Google Play Phone

- Seven portrait PNG files.
- Use the same approved English story and Quiet Editorial system.
- Recompose for 1080 x 1920 rather than stretching an Apple export.

### Google Play Feature Graphic

- One opaque RGB PNG at exactly 1024 x 500.
- Use Logica branding, the collection positioning, and restrained visual cues
  from the three current games.
- Do not place a device frame in the feature graphic.

## Localization

The first pass is English only. Store copy must be saved under locale `en` and
the editor locale selector may remain hidden for the single-locale project.
The deck structure and `{locale}`-compatible asset paths should remain ready for
future localization without changing the narrative architecture.

## Quality Gates

### Message Quality

- One primary idea per slide.
- The hero communicates a growing puzzle collection in one second.
- Headlines remain readable at approximately 160 pixels wide.
- No fixed count makes the deck obsolete when more games ship.
- No unsupported proof or comparative claim appears.

### Visual Quality

- Quiet Editorial styling is consistent across the full deck.
- No adjacent slides repeat the same layout.
- The game UI remains real and unaltered.
- At least one restrained contrast treatment creates rhythm.
- The Slides 5-6 connected moment is deliberate and non-essential to
  understanding either individual crop.
- iPad assets use genuine adaptive tablet UI.

### Export Quality

- Every final asset has the exact target dimensions.
- Every PNG is opaque RGB with no blank or transparent edge pixels.
- Text and device frames are not clipped.
- Source screenshots align correctly inside their frames.
- Filenames are zero-padded and sort in narrative order.
- The Feature Graphic is exactly 1024 x 500 and contains no device frame.

### Build and Review

- The screenshot editor installs and builds successfully with the detected
  package manager.
- The editor starts locally and loads the seeded decks without migration or
  missing-asset warnings.
- A contact sheet is produced for final visual review.
- The final report lists the simulator devices used, capture gaps if any,
  editor build command, and generated output paths.

## Non-Goals

- Uploading assets to App Store Connect or Google Play Console.
- Submitting, publishing, or rolling out a release.
- Creating localized decks beyond English in this pass.
- Changing game UI or adding screenshot-only behavior to production code.
- Advertising a fixed count of games.
