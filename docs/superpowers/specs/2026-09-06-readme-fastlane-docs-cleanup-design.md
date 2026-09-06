# README and Store Automation Documentation Cleanup Design

## Goal

Keep the repository root README focused on Logica as a product and contributor
project. Move operational screenshot, store-listing, signing, and release
instructions into Fastlane-owned documentation. Remove command-line surfaces
that are not used by GitHub Actions or another retained automation path.

## Root README

The root README will:

- retain one compact Georgian iPhone screenshot gallery;
- remove the complete iPad gallery;
- remove detailed screenshot-editor, locale-matrix, store-upload, secret-setup,
  timing, artifact-retention, and signed-iOS-release instructions;
- link to `fastlane/README.md` for store and release operations;
- describe the current three-game catalog: Block Blast, 2048, and Fruit Merge;
- update Kotlin and Compose versions from the version catalog;
- list the principal runtime, architecture, UI, service, and testing libraries;
- credit [ParthJadhav/app-store-screenshots](https://github.com/ParthJadhav/app-store-screenshots)
  as the foundation/tooling used to create the app's marketing screenshots;
- update the repository tree so it includes Fruit Merge and identifies Fastlane
  as the home of store metadata, screenshots, and release documentation.

The README will not duplicate copy-paste release commands or secret setup.

## Fastlane Documentation

Create `fastlane/README.md` as the canonical operational guide for:

- the manual `store-screenshots.yml` workflow;
- the difference between one-locale artifact generation and the `locale=all`
  draft publication path;
- Google Play and App Store Connect metadata/screenshot staging;
- required GitHub secrets, without secret values;
- the tag-driven Android and iOS release jobs;
- iOS certificate, provisioning-profile, and temporary-keychain behavior;
- expected runtime, artifact contents, and 14-day retention;
- safety guarantees: store listing edits remain drafts and iOS builds are not
  submitted for review or automatically released.

The screenshot editor README remains responsible for editor-specific controls,
local development, export internals, visual contracts, and performance details.
It may link to the Fastlane guide instead of repeating release setup.

## Tool Surface Cleanup

Retain only executable Python entry points required by CI:

1. `verify_exports.py` for validating generated PNG bundles;
2. `play_listing.py stage` for validating Play metadata and atomically staging
   all Play assets;
3. `app_store_listing.py stage` for validating App Store metadata and staging
   all Apple screenshots.

Remove:

- `stage_fastlane.py` and its dedicated test;
- the unused Fastlane `stage_store_assets` lane;
- the `report` and standalone `validate-metadata` CLI commands from the Play and
  App Store scripts.

The underlying metadata validation functions remain because both retained
`stage` paths depend on them. Unit tests continue to cover validation directly,
while tests for deleted CLI surfaces are removed or rewritten around retained
behavior. Documentation and plans are historical records and will not be
rewritten solely to erase references to commands used during their execution.

## Technology Stack

The root stack will be concise and sourced from `gradle/libs.versions.toml` and
the screenshot editor package. It will cover:

- Kotlin Multiplatform and Compose Multiplatform;
- Material 3 and Material 3 Adaptive;
- Decompose, Essenty, MVIKotlin, and Metro;
- Kotlinx Coroutines, Serialization, and Datetime;
- Multiplatform Settings;
- Firebase Analytics and Crashlytics through GitLive;
- Google Mobile Ads and User Messaging Platform;
- Haze, ConfettiKit, and AboutLibraries;
- Turbine, Robolectric, and Compose UI testing;
- the `app-store-screenshots`-based Next.js editor used for store marketing
  assets.

Only major libraries useful to readers are listed; the README will not mirror
every transitive dependency or the entire version catalog.

## Sponsorship

Replace the generic `Support Me` heading with a clearer `Sponsor Logica`
section. It will state that the project welcomes:

- direct financial sponsorship;
- AI API credits/tokens used for development, localization, testing, and
  contributor tooling;
- CI capacity, devices, design, and localization support.

Commercial or in-kind sponsorship enquiries use
`ryaeh7282@gmail.com`. Existing TON, BTC, ETH, USDT, and BNB wallet addresses
remain at the bottom as direct donation options. The section must not claim an
existing GitHub Sponsors program, promise endorsements, or imply that sponsors
control the production MiniApp allowlist.

## Verification

- Assert the root README no longer contains an iPad heading or operational
  secret/setup command blocks.
- Assert the Fastlane README documents the retained workflows and secret names.
- Assert every executable Python tool under `tools/store_screenshots` is used by
  CI/Fastlane or is a test for retained behavior.
- Run the complete store-screenshot Python suite.
- Run screenshot editor contract, project, localization, visual, and production
  build checks.
- Validate both GitHub workflow YAML files and Fastlane syntax.
- Run both metadata validators through their retained staging/unit-test paths.
- Run `git diff --check`.

