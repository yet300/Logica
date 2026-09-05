# Google Play Localized Listing Draft Publish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing all-locale screenshot workflow upload a complete 37-locale Google Play listing edit, including rewritten super-app metadata and generated images, without sending the edit for review.

**Architecture:** Keep translated Play metadata as reviewable Fastlane text files in Git. Add a Python validation/staging boundary that assembles the 37 downloaded screenshot artifacts into Fastlane's Android metadata tree and refuses partial inputs. After every locale render succeeds, one gated GitHub Actions job invokes a dedicated metadata-only Fastlane lane with `changes_not_sent_for_review: true`.

**Tech Stack:** GitHub Actions, Python 3.13 standard library and `unittest`, Fastlane Supply/Ruby, Bun/Node test runner, Playwright-generated PNG artifacts.

---

## File structure

- Create `tools/store_screenshots/play_listing.py`: canonical Play locale map, metadata validation, artifact discovery, and all-locale image staging.
- Create `tools/store_screenshots/test_play_listing.py`: unit and integration-style tests for metadata limits, exact locale coverage, artifact staging, and failure atomicity.
- Create `fastlane/metadata/android/<play-locale>/title.txt`: unchanged title for each of the 37 Play locales.
- Create `fastlane/metadata/android/<play-locale>/short_description.txt`: localized super-app short description.
- Create `fastlane/metadata/android/<play-locale>/full_description.txt`: localized super-app full description.
- Modify `tools/store_screenshots/stage_fastlane.py`: import the shared Play locale map instead of maintaining a second Python copy.
- Modify `tools/store_screenshots/test_stage_fastlane.py`: keep locale mapping and single-locale staging regression coverage after the refactor.
- Modify `fastlane/Fastfile`: add the metadata/images/screenshots-only draft lane.
- Modify `.github/workflows/store-screenshots.yml`: add the post-matrix Play draft publication job.
- Modify `store-assets/screenshot-editor/scripts/test-export-contract.mjs`: verify workflow gating, secret use, and the absence of binary/App Store publishing.
- Modify `.gitignore`: ignore generated Fastlane `images/` directories while keeping translated text and changelogs tracked.
- Modify `store-assets/screenshot-editor/README.md`: document that `locale=all` now uploads a Play Console edit that is not sent for review.
- Modify `README.md`: document the one-command draft publication behavior and required secret.

### Task 1: Canonical Google Play locale and metadata validator

**Files:**
- Create: `tools/store_screenshots/play_listing.py`
- Create: `tools/store_screenshots/test_play_listing.py`
- Modify: `tools/store_screenshots/stage_fastlane.py`
- Modify: `tools/store_screenshots/test_stage_fastlane.py`

- [ ] **Step 1: Write failing tests for the canonical locale contract**

Add tests that assert the exact application-to-Play mapping:

```python
EXPECTED_PLAY_LOCALES = {
    "ar": "ar", "az": "az-AZ", "be": "be-BY", "bn": "bn-BD",
    "da": "da-DK", "de": "de-DE", "el": "el-GR", "en": "en-US",
    "es": "es-ES", "fi": "fi-FI", "fr": "fr-FR", "he": "he-IL",
    "hi": "hi-IN", "hu": "hu-HU", "hy": "hy-AM", "id": "id-ID",
    "it": "it-IT", "ja": "ja-JP", "ka": "ka-GE", "kk": "kk-KZ",
    "ko": "ko-KR", "ky": "ky-KG", "nb": "nb-NO", "nl": "nl-NL",
    "pl": "pl-PL", "pt": "pt-BR", "ro": "ro-RO", "ru": "ru-RU",
    "sv": "sv-SE", "tg": "tg-TJ", "th": "th-TH", "tk": "tk-TM",
    "tr": "tr-TR", "uk": "uk-UA", "uz": "uz-UZ", "vi": "vi-VN",
    "zh": "zh-TW",
}

def test_play_locale_map_is_exact():
    self.assertEqual(PLAY_STORE_LOCALES, EXPECTED_PLAY_LOCALES)
    self.assertEqual(len(PLAY_STORE_LOCALES), 37)
```

- [ ] **Step 2: Write failing metadata validation tests**

Test a complete temporary metadata tree and individual failures for a missing
locale, an extra locale, a translated title, an 81-character short description,
an empty full description, and a 4001-character full description:

```python
def test_rejects_title_translation(self):
    root = self.complete_metadata_tree()
    (root / "ka-GE/title.txt").write_text("ლოგიკა", encoding="utf-8")
    with self.assertRaisesRegex(ValueError, "title"):
        validate_metadata(root)
```

- [ ] **Step 3: Run the tests and verify the red state**

Run:

```bash
python3 -m unittest tools.store_screenshots.test_play_listing -v
```

Expected: import failure because `play_listing.py` does not exist.

- [ ] **Step 4: Implement the canonical validator**

Create constants and functions with these public signatures:

```python
TITLE = "Logica — Block Puzzle"
PLAY_STORE_LOCALES: dict[str, str] = {
    "ar": "ar", "az": "az-AZ", "be": "be-BY", "bn": "bn-BD",
    "da": "da-DK", "de": "de-DE", "el": "el-GR", "en": "en-US",
    "es": "es-ES", "fi": "fi-FI", "fr": "fr-FR", "he": "he-IL",
    "hi": "hi-IN", "hu": "hu-HU", "hy": "hy-AM", "id": "id-ID",
    "it": "it-IT", "ja": "ja-JP", "ka": "ka-GE", "kk": "kk-KZ",
    "ko": "ko-KR", "ky": "ky-KG", "nb": "nb-NO", "nl": "nl-NL",
    "pl": "pl-PL", "pt": "pt-BR", "ro": "ro-RO", "ru": "ru-RU",
    "sv": "sv-SE", "tg": "tg-TJ", "th": "th-TH", "tk": "tk-TM",
    "tr": "tr-TR", "uk": "uk-UA", "uz": "uz-UZ", "vi": "vi-VN",
    "zh": "zh-TW",
}
REQUIRED_METADATA_FILES = (
    "title.txt",
    "short_description.txt",
    "full_description.txt",
)

@dataclass(frozen=True)
class MetadataSummary:
    locales: int
    titles: int
    short_descriptions: int
    full_descriptions: int

def play_store_locale(locale: str) -> str:
    try:
        return PLAY_STORE_LOCALES[locale]
    except KeyError as error:
        raise ValueError(f"Unsupported locale: {locale}") from error

def validate_metadata(metadata_root: Path) -> MetadataSummary:
    expected = set(PLAY_STORE_LOCALES.values())
    actual = {path.name for path in metadata_root.iterdir() if path.is_dir()}
    if actual != expected:
        raise ValueError(
            f"Metadata locales differ: missing={sorted(expected - actual)}, "
            f"extra={sorted(actual - expected)}"
        )
    for play_locale in sorted(expected):
        locale_root = metadata_root / play_locale
        title = (locale_root / "title.txt").read_text(encoding="utf-8").strip()
        short = (locale_root / "short_description.txt").read_text(encoding="utf-8").strip()
        full = (locale_root / "full_description.txt").read_text(encoding="utf-8").strip()
        if title != TITLE or len(title) > 30:
            raise ValueError(f"Invalid title for {play_locale}")
        if not 1 <= len(short) <= 80:
            raise ValueError(f"Invalid short description for {play_locale}")
        if not 1 <= len(full) <= 4000:
            raise ValueError(f"Invalid full description for {play_locale}")
    count = len(expected)
    return MetadataSummary(count, count, count, count)
```

`validate_metadata` reads each required file as strict UTF-8, strips only outer
whitespace for validation, counts Unicode code points with Python `len`, and
requires exact equality between directory names and the 37 mapped Play locale
codes. It ignores `changelogs/` and generated `images/` subdirectories.

- [ ] **Step 5: Remove the duplicate Python Play locale map**

Import `PLAY_STORE_LOCALES` and `play_store_locale` into
`stage_fastlane.py`. Keep the App Store mapping local because it has different
platform semantics. Update the existing test import to use the canonical module.

- [ ] **Step 6: Run both test modules**

Run:

```bash
python3 -m unittest \
  tools.store_screenshots.test_play_listing \
  tools.store_screenshots.test_stage_fastlane -v
```

Expected: all locale and validator tests pass.

- [ ] **Step 7: Commit the validator boundary**

```bash
git add tools/store_screenshots/play_listing.py \
  tools/store_screenshots/test_play_listing.py \
  tools/store_screenshots/stage_fastlane.py \
  tools/store_screenshots/test_stage_fastlane.py
git commit -m "feat: validate localized Play Store metadata"
```

### Task 2: Rewrite and localize the Google Play listing

**Files:**
- Create: `fastlane/metadata/android/{ar,az-AZ,be-BY,bn-BD,da-DK,de-DE,el-GR,en-US,es-ES,fi-FI,fr-FR,he-IL,hi-IN,hu-HU,hy-AM,id-ID,it-IT,ja-JP,ka-GE,kk-KZ,ko-KR,ky-KG,nb-NO,nl-NL,pl-PL,pt-BR,ro-RO,ru-RU,sv-SE,tg-TJ,th-TH,tk-TM,tr-TR,uk-UA,uz-UZ,vi-VN,zh-TW}/{title,short_description,full_description}.txt`
- Preserve: `fastlane/metadata/android/en-US/changelogs/*.txt`

- [ ] **Step 1: Add the canonical English listing**

Write the exact title and short description:

```text
Logica — Block Puzzle
```

```text
A growing collection of relaxing puzzle games for focus, strategy, and fun.
```

Write the English full description with this exact source copy:

```text
One app. A growing world of puzzle games.

Logica brings relaxing, thoughtful games together in one place. Pick a quick challenge, build a new strategy, and switch games whenever you want.

PLAY YOUR WAY
• Clear lines and shape the board in Block Blast
• Combine tiles and chase higher numbers in 2048
• Drop and merge fruit in a playful physics puzzle
• Discover more puzzle games as the collection grows

MADE FOR EVERY MOMENT
• Quick sessions that are easy to start
• Simple controls with satisfying feedback
• Personal best scores and saved progress
• Play offline wherever you are
• A clean, calm interface that keeps the puzzle in focus

Whether you enjoy block puzzles, number games, merge games, logic challenges, or casual brain teasers, Logica gives you a growing collection without filling your phone with separate apps.

Choose a game, settle in, and find your next favorite puzzle.
```

- [ ] **Step 2: Add the other 36 metadata packages**

For every mapped locale, write the same exact title and a native, concise
translation of the short and full description. Preserve the two-section
structure and the four current-game/growing-catalog bullets. Localize prose and
section labels, but keep `Logica`, `Block Blast`, `2048`, and `Fruit Merge`
unchanged. Do not introduce claims absent from the English source.

- [ ] **Step 3: Run the validator against the real metadata tree**

Run:

```bash
python3 tools/store_screenshots/play_listing.py \
  validate-metadata --metadata-root fastlane/metadata/android
```

Expected:

```text
Validated 37 Play Store metadata locales: 37 titles, 37 short descriptions, 37 full descriptions.
```

- [ ] **Step 4: Inspect character counts and scripts**

Run a report mode that prints locale, title length, short-description length,
and full-description length. Confirm every title is 21 characters, every short
description is 1–80 characters, and every full description is 1–4000
characters. Spot-check `ar` and `he` as RTL, `ka-GE` as Georgian, `ja-JP` as
Japanese, and `zh-TW` as Traditional Chinese.

- [ ] **Step 5: Commit localized listing copy**

```bash
git add fastlane/metadata/android
git commit -m "feat: localize Play Store super-app listing"
```

### Task 3: Assemble all screenshot artifacts for one Play edit

**Files:**
- Modify: `tools/store_screenshots/play_listing.py`
- Modify: `tools/store_screenshots/test_play_listing.py`
- Modify: `.gitignore`

- [ ] **Step 1: Write failing artifact staging tests**

Create 37 temporary artifact directories named
`store-screenshots-<app-locale>`. Each contains seven files under
`android/1080x1920/` and one
`feature-graphic/1024x500/01-feature-graphic.png`. Assert that staging produces:

```text
fastlane/metadata/android/<play-locale>/images/phoneScreenshots/01-hero.png
fastlane/metadata/android/<play-locale>/images/phoneScreenshots/07-hero.png
fastlane/metadata/android/<play-locale>/images/featureGraphic.png
```

Assert a summary of 37 locales, 259 phone screenshots, and 37 feature graphics.
Add failure tests for a missing artifact locale, six instead of seven phone
screenshots, and a missing feature graphic. Assert the destination remains
untouched when preflight fails.

- [ ] **Step 2: Run tests and verify the new failures**

Run:

```bash
python3 -m unittest tools.store_screenshots.test_play_listing -v
```

Expected: failures because `stage_all_play_assets` is not implemented.

- [ ] **Step 3: Implement preflight and staging**

Add:

```python
@dataclass(frozen=True)
class AssetSummary:
    locales: int
    phone_screenshots: int
    feature_graphics: int

def stage_all_play_assets(
    artifacts_root: Path,
    metadata_root: Path,
) -> AssetSummary:
    sources: dict[str, tuple[list[Path], Path]] = {}
    for locale, play_locale in PLAY_STORE_LOCALES.items():
        artifact = artifacts_root / f"store-screenshots-{locale}"
        phones = sorted((artifact / "android/1080x1920").glob("*.png"))
        feature = artifact / "feature-graphic/1024x500/01-feature-graphic.png"
        if len(phones) != 7:
            raise ValueError(f"Expected 7 phone screenshots for {locale}")
        if not feature.is_file():
            raise ValueError(f"Missing feature graphic for {locale}")
        sources[play_locale] = (phones, feature)

    for play_locale, (phones, feature) in sources.items():
        images = metadata_root / play_locale / "images"
        if images.exists():
            shutil.rmtree(images)
        phone_destination = images / "phoneScreenshots"
        phone_destination.mkdir(parents=True)
        for source in phones:
            shutil.copy2(source, phone_destination / source.name)
        shutil.copy2(feature, images / "featureGraphic.png")

    return AssetSummary(
        locales=len(sources),
        phone_screenshots=sum(len(value[0]) for value in sources.values()),
        feature_graphics=len(sources),
    )
```

Preflight all 37 source artifact directories before deleting or writing any
destination `images/` directory. After preflight, replace each locale's
`phoneScreenshots/`, copy the feature graphic, and retain existing metadata and
changelog files.

- [ ] **Step 4: Add the CLI command used by CI**

Support:

```bash
python3 tools/store_screenshots/play_listing.py stage \
  --artifacts-root store-assets/ci-artifacts \
  --metadata-root fastlane/metadata/android
```

It must validate text first, stage images second, validate the final image
inventory, and print deterministic counts.

- [ ] **Step 5: Ignore only generated Fastlane image directories**

Append:

```gitignore
fastlane/metadata/android/*/images/
```

Do not ignore locale text files or changelogs.

- [ ] **Step 6: Run the complete Python suite**

Run:

```bash
python3 -m unittest \
  tools.store_screenshots.test_verify_exports \
  tools.store_screenshots.test_stage_fastlane \
  tools.store_screenshots.test_play_listing -v
```

Expected: all tests pass.

- [ ] **Step 7: Commit all-locale staging**

```bash
git add .gitignore tools/store_screenshots/play_listing.py \
  tools/store_screenshots/test_play_listing.py
git commit -m "feat: stage complete Play listing artifacts"
```

### Task 4: Add the Fastlane metadata-only draft lane

**Files:**
- Modify: `fastlane/Fastfile`
- Modify: `tools/store_screenshots/test_play_listing.py`

- [ ] **Step 1: Write a failing Fastfile contract test**

Read `fastlane/Fastfile` as text and require one lane containing all of these
settings:

```ruby
lane :publish_store_listing_draft
skip_upload_apk: true
skip_upload_aab: true
skip_upload_metadata: false
skip_upload_changelogs: true
skip_upload_images: false
skip_upload_screenshots: false
sync_image_upload: true
changes_not_sent_for_review: true
```

Also reject `aab:`, `apk:`, and release-track mutation inside that lane.

- [ ] **Step 2: Run the test and verify it fails**

Run:

```bash
python3 -m unittest \
  tools.store_screenshots.test_play_listing.PlayListingFastfileTest -v
```

Expected: failure because the lane is absent.

- [ ] **Step 3: Implement the lane**

Add this lane under `platform :android`:

```ruby
desc "Upload localized Google Play listing changes without sending for review"
lane :publish_store_listing_draft do
  upload_to_play_store(
    package_name: "ge.yet.blokblast",
    metadata_path: "fastlane/metadata/android",
    json_key_data: ENV["PLAY_STORE_JSON_KEY"],
    skip_upload_apk: true,
    skip_upload_aab: true,
    skip_upload_metadata: false,
    skip_upload_changelogs: true,
    skip_upload_images: false,
    skip_upload_screenshots: false,
    sync_image_upload: true,
    changes_not_sent_for_review: true,
  )
end
```

Fail with `UI.user_error!` before Supply if `PLAY_STORE_JSON_KEY` is empty.

- [ ] **Step 4: Verify Ruby syntax and the contract test**

Run:

```bash
ruby -c fastlane/Fastfile
python3 -m unittest \
  tools.store_screenshots.test_play_listing.PlayListingFastfileTest -v
```

Expected: `Syntax OK` and passing tests.

- [ ] **Step 5: Commit the draft lane**

```bash
git add fastlane/Fastfile tools/store_screenshots/test_play_listing.py
git commit -m "feat: upload Play listing changes as draft"
```

### Task 5: Publish only after a successful all-locale render

**Files:**
- Modify: `.github/workflows/store-screenshots.yml`
- Modify: `store-assets/screenshot-editor/scripts/test-export-contract.mjs`

- [ ] **Step 1: Extend the workflow contract test**

Require a `publish-play-draft` job with:

```yaml
needs: [prepare, render]
if: ${{ inputs.locale == 'all' && needs.render.result == 'success' }}
```

Require `actions/download-artifact@v5`, artifact pattern
`store-screenshots-*`, the staging CLI, the draft Fastlane lane, and
`PLAY_STORE_JSON_KEY: ${{ secrets.PLAY_STORE_JSON_KEY }}`. Assert the workflow
does not call the normal `android release` lane and contains no App Store upload.

- [ ] **Step 2: Run the Node test and verify it fails**

Run:

```bash
cd store-assets/screenshot-editor
bun run test:export-contract
```

Expected: the publish-job assertions fail.

- [ ] **Step 3: Add the singular publication job**

Add a job that checks out the same commit, sets up Ruby/Bundler and Python,
downloads all screenshot artifacts without merging their directories, stages
and validates them, then calls:

```bash
bundle exec fastlane android publish_store_listing_draft
```

Use `FL_NUMBER_OF_THREADS=4`, `FASTLANE_SKIP_UPDATE_CHECK=1`, and
`LC_ALL=C.UTF-8`. Do not write the JSON secret to disk. Add a summary line with
37 locales, 259 screenshots, and 37 feature graphics after Fastlane succeeds.

- [ ] **Step 4: Verify workflow syntax and contract**

Run:

```bash
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/store-screenshots.yml")'
cd store-assets/screenshot-editor && bun run test:export-contract
```

Expected: YAML parses and all Node tests pass.

- [ ] **Step 5: Commit the workflow**

```bash
git add .github/workflows/store-screenshots.yml \
  store-assets/screenshot-editor/scripts/test-export-contract.mjs
git commit -m "ci: publish complete Play listing draft"
```

### Task 6: Documentation and final verification

**Files:**
- Modify: `README.md`
- Modify: `store-assets/screenshot-editor/README.md`

- [ ] **Step 1: Document the operational behavior**

State explicitly that:

```bash
gh workflow run store-screenshots.yml -f locale=all
```

generates all 37 locales and uploads the complete listing as changes not sent
for review, while a specific locale only generates an artifact. Document the
required `PLAY_STORE_JSON_KEY` repository secret and the manual Play Console
review action. Remove the obsolete statement that the workflow never publishes
to a store.

- [ ] **Step 2: Run all deterministic verification**

Run:

```bash
cd store-assets/screenshot-editor
bun run verify:project
bun run verify:localizations
bun run verify:visual
bun run test:export-contract
bun run build

cd ../../
python3 -m unittest \
  tools.store_screenshots.test_verify_exports \
  tools.store_screenshots.test_stage_fastlane \
  tools.store_screenshots.test_play_listing -v
python3 tools/store_screenshots/play_listing.py \
  validate-metadata --metadata-root fastlane/metadata/android
ruby -c fastlane/Fastfile
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/store-screenshots.yml")'
git diff --check
```

Expected: all JavaScript and Python tests pass, Next.js builds, 37 metadata
locales validate, Fastfile and workflow syntax pass, and `git diff --check`
prints no errors.

- [ ] **Step 3: Review generated-copy risks**

Read the English, Georgian, Russian, Arabic, German, Japanese, and Traditional
Chinese metadata files. Confirm game names are stable, no locale claims a fixed
game count, and no description claims the app is ad-free or medically improves
cognition.

- [ ] **Step 4: Commit documentation**

```bash
git add README.md store-assets/screenshot-editor/README.md
git commit -m "docs: explain Play listing draft publication"
```

- [ ] **Step 5: Record the external verification boundary**

Do not invoke Google Play from the development environment. Report that the
first real API verification requires merging/pushing the workflow, confirming
the repository secret, manually running `locale=all`, and inspecting the
pending Play Console changes before sending them for review.
