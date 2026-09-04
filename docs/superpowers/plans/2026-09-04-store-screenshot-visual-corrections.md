# Store Screenshot Visual Corrections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve Feature Graphic contrast, use heavier SF-style marketing typography, remove the Android camera treatment, and eliminate the Android screen-3 overlap.

**Architecture:** Keep the existing screenshot editor and canonical deck state. Add a source-level visual contract check, change shared canvas presentation primitives, then store one Android-only transform in the deterministic seed state. Re-export only Android Phone and Feature Graphic, normalize them through the existing RGB verifier, and rebuild the contact sheet.

**Tech Stack:** Next.js 15, React, TypeScript, html-to-image, Playwright CLI, Pillow, Node.js verification scripts.

---

### Task 1: Add the Visual Regression Contract

**Files:**
- Create: `store-assets/screenshot-editor/scripts/verify-visual-contract.mjs`
- Modify: `store-assets/screenshot-editor/package.json`

- [ ] **Step 1: Write the failing source and state verifier**

Create a Node script that reads `device-frames.tsx`, `slide-canvas.tsx`, and `app-store-screenshots.json`. Require the exact orange gradient colors `#862B18` and `#C84E25`, the font family token `SF Pro Display`, headline weight `900`, no camera marker inside `AndroidPhone`, and this Android screen-3 transform:

```json
{
  "device": {
    "x": 252,
    "y": -96,
    "width": 576,
    "height": 1248,
    "zIndex": 3
  }
}
```

Add `"verify:visual": "node scripts/verify-visual-contract.mjs"` to `package.json`.

- [ ] **Step 2: Confirm the verifier fails**

Run:

```bash
rtk bun run verify:visual
```

Expected: failure because the current code still uses Inter, a camera dot, and the old gradient.

- [ ] **Step 3: Commit the failing contract**

```bash
rtk git add store-assets/screenshot-editor/scripts/verify-visual-contract.mjs store-assets/screenshot-editor/package.json
rtk git commit -m "test: define store screenshot visual corrections"
```

### Task 2: Correct Typography, Feature Graphic, and Android Frame

**Files:**
- Modify: `store-assets/screenshot-editor/src/components/editor/slide-canvas.tsx`
- Modify: `store-assets/screenshot-editor/src/components/editor/device-frames.tsx`

- [ ] **Step 1: Introduce the marketing font stack**

Add one canvas-only constant:

```ts
const MARKETING_FONT_FAMILY =
  '"SF Pro Display", -apple-system, BlinkMacSystemFont, "Helvetica Neue", sans-serif';
```

Apply it to `Caption` and `FeatureGraphicCanvas`. Set headline `fontWeight` to `900`, `lineHeight` to `0.94`, and label weight to `700`. Editor controls remain on Inter.

- [ ] **Step 2: Replace the Feature Graphic colors**

Use:

```ts
background: "linear-gradient(125deg, #862B18 0%, #A93B1E 52%, #C84E25 100%)"
```

Set both app name and tagline to `#FFFFFF`; keep the tagline fully opaque.

- [ ] **Step 3: Simplify the Android phone shell**

Delete the camera-dot element from `AndroidPhone`. Use a matte `#202023` shell, a subtle single inner highlight, `1.8%` horizontal and `1.1%` vertical screen insets, and a clean screen radius. Do not add a notch, punch-hole, speaker, buttons, or Pixel-specific hardware.

- [ ] **Step 4: Run the editor build**

```bash
rtk bun run build
```

Expected: TypeScript and production build pass.

### Task 3: Store the Android-Only Screen-3 Correction

**Files:**
- Modify: `store-assets/screenshot-editor/scripts/seed-project.mjs`
- Modify: `store-assets/screenshot-editor/app-store-screenshots.json`

- [ ] **Step 1: Add a device-specific transform hook**

Extend `buildDeck` so only `device === "android"` and `item.id === "03-blockblast"` receives:

```js
transforms: {
  device: { x: 252, y: -96, width: 576, height: 1248, zIndex: 3 },
}
```

- [ ] **Step 2: Regenerate canonical project state**

```bash
rtk node scripts/seed-project.mjs
rtk bun run verify:project
rtk bun run verify:visual
```

Expected: both verifiers pass; iPhone and iPad slides contain no new transforms.

- [ ] **Step 3: Commit implementation**

```bash
rtk git add store-assets/screenshot-editor
rtk git commit -m "design: refine store screenshot presentation"
```

### Task 4: Re-export and Verify Corrected Assets

**Files:**
- Replace: `store-assets/exports/en/android/1080x1920/*.png`
- Replace: `store-assets/exports/en/feature-graphic/1024x500/01-feature-graphic.png`
- Replace: `store-assets/review/contact-sheet.png`

- [ ] **Step 1: Start the editor and inspect both corrected decks**

```bash
rtk bun dev
```

Open `http://localhost:3000`, inspect Android screen 3 and Feature Graphic, and confirm the visual acceptance criteria before export.

- [ ] **Step 2: Export Android Phone and Feature Graphic through Playwright**

Save both downloaded ZIP files under a temporary directory, extract the seven Android PNGs and one Feature Graphic into their canonical export directories, and preserve their existing filenames.

- [ ] **Step 3: Normalize and verify all exports**

```bash
rtk python3 tools/store_screenshots/verify_exports.py --flatten \
  --export-root store-assets/exports/en \
  --contact-sheet store-assets/review/contact-sheet.png
```

Expected: `Flattened 8; verified 50 opaque RGB PNGs`.

- [ ] **Step 4: Inspect corrected assets at original resolution**

Verify the orange banner, white text contrast, camera-less Android shell, heavier SF typography, and clean separation between the Android screen-3 device and caption.

- [ ] **Step 5: Commit regenerated assets**

```bash
rtk git add store-assets/exports/en/android store-assets/exports/en/feature-graphic store-assets/review/contact-sheet.png
rtk git commit -m "assets: refresh corrected store screenshots"
```

### Task 5: Final Verification

**Files:**
- Verify only.

- [ ] **Step 1: Run durable checks**

```bash
rtk bun run verify:project
rtk bun run verify:visual
rtk bun run build
rtk python3 -m unittest tools.store_screenshots.test_verify_exports -v
rtk python3 tools/store_screenshots/verify_exports.py --flatten \
  --export-root store-assets/exports/en \
  --contact-sheet store-assets/review/contact-sheet.png
rtk git diff --check
rtk git status --short
```

Expected: both editor verifiers and build pass, six Python tests pass, all 50 PNGs remain valid, and only the known generated Xcode linkage directories remain untracked.
