# Localized Store Screenshot Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Translate the approved seven-screen Quiet Editorial campaign into all 37 application locales, generate store-ready images headlessly in CI, retain only English and Georgian README imagery in Git, and measure real export duration.

**Architecture:** `app-store-screenshots.json` remains the visual and localization source of truth. A read-only Next.js export route renders a requested locale/device/slide, a Playwright command captures exact store dimensions, a validator checks the result, and a manually dispatched GitHub Actions matrix generates one artifact per locale with bounded parallelism. Fastlane receives explicit staged copies and never owns the editor or publishes assets implicitly.

**Tech Stack:** Next.js 15, React 19, TypeScript, Playwright Chromium, Bun, Python/Pillow validators, GitHub Actions artifacts, Fastlane supply/deliver directory conventions.

---

## File map

- Modify `store-assets/screenshot-editor/app-store-screenshots.json`: all localized labels/headlines and shared capture paths.
- Create `store-assets/screenshot-editor/src/lib/store-locales.ts`: locale inventory, RTL detection, and App Store/Google Play mappings.
- Create `store-assets/screenshot-editor/scripts/verify-localizations.mjs`: compare application resource locales with project locales and reject fallback-only copy.
- Modify `store-assets/screenshot-editor/scripts/verify-project.mjs`: retain structural deck checks while accepting the full locale set.
- Modify `store-assets/screenshot-editor/src/components/editor/slide-canvas.tsx`: apply locale direction to marketing copy.
- Create `store-assets/screenshot-editor/src/app/export/page.tsx`: read-only export entry point.
- Create `store-assets/screenshot-editor/src/components/editor/export-canvas.tsx`: isolated exact-size render surface.
- Create `store-assets/screenshot-editor/scripts/export-ci.mjs`: Playwright capture command and timing manifest.
- Create `store-assets/screenshot-editor/scripts/test-export-contract.mjs`: argument, path, count, and mapping tests.
- Modify `store-assets/screenshot-editor/package.json` and `bun.lock`: Playwright command and dependency.
- Create `.github/workflows/store-screenshots.yml`: manual one/all-locale artifact generation.
- Create `tools/store_screenshots/stage_fastlane.py`: explicit store directory mapping.
- Create `tools/store_screenshots/test_stage_fastlane.py`: staging behavior tests.
- Modify `.gitignore`: ignore disposable localized exports while retaining approved English/Georgian README files.
- Modify `README.md`: Georgian iPhone/iPad gallery and regeneration instructions.
- Create `store-assets/exports/ka/iphone/1320x2868/*.png`: tracked Georgian README images.
- Create `store-assets/exports/ka/ipad/2064x2752/*.png`: tracked Georgian README images.

### Task 1: Locale and translation contract

**Files:**
- Create: `store-assets/screenshot-editor/src/lib/store-locales.ts`
- Create: `store-assets/screenshot-editor/scripts/verify-localizations.mjs`
- Modify: `store-assets/screenshot-editor/package.json`
- Test: `store-assets/screenshot-editor/scripts/verify-localizations.mjs`

- [ ] **Step 1: Write the failing localization verifier**

Define the expected application locale list exactly as:

```js
const EXPECTED = [
  "ar", "az", "be", "bn", "da", "de", "el", "en", "es", "fi",
  "fr", "he", "hi", "hu", "hy", "id", "it", "ja", "ka", "kk",
  "ko", "ky", "nb", "nl", "pl", "pt", "ro", "ru", "sv", "tg",
  "th", "tk", "tr", "uk", "uz", "vi", "zh",
];
```

The verifier reads the five `composeResources` roots, asserts identical locale
sets, checks `state.locales`, then checks every slide's `label[locale]` and
`headline[locale]`. Empty labels remain allowed only for the feature graphic.

- [ ] **Step 2: Run the verifier and confirm RED**

Run:

```bash
cd store-assets/screenshot-editor
bun run verify:localizations
```

Expected: FAIL because the project currently targets only `en`.

- [ ] **Step 3: Add shared locale policy**

Export immutable data and helpers:

```ts
export const STORE_LOCALES = [/* exact 37-code list above */] as const;
export type StoreLocale = (typeof STORE_LOCALES)[number];
export const RTL_LOCALES = new Set<StoreLocale>(["ar", "he"]);

export function directionForLocale(locale: string): "ltr" | "rtl" {
  return RTL_LOCALES.has(locale as StoreLocale) ? "rtl" : "ltr";
}
```

Include explicit mappings for App Store and Google Play. `ka` maps only to
Google Play `ka-GE`; unsupported Apple locales return `null`. `zh` maps to
`zh-Hant` for Apple and `zh-TW` for Play until a Simplified Chinese campaign is
authored separately.

- [ ] **Step 4: Run unit/static checks**

Run `bun run verify:localizations`.

Expected: still FAIL only for missing project translations, proving repository
locale discovery and mapping code load successfully.

- [ ] **Step 5: Commit**

```bash
git add store-assets/screenshot-editor/src/lib/store-locales.ts \
  store-assets/screenshot-editor/scripts/verify-localizations.mjs \
  store-assets/screenshot-editor/package.json
git commit -m "test: define screenshot localization contract"
```

### Task 2: Author all localized campaign copy

**Files:**
- Modify: `store-assets/screenshot-editor/app-store-screenshots.json`
- Modify: `store-assets/screenshot-editor/scripts/seed-project.mjs`
- Modify: `store-assets/screenshot-editor/scripts/verify-project.mjs`
- Modify: `store-assets/screenshot-editor/src/components/editor/slide-canvas.tsx`
- Test: `store-assets/screenshot-editor/scripts/verify-localizations.mjs`

- [ ] **Step 1: Add translation completeness assertions to the existing project verifier**

Check all 37 locales, seven slides in each populated device deck, one localized
feature graphic, and the fixed layout rhythm. Assert every localized value is
trimmed and contains no fallback markers such as `[en]`.

- [ ] **Step 2: Run both verifiers and confirm RED**

Run:

```bash
bun run verify:project
bun run verify:localizations
```

Expected: FAIL listing missing locale values.

- [ ] **Step 3: Populate the 37-locale copy table**

For every locale, adapt these seven approved ideas rather than translating
word-for-word:

1. growing world of puzzles;
2. choose the next challenge;
3. clear lines and find a flow;
4. merge numbers and think ahead;
5. drop fruit and grow bigger;
6. different games in one thoughtful home;
7. the next puzzle awaits.

Keep brand/game labels (`LOGICA`, `BLOCK BLAST`, `2048`, `FRUIT MERGE`) intact;
localize `CHOOSE YOUR GAME` and `GROWING COLLECTION`. Each headline keeps an
intentional one- or two-line break and must fit the existing caption bounds.
Update `seed-project.mjs` from the same translation table so reset cannot erase
localizations.

- [ ] **Step 4: Stop duplicating source captures by locale**

Replace `/screenshots/apple/{device}/{locale}/...` with the checked-in shared
English capture paths. The marketing layer is localized; this phase does not
claim that the UI inside each device was recaptured.

- [ ] **Step 5: Apply RTL direction**

Set `dir={directionForLocale(locale)}` on caption and feature-graphic text
containers. Preserve centered alignment and explicit line breaks.

- [ ] **Step 6: Run localization and build checks**

Run:

```bash
bun run verify:project
bun run verify:localizations
bun run verify:visual
bun run build
```

Expected: all commands PASS.

- [ ] **Step 7: Commit**

```bash
git add store-assets/screenshot-editor/app-store-screenshots.json \
  store-assets/screenshot-editor/scripts/seed-project.mjs \
  store-assets/screenshot-editor/scripts/verify-project.mjs \
  store-assets/screenshot-editor/src/components/editor/slide-canvas.tsx
git commit -m "feat: localize store campaign for all app languages"
```

### Task 3: Build a deterministic headless renderer

**Files:**
- Create: `store-assets/screenshot-editor/src/app/export/page.tsx`
- Create: `store-assets/screenshot-editor/src/components/editor/export-canvas.tsx`
- Create: `store-assets/screenshot-editor/scripts/export-ci.mjs`
- Create: `store-assets/screenshot-editor/scripts/test-export-contract.mjs`
- Modify: `store-assets/screenshot-editor/package.json`
- Modify: `store-assets/screenshot-editor/bun.lock`

- [ ] **Step 1: Write failing export-contract tests**

Test that the CLI rejects unknown locales/devices, derives the expected 50-file
count for one complete locale (28 iPhone + 14 iPad + 7 Android + 1 feature
graphic), generates zero-padded paths, and produces deterministic manifest
metadata apart from elapsed time and commit SHA.

- [ ] **Step 2: Run and confirm RED**

Run `bun run test:export-contract`.

Expected: FAIL because the export module does not exist.

- [ ] **Step 3: Create the read-only export page**

The page accepts `device`, `locale`, `slide`, `width`, and `height`. It imports
the checked-in project data server-side, passes only the requested deck to
`ExportCanvas`, and returns an error for invalid parameters. It never calls
`useProject` and therefore never writes project JSON or local storage.

- [ ] **Step 4: Render exact output dimensions**

`ExportCanvas` creates `#export-frame` at requested dimensions, places the
canonical `DeckCanvas` inside it with independent X/Y scaling, shifts the deck
by the requested slide index, waits for `document.fonts.ready` and every image,
then sets `data-export-ready="true"`. This preserves connected-canvas crops and
matches the current `html-to-image` scaling behavior.

- [ ] **Step 5: Implement the Playwright command**

The CLI accepts:

```text
--locale <code>
--base-url <url>
--output <directory>
--devices iphone,ipad,android,feature-graphic
```

It launches Chromium, visits one export URL per output, waits for the ready
marker, captures `#export-frame`, records per-device and total elapsed seconds,
and writes `manifest.json`. It exits non-zero on console errors, missing assets,
timeouts, or a wrong output count.

- [ ] **Step 6: Add Playwright and scripts**

Add `playwright` as a pinned development dependency and scripts:

```json
{
  "test:export-contract": "node scripts/test-export-contract.mjs",
  "export:ci": "node scripts/export-ci.mjs"
}
```

- [ ] **Step 7: Run contract tests and a one-locale smoke export**

Run the production server and export English to a temporary directory. Validate
with `tools/store_screenshots/verify_exports.py`.

Expected: 50 RGB PNG files plus `manifest.json`, with every declared dimension
passing.

- [ ] **Step 8: Commit**

```bash
git add store-assets/screenshot-editor/src/app/export \
  store-assets/screenshot-editor/src/components/editor/export-canvas.tsx \
  store-assets/screenshot-editor/scripts/export-ci.mjs \
  store-assets/screenshot-editor/scripts/test-export-contract.mjs \
  store-assets/screenshot-editor/package.json store-assets/screenshot-editor/bun.lock
git commit -m "feat: add headless store screenshot exporter"
```

### Task 4: Add disposable CI artifacts and timing reports

**Files:**
- Create: `.github/workflows/store-screenshots.yml`
- Modify: `.gitignore`

- [ ] **Step 1: Add a workflow syntax assertion**

Extend the export-contract test to parse the workflow and assert
`workflow_dispatch`, a locale input, `max-parallel: 4`, `retention-days: 14`,
and no store credential or upload action.

- [ ] **Step 2: Run and confirm RED**

Run `bun run test:export-contract`.

Expected: FAIL because the workflow is absent.

- [ ] **Step 3: Implement the prepare job**

Accept input `locale` with default `all`. Validate a single code against the
37-code list or emit the complete JSON matrix. Do not run on push or pull
request.

- [ ] **Step 4: Implement the render matrix**

Use `ubuntu-latest`, `strategy.max-parallel: 4`, Bun's frozen lockfile,
Playwright Chromium, a production Next.js build/server, the one-locale exporter,
the Python image validator, and `actions/upload-artifact@v4` with 14-day
retention. Add the manifest timing summary to `$GITHUB_STEP_SUMMARY`.

- [ ] **Step 5: Ignore disposable output**

Ignore `store-assets/ci-exports/` and all non-English/non-Georgian generated
locale directories. Use explicit negations so the two README galleries remain
tracked.

- [ ] **Step 6: Validate YAML contract and commit**

Run `bun run test:export-contract`, then commit the workflow and ignore rules.

### Task 5: Add explicit Fastlane staging

**Files:**
- Create: `tools/store_screenshots/stage_fastlane.py`
- Create: `tools/store_screenshots/test_stage_fastlane.py`
- Modify: `fastlane/Fastfile`

- [ ] **Step 1: Write failing staging tests**

Cover `en -> en-US`, `ka -> ka-GE` on Play, Georgian omission on Apple,
`zh -> zh-Hant/zh-TW`, ordered screenshot copying, feature graphic naming, clean
replacement of a single locale staging directory, and rejection of incomplete
input.

- [ ] **Step 2: Run and confirm RED**

Run `python3 -m unittest tools.store_screenshots.test_stage_fastlane -v`.

- [ ] **Step 3: Implement staging without publishing**

Accept `--input`, `--fastlane-root`, and `--locale`. Copy only the requested
locale into Fastlane's iOS/Android conventions and emit a summary. Never invoke
Fastlane or access credentials.

- [ ] **Step 4: Add a local Fastlane helper lane**

Add `android stage_store_assets` that shells out to the Python staging command.
Keep `android release` flags unchanged, including screenshot/image skips.

- [ ] **Step 5: Run tests and commit**

Expected: all staging tests PASS and the existing release lane remains inert
with respect to listing imagery.

### Task 6: Generate Georgian README galleries

**Files:**
- Create: `store-assets/exports/ka/iphone/1320x2868/*.png`
- Create: `store-assets/exports/ka/ipad/2064x2752/*.png`
- Modify: `README.md`

- [ ] **Step 1: Export Georgian canonical sizes**

Run the headless exporter for `ka` into a temporary full output directory, then
copy only the seven 1320×2868 iPhone and seven 2064×2752 iPad images into the
tracked `store-assets/exports/ka` paths.

- [ ] **Step 2: Validate Georgian output**

Run the image validator and visually inspect a Georgian contact sheet. Confirm
headline fit, Georgian glyph rendering, RGB output, and no English marketing
fallback.

- [ ] **Step 3: Update README**

Add `ქართული` iPhone and iPad galleries with Georgian alt text. Add a short
section explaining that all other localizations are generated by the manual
`Store screenshots` workflow and are intentionally not committed.

- [ ] **Step 4: Verify every README image reference**

Resolve all local `<img src>` paths and fail if any file is missing.

- [ ] **Step 5: Commit**

```bash
git add README.md store-assets/exports/ka
git commit -m "docs: add Georgian store screenshot gallery"
```

### Task 7: Measure and document CI duration

**Files:**
- Modify: `store-assets/screenshot-editor/README.md`
- Modify: `README.md`

- [ ] **Step 1: Benchmark a cold local one-locale run**

Measure dependency/browser setup separately from build, server startup, and 50
PNG renders. Record the machine context and manifest timings.

- [ ] **Step 2: Benchmark a warm second locale**

Reuse installed dependencies and browser to isolate render cost. Do not report
local wall time as GitHub-hosted truth.

- [ ] **Step 3: Calculate CI bounds**

For `max-parallel: 4`, calculate:

```text
waves = ceil(37 / 4) = 10
estimated wall time = waves × measured cold job duration + queue variance
total billed runner time = 37 × measured cold job duration
```

State the local measurement, the calculated initial CI estimate, and that the
first actual workflow run's job manifests supersede the estimate.

- [ ] **Step 4: Document commands and storage policy**

Document one-locale and all-locale workflow dispatch, artifact expiry, Fastlane
staging, and why generated PNGs are not versioned.

- [ ] **Step 5: Run final verification**

Run:

```bash
cd store-assets/screenshot-editor
bun run verify:project
bun run verify:localizations
bun run verify:visual
bun run test:export-contract
bun run build
cd ../../
python3 -m unittest tools.store_screenshots.test_verify_exports \
  tools.store_screenshots.test_stage_fastlane -v
git diff --check
```

Expected: every command PASS. Report any GitHub-hosted timing as unverified
until the manual workflow has actually run.

- [ ] **Step 6: Commit**

```bash
git add README.md store-assets/screenshot-editor/README.md
git commit -m "docs: explain localized screenshot automation"
```
