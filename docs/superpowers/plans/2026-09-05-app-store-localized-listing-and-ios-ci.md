# App Store Localized Listing and iOS CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a complete 28-locale App Store Connect listing draft from the screenshot workflow and build/upload a signed iOS IPA from version tags without submitting anything for review.

**Architecture:** Add an Apple-specific validation and staging boundary beside the existing Google Play boundary. Keep localized Fastlane metadata in Git, stage generated screenshots only in CI, use one metadata-only `deliver` edit, and use an independent tag-driven signed `build_app`/TestFlight upload lane. GitHub Actions materializes credentials; Fastlane owns App Store behavior.

**Tech Stack:** Python 3.13, Fastlane `deliver`/`build_app`/`upload_to_testflight`, GitHub Actions on `macos-26`, Xcode scheme `iosApp`, Kotlin Multiplatform/Gradle, App Store Connect API key authentication.

---

### Task 1: Canonical Apple locale and metadata contract

**Files:**
- Create: `tools/store_screenshots/app_store_listing.py`
- Create: `tools/store_screenshots/test_app_store_listing.py`
- Modify: `tools/store_screenshots/stage_fastlane.py`
- Modify: `store-assets/screenshot-editor/src/lib/store-locales.ts`

- [ ] **Step 1: Write the failing locale-contract test**

Assert the exact mapping:

```python
EXPECTED_APP_STORE_LOCALES = {
    "ar": "ar-SA", "bn": "bn-BD", "da": "da", "de": "de-DE",
    "el": "el", "en": "en-US", "es": "es-ES", "fi": "fi",
    "fr": "fr-FR", "he": "he", "hi": "hi", "hu": "hu",
    "id": "id", "it": "it", "ja": "ja", "ko": "ko",
    "nb": "no", "nl": "nl-NL", "pl": "pl", "pt": "pt-BR",
    "ro": "ro", "ru": "ru", "sv": "sv", "th": "th",
    "tr": "tr", "uk": "uk", "vi": "vi", "zh": "zh-Hant",
}
```

Also assert that `az`, `be`, `hy`, `ka`, `kk`, `ky`, `tg`, `tk`, and `uz`
are rejected for App Store staging.

- [ ] **Step 2: Run the focused test and observe the missing-module failure**

Run:

```bash
python3 -m unittest tools.store_screenshots.test_app_store_listing.AppStoreLocaleContractTest -v
```

Expected: failure because `app_store_listing.py` does not exist.

- [ ] **Step 3: Implement the canonical mapping and lookup**

Create `APP_STORE_LOCALES`, `TITLE`, `app_store_locale()`, and metadata filename
constants in the new module. Import the lookup from `stage_fastlane.py` and
remove its duplicate mapping. Keep the TypeScript mapping byte-for-byte
equivalent.

- [ ] **Step 4: Run the focused locale test**

Expected: all locale contract assertions pass.

- [ ] **Step 5: Commit**

```bash
git add tools/store_screenshots/app_store_listing.py tools/store_screenshots/test_app_store_listing.py tools/store_screenshots/stage_fastlane.py store-assets/screenshot-editor/src/lib/store-locales.ts
git commit -m "feat: define App Store locale contract"
```

### Task 2: Strict localized iOS metadata

**Files:**
- Modify: `tools/store_screenshots/app_store_listing.py`
- Modify: `tools/store_screenshots/test_app_store_listing.py`
- Create: `fastlane/metadata/ios/<28 Apple locales>/{name,subtitle,description,keywords,promotional_text,support_url,privacy_url}.txt`

- [ ] **Step 1: Write failing metadata validation tests**

Tests create a complete temporary 28-locale tree and assert:

```python
MetadataSummary(
    locales=28,
    names=28,
    subtitles=28,
    descriptions=28,
    keywords=28,
    promotional_texts=28,
    support_urls=28,
    privacy_urls=28,
)
```

Separate tests reject a missing/extra locale, translated name, invalid UTF-8,
empty field, subtitle over 30, keywords over 100, promotional text over 170,
description over 4000, and non-HTTPS URLs.

- [ ] **Step 2: Run tests and observe missing validator failures**

Run:

```bash
python3 -m unittest tools.store_screenshots.test_app_store_listing.AppStoreMetadataValidationTest -v
```

- [ ] **Step 3: Implement validation and report commands**

Add `validate_metadata()` plus `validate-metadata` and `report` CLI commands.
All reads use strict UTF-8 and all validation completes before any staging or
network operation.

- [ ] **Step 4: Add the 28 localized metadata packages**

Use the matching reviewed Google Play full description as each iOS description,
the Play short description as promotional text, and the fixed title as name.
Add natural, locale-specific subtitles of at most 30 characters and comma-
separated keyword sets of at most 100 characters. Use these HTTPS URLs in every
locale:

```text
support_url.txt: https://github.com/yet300/BlockBlast#support-me
privacy_url.txt: https://github.com/yet300/block_blast/blob/main/privacy_policy.md
```

- [ ] **Step 5: Validate the real tree and inspect character counts**

```bash
python3 tools/store_screenshots/app_store_listing.py validate-metadata --metadata-root fastlane/metadata/ios
python3 tools/store_screenshots/app_store_listing.py report --metadata-root fastlane/metadata/ios
```

Expected: exactly 28 valid locales and every field below its Apple limit.

- [ ] **Step 6: Commit**

```bash
git add tools/store_screenshots/app_store_listing.py tools/store_screenshots/test_app_store_listing.py fastlane/metadata/ios
git commit -m "feat: localize App Store super-app listing"
```

### Task 3: Atomic Apple screenshot staging

**Files:**
- Modify: `tools/store_screenshots/app_store_listing.py`
- Modify: `tools/store_screenshots/test_app_store_listing.py`
- Modify: `.gitignore`

- [ ] **Step 1: Write failing staging tests**

Build temporary `store-screenshots-<locale>` artifacts for all 34 render
locales. Assert that staging selects exactly 28 Apple locales and copies 392
files under `fastlane/screenshots/<apple-locale>/`. Assert deterministic names
such as `01-iphone-1320x2868-hero.png` and
`01-ipad-2064x2752-hero.png`.

Add failure tests for a missing Apple locale artifact, missing iPhone image,
missing iPad image, wrong filename, and destination preservation when any
preflight check fails.

- [ ] **Step 2: Run staging tests and observe missing behavior**

```bash
python3 -m unittest tools.store_screenshots.test_app_store_listing.AppStoreAssetStagingTest -v
```

- [ ] **Step 3: Implement `stage_all_app_store_assets()`**

Preflight all 28 source locales and all 392 source PNGs first. Only after a
complete preflight, replace each generated screenshot destination. Do not touch
tracked metadata files.

- [ ] **Step 4: Ignore generated screenshot directories**

Add `fastlane/screenshots/` to `.gitignore` while leaving
`fastlane/metadata/ios/` tracked.

- [ ] **Step 5: Run all Apple listing tests**

Expected: locale, metadata, and atomic staging suites pass.

- [ ] **Step 6: Commit**

```bash
git add .gitignore tools/store_screenshots/app_store_listing.py tools/store_screenshots/test_app_store_listing.py
git commit -m "feat: stage complete App Store screenshots"
```

### Task 4: Metadata-only App Store Fastlane lane

**Files:**
- Modify: `fastlane/Fastfile`
- Modify: `tools/store_screenshots/test_app_store_listing.py`

- [ ] **Step 1: Write the failing lane contract test**

Assert that `ios publish_store_listing_draft`:

- requires `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, and
  `APP_STORE_CONNECT_KEY_BASE64`;
- uses `app_store_connect_api_key` with base64 key content;
- calls `upload_to_app_store` with `metadata_path: "fastlane/metadata/ios"` and
  `screenshots_path: "fastlane/screenshots"`;
- sets `skip_binary_upload: true`, `skip_metadata: false`,
  `skip_screenshots: false`, `submit_for_review: false`,
  `automatic_release: false`, and `force: true`.

- [ ] **Step 2: Run the lane test and observe failure**

- [ ] **Step 3: Implement the iOS platform and lane**

Read `MARKETING_VERSION` from `iosApp/Configuration/Config.xcconfig`, construct
the App Store Connect API key in memory, and upload only the editable listing.
Never write the `.p8` key to the repository.

- [ ] **Step 4: Run the lane test and Ruby syntax check**

```bash
python3 -m unittest tools.store_screenshots.test_app_store_listing.AppStoreFastfileTest -v
ruby -c fastlane/Fastfile
```

- [ ] **Step 5: Commit**

```bash
git add fastlane/Fastfile tools/store_screenshots/test_app_store_listing.py
git commit -m "feat: upload App Store listing as draft"
```

### Task 5: App Store publication job in screenshot CI

**Files:**
- Modify: `.github/workflows/store-screenshots.yml`
- Modify: `store-assets/screenshot-editor/scripts/test-export-contract.mjs`

- [ ] **Step 1: Extend the failing workflow contract test**

Assert one `publish-app-store-draft` macOS job with `needs: [prepare, render]`,
`locale=all`, render-success, and `main` gates. Assert isolated artifact
download, Apple staging, API key secrets, Fastlane lane invocation, a 28/392
summary, and absence of review/release commands.

- [ ] **Step 2: Run the Node test and observe missing-job failure**

```bash
cd store-assets/screenshot-editor
npm run test:export-contract
```

- [ ] **Step 3: Implement the publication job**

Use `macos-26`, Python 3.13, Ruby 3.3, and concurrency group
`app-store-listing-draft`. Stage only after all artifacts download. Upload no
binary and do not couple its success to the independent Play publication job.

- [ ] **Step 4: Run workflow tests and YAML parsing**

Expected: Node contract passes and Ruby YAML loader parses the workflow.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/store-screenshots.yml store-assets/screenshot-editor/scripts/test-export-contract.mjs
git commit -m "ci: publish complete App Store listing draft"
```

### Task 6: Signed iOS release build and TestFlight upload

**Files:**
- Modify: `fastlane/Fastfile`
- Create: `tools/store_screenshots/test_ios_release.py`
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: Write failing Fastlane release contract tests**

Assert that `ios release` validates version/build/signing inputs, calls
`build_app` for `iosApp/iosApp.xcodeproj` and scheme `iosApp`, exports with
method `app-store`, uses the fixed bundle ID/team/profile, and calls
`upload_to_testflight(skip_waiting_for_build_processing: true)` without tester
distribution or review submission.

- [ ] **Step 2: Write the failing release workflow contract tests**

Assert a macOS iOS job depending on `version`, restoration of
`GoogleService-Info.plist`, temporary keychain/certificate/profile setup,
Fastlane invocation, IPA/dSYM artifact upload, and unconditional keychain
cleanup. Assert every required secret name and tag-derived version/build values.

- [ ] **Step 3: Run tests and observe missing lane/job failures**

```bash
python3 -m unittest tools.store_screenshots.test_ios_release -v
```

- [ ] **Step 4: Implement `ios release`**

Use the App Store Connect API key, explicit project/scheme/bundle/team/profile,
tag marketing version, run-number build number, and deterministic output paths.
Return the IPA and dSYM paths through Fastlane lane context.

- [ ] **Step 5: Implement the macOS release job**

Decode secrets only into `$RUNNER_TEMP`, import signing material into a
temporary keychain, install the provisioning profile, restore Firebase config,
run Fastlane, retain outputs for 14 days, and delete the keychain under
`if: always()`.

- [ ] **Step 6: Run focused tests, Ruby syntax, and YAML parsing**

Expected: all release contracts pass; both files parse.

- [ ] **Step 7: Commit**

```bash
git add fastlane/Fastfile .github/workflows/release.yml tools/store_screenshots/test_ios_release.py
git commit -m "ci: build and upload signed iOS releases"
```

### Task 7: Documentation and full verification

**Files:**
- Modify: `README.md`
- Modify: `store-assets/screenshot-editor/README.md`

- [ ] **Step 1: Document commands and secrets**

Document that `gh workflow run store-screenshots.yml -f locale=all` publishes
both store listing drafts from `main`; neither is sent for review. Document the
28 Apple locale/392 screenshot inventory, version prerequisite, the signed tag
build behavior, all secret setup commands, artifact retention, and expected
runtime.

- [ ] **Step 2: Run the full verification suite**

```bash
python3 -m unittest \
  tools.store_screenshots.test_verify_exports \
  tools.store_screenshots.test_stage_fastlane \
  tools.store_screenshots.test_play_listing \
  tools.store_screenshots.test_app_store_listing \
  tools.store_screenshots.test_ios_release -v
cd store-assets/screenshot-editor
npm run test:export-contract
npm run verify:project
npm run verify:localizations
npm run verify:visual
npm run build
cd ../..
ruby -c fastlane/Fastfile
ruby -e "require 'yaml'; YAML.load_file('.github/workflows/store-screenshots.yml'); YAML.load_file('.github/workflows/release.yml')"
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
git diff --check
```

- [ ] **Step 3: Audit the final tree**

Confirm exactly 28 iOS metadata directories, 34 Play directories, zero tracked
generated screenshot directories, no signing files, no secret values, and no
changes to the unrelated SwiftPM alpha02 directories.

- [ ] **Step 4: Commit**

```bash
git add README.md store-assets/screenshot-editor/README.md
git commit -m "docs: document App Store and iOS release automation"
```
