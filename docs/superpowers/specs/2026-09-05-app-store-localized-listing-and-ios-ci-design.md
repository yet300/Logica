# App Store Localized Listing and iOS CI Design

## Objective

Extend the existing store automation so Logica has the same controlled delivery
path on iOS as it has on Google Play:

- localized App Store metadata for every application language Apple accepts;
- localized 6.9-inch iPhone and 13-inch iPad screenshot decks;
- a manual App Store Connect metadata/screenshot upload that never submits for
  App Review;
- a signed iOS archive and IPA on version tags;
- automatic build upload to App Store Connect/TestFlight without selecting the
  build for review or releasing it.

Google Play behavior remains unchanged.

## Product-page positioning

The iOS listing uses the already approved super-app positioning: Logica is a
growing, calm collection of puzzle games rather than a single Block Blast game.
The public title remains exactly:

```text
Logica — Block Puzzle
```

Each Apple localization receives a natural subtitle, description, promotional
text, and keyword set. The description is adapted from the corresponding
reviewed Google Play translation. Copy must not claim a fixed number of games,
cognitive or medical benefits, awards, ratings, an ad-free experience, or
features absent from the shipping build.

## Supported App Store localizations

Apple accepts 28 of the 37 application UI languages:

`ar-SA`, `bn-BD`, `da`, `de-DE`, `el`, `en-US`, `es-ES`, `fi`, `fr-FR`, `he`,
`hi`, `hu`, `id`, `it`, `ja`, `ko`, `no`, `nl-NL`, `pl`, `pt-BR`, `ro`, `ru`,
`sv`, `th`, `tr`, `uk`, `vi`, and `zh-Hant`.

The application locales `az`, `be`, `hy`, `ka`, `kk`, and `ky` remain in the
Google Play pipeline but are excluded from App Store metadata because Apple
does not accept them. `tg`, `tk`, and `uz` remain app-only UI localizations and
are excluded from both store pipelines.

The application-locale-to-App-Store-locale mapping has one canonical Python
owner. Screenshot editor TypeScript data must match it, and tests must reject
mapping drift.

## Tracked metadata

Reviewable source files live at:

```text
fastlane/metadata/ios/<apple-locale>/
├── name.txt
├── subtitle.txt
├── description.txt
├── keywords.txt
├── promotional_text.txt
├── support_url.txt
└── privacy_url.txt
```

Validation is strict and offline. It rejects missing or extra locales, invalid
UTF-8, an altered title, empty values, and Apple field-limit violations:

- name: 2–30 characters;
- subtitle: 1–30 characters;
- keywords: 1–100 characters;
- promotional text: 1–170 characters;
- description: 1–4000 characters;
- support and privacy values: absolute HTTPS URLs.

The support URL is the repository support page and the privacy URL is the
existing application privacy-policy URL. Generated screenshot directories are
ignored by Git and staged only inside CI.

## Screenshot staging and App Store draft upload

The existing screenshot render matrix remains the only renderer. A complete
`locale=all` run produces artifacts for all 34 store-supported application
locales. The App Store staging boundary selects the 28 Apple-supported locales,
requires all 14 canonical Apple images for each locale, and writes them to
Fastlane's language directories. Each directory contains seven 1320×2868
iPhone images and seven 2064×2752 iPad images with deterministic names.

After every render succeeds, `store-screenshots.yml` runs a single macOS
`publish-app-store-draft` job only on `main` and only for `locale=all`. It:

1. downloads every localized render artifact;
2. validates the complete iOS metadata inventory;
3. stages all 392 screenshots (28 × 14);
4. authenticates with an App Store Connect API team key;
5. calls a metadata-only Fastlane lane with binary upload disabled;
6. keeps `submit_for_review` and automatic release disabled;
7. writes locale, screenshot, and review-state counts to the Actions summary.

The version edited in App Store Connect is read from
`iosApp/Configuration/Config.xcconfig`. The repository must therefore contain
the next editable marketing version before the manual all-locale publication
is run. A single-locale render remains artifact-only and never publishes a
partial App Store listing.

## Signed iOS build and upload

The tag-driven `release.yml` gains an independent macOS iOS job alongside the
Android job. For tag `vX.Y.Z`, it:

1. restores the iOS Firebase configuration from an encrypted repository secret;
2. creates a temporary keychain;
3. imports an Apple Distribution PKCS#12 certificate;
4. installs the App Store provisioning profile;
5. resolves Gradle and Swift Package dependencies;
6. archives the shared Kotlin/Compose framework and SwiftUI host with marketing
   version `X.Y.Z` and build number `GITHUB_RUN_NUMBER`;
7. exports a signed App Store IPA and dSYM;
8. uploads the IPA to App Store Connect/TestFlight with the API key;
9. does not distribute to testers, select the build for an App Store version,
   submit for review, or release it;
10. retains IPA and dSYM as short-lived workflow artifacts and deletes the
    temporary keychain even on failure.

Fastlane owns build parameters and upload semantics; GitHub Actions owns secret
materialization and artifact retention. The bundle identifier stays
`ge.yet3.blokblast.BlockBlast`, the Xcode scheme stays `iosApp`, and the
development team stays `3KKQ642Q9H`.

## Required GitHub secrets

The implementation documents and validates these secrets without printing
their contents:

- `APP_STORE_CONNECT_KEY_ID`;
- `APP_STORE_CONNECT_ISSUER_ID`;
- `APP_STORE_CONNECT_KEY_BASE64` (base64-encoded `.p8` team key);
- `IOS_DISTRIBUTION_CERTIFICATE_BASE64` (base64-encoded `.p12`);
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`;
- `IOS_PROVISIONING_PROFILE_BASE64`;
- `IOS_PROVISIONING_PROFILE_NAME`;
- `IOS_KEYCHAIN_PASSWORD`;
- `GOOGLE_SERVICE_INFO_PLIST_BASE64`.

Metadata-only publication needs only the three App Store Connect key secrets.
Signed tag builds need the complete set.

## Failure and safety behavior

- No network call occurs until every required metadata file and screenshot is
  validated locally.
- Missing secrets fail with explicit variable names but never reveal values.
- App Store and Play publication jobs are independent; failure in one does not
  weaken the other's validation or submission settings.
- No lane contains `submit_for_review: true`, `automatic_release: true`, or an
  App Store release action.
- No signing material or generated IPA is committed.
- Existing unrelated SwiftPM working-tree directories remain untouched.

## Verification

Automated tests cover the exact 28-locale mapping, metadata field limits,
atomic staging, deterministic screenshot names, Fastlane draft/build settings,
workflow gates, required secrets, tag-derived versions, and the absence of any
review submission. Verification also includes Ruby syntax, YAML parsing, the
screenshot editor production build, the existing screenshot/localization test
suites, an unsigned simulator host build, and an unsigned device archive where
the local environment permits it. Actual signed export and App Store Connect
upload require repository secrets and are verified by the first controlled tag
run.
