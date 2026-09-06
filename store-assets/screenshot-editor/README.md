# App Store Screenshots — Editor Template

A pre-built Next.js + ShadCN editor for generating App Store and Google Play screenshots. Scaffolded by the `app-store-screenshots` skill.

## Quick start

```bash
bun install   # or pnpm / yarn / npm
bun dev       # http://localhost:3000
```

## What's inside

- **Connected canvas editor** (`src/components/editor/`) — every screen sits on one horizontal canvas, so phones, captions, and other elements can be dragged across screen boundaries and exported as split crops when Connected mode is enabled.
- **Screen controls** — drag-to-reorder screens, click-to-edit text, screenshot drop targets, per-screen layout switcher, dark/light toggle.
- **Device frames** (`src/components/editor/device-frames.tsx`) — iPhone (PNG mockup), iPad, Android phone, Android tablet (portrait + landscape), feature graphic.
- **Auto-save (git-trackable)** — every change is persisted within ~600ms to **`app-store-screenshots.json`** at the project root (via `/api/project`) **and** mirrored to `localStorage` as an instant-paint cache. Commit `app-store-screenshots.json` and you can `git clone` to another machine and resume exactly where you left off.
- **Multi-device decks** — iOS and Android slide decks live side by side; switching the platform tab preserves both.
- **One-click export** — bulk PNG export at any required App Store / Play Store resolution using `html-to-image`; each PNG is rendered from the current connected or isolated deck mode.
- **Project migration** — older `app-store-screenshots.json` files are migrated on load. Existing per-slide transforms remain valid, and connected crops become available without rewriting the deck by hand.
- **Legacy-safe mode** — pre-v2 projects opened directly in the editor start in isolated-screen mode first, then can opt into connected crops with the toolbar's Connected/Isolated control. Skill-run in-place migrations keep legacy decks isolated unless the project had already explicitly opted into connected canvas.

## Adding screenshots

Two ways:

1. **Drop a file in the inspector** — drag-and-drop or click Pick. The file is sent to `/api/upload`, hashed, and written to `public/screenshots/uploaded/<hash>.png`. The slide stores the resulting `/screenshots/uploaded/...` path, so commit those files alongside `app-store-screenshots.json` and the screenshots survive a `git clone`.
2. **Reference a static file** — put PNGs under `public/screenshots/{platform}/{device}/{locale}/` and reference them by path. Default sample slides expect:
   - `public/screenshots/apple/iphone/en/...`
   - `public/screenshots/android/phone/en/...`
   - `public/screenshots/apple/ipad/en/...`

Update the matching `screenshot` fields in `app-store-screenshots.json` to point at whatever filenames you choose.

## Exporting

The toolbar dropdown lists every Apple/Google-required size for the current device. Click **Export bundle** to download a zip. In Connected mode, each PNG is clipped from the connected canvas, so an element that straddles two screens appears split exactly where you placed it. In Isolated mode, each screen clips its own elements and legacy offscreen content cannot leak into neighboring exports.

### Headless localized export

The editor includes marketing copy for every locale shipped by the application.
To generate and validate one complete 50-image bundle locally:

```bash
bun run build
bun start -- -H 127.0.0.1 -p 3100
bun run export:ci -- --locale ka --output ../../store-assets/ci-exports
```

The exporter downloads its managed Chromium browser by default. On macOS, an
installed Chrome can be used with `SCREENSHOT_BROWSER_CHANNEL=chrome` if the
Playwright browser is unavailable.

The manual **Store screenshots** GitHub Actions workflow accepts either one
locale code or `all`. It renders and validates all 50 size variants per locale,
then retains the 22 canonical upload files: seven 6.9-inch iPhone screenshots,
seven 13-inch iPad screenshots, seven Android phone screenshots, one Google Play
feature graphic, and the timing manifest. Apple generates the smaller accepted
device sizes from the highest-resolution uploads. Artifacts expire after 14
days. A single-locale run only generates its artifact. An all-locale run from
`main` continues to independent Google Play and App Store Connect publication
jobs after every locale passes validation.

```bash
gh workflow run store-screenshots.yml -f locale=ka
gh workflow run store-screenshots.yml -f locale=all
```

The `locale=all` command uploads all 34 Google Play metadata packages, phone
screenshot decks, and feature graphics with `changes_not_sent_for_review: true`.
In parallel it uploads 28 App Store metadata packages and 392 canonical Apple
screenshots: 196 for iPhone and 196 for iPad. Both store edits remain pending
until a human explicitly sends them for review. The workflow uploads no APK,
AAB, or IPA and changes no release track. Publication requires
`PLAY_STORE_JSON_KEY` plus `APP_STORE_CONNECT_KEY_ID`,
`APP_STORE_CONNECT_ISSUER_ID`, and `APP_STORE_CONNECT_KEY_BASE64`, and is skipped
outside `main`.

App Store screenshots attach to an editable version. Before running `all`, set
`MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig` to the next version
being prepared in App Store Connect. A locale-only run remains artifact-only.

After downloading an artifact, stage its store-specific locale paths for
Fastlane without committing generated bundles:

```bash
SCREENSHOT_LOCALE=ka \
SCREENSHOT_EXPORT_ROOT=/path/to/downloaded/export \
bundle exec fastlane android stage_store_assets
```

Georgian has no App Store Connect screenshot locale, so the staging command
places Georgian assets only under Google Play metadata. The one-locale staging
lane itself does not publish anything.

### Measured runtime and storage

Local release-build measurements on this machine were 35.1 seconds for English,
35.3 seconds for Georgian, and 35.4 seconds for German per 50-image locale. At
four parallel jobs, pure rendering of all 34 locales is about 5.3 minutes of
wall time. Allow roughly 15–25 minutes for the cached GitHub Actions workflow,
including runner setup, build, browser startup, validation, and upload; the
first uncached render can take 20–30 minutes. Artifact download, staging, and
parallel store draft uploads add an estimated 5–20 minutes, giving an expected
20–45 minute end-to-end run. Each run writes its actual job and render duration
to the Actions summary, which is the authoritative measurement.

The tag-driven release workflow separately builds a signed iOS IPA and uploads
it to App Store Connect/TestFlight without distributing or submitting it. It
requires the App Store Connect key, Apple Distribution `.p12`, matching App
Store provisioning profile, temporary-keychain password, and base64 iOS
Firebase plist secrets documented in the repository README. The IPA and dSYM
expire after 14 days; signing material is removed even when the job fails.

Keeping every size for every locale would be roughly 850 MB. The workflow keeps
only canonical artifacts, approximately 10 MB per locale or 340 MB for all 34,
and removes them automatically after 14 days. Only the English visual-regression
baseline and Georgian README gallery are stored permanently in Git. Tajik,
Turkmen, and Uzbek stay localized inside the app but are omitted from screenshot
generation because neither Google Play nor App Store Connect accepts those
listing locales.

## Customizing

| Where | What |
|-------|------|
| `src/lib/constants.ts` | Canvas dimensions, export sizes, frame ratios, themes, locales |
| `app-store-screenshots.json` | Canonical starter project: app name, current device, connected-canvas mode, slide copy, screenshots, and transforms |
| `src/lib/defaults.ts` | Fallback/reset state used when no project file or local cache exists |
| `src/components/editor/slide-canvas.tsx` | Add new layouts and connected-canvas element rendering |
| `src/components/editor/device-frames.tsx` | Tweak device chrome (bezel radii, camera dots) |
| `src/app/layout.tsx` | Swap the font (`next/font/google`) |

## Notes

- `mockup.png` is the iPhone bezel overlay; replacing it requires re-measuring the `PHONE_SCREEN` constants.
- Image preloading converts every static path to a base64 data URI before exports run, and export retries paths that were previously missing — this prevents the html-to-image race where some slide screenshots come out black.
- Reset via the toolbar's circular arrow icon clears in-memory state and reloads the default screens. To wipe disk state too, delete `app-store-screenshots.json`.
- **Persistence model** — the canonical state lives in `app-store-screenshots.json` (git-tracked). On load, the editor reads localStorage first for instant paint, then overwrites with the file contents if present; if the file endpoint is unavailable, autosave is blocked so stale cache cannot overwrite disk. On save, both are written. If you ever see a conflict, the file always wins.
- **Migration model** — schema v1 projects do not need a manual conversion. On first load, the editor upgrades localized text and transform records, writes `schemaVersion: 2`, preserves all existing screens, and keeps `connectedCanvas: false` so old offscreen/clipped elements export exactly as isolated screens. Turn on **Connected** in the toolbar when you want elements to cross screen edges. Explicit skill migrations preserve an existing `connectedCanvas` choice, otherwise they keep legacy decks isolated too.
- **Custom themes** — if a project file references a theme id that is not present in `src/lib/constants.ts`, the editor falls back to `clean-light` and shows a warning. Merge custom `THEMES` entries during in-place upgrades.
