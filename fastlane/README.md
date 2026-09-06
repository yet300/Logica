# Store and Release Operations

This directory is the canonical home for Google Play and App Store Connect
metadata, generated screenshot staging, and release automation. Credentials
belong in GitHub Actions secrets and must never be committed to the repository.

## Screenshot and listing workflow

### Version bump and localized What's New

Keep one translation source per release in
`fastlane/metadata/release-notes/<version>.json`. It must contain every iOS
locale key. Generate all App Store Connect `release_notes.txt` files, Google
Play changelogs, and bump both platform version files with:

```bash
python3 tools/store_screenshots/release_metadata.py \
  --version-name 2.0.0 \
  --version-code 15 \
  --notes fastlane/metadata/release-notes/2.0.0.json
```

The iOS draft lane validates that every locale has a non-empty
`release_notes.txt` before contacting App Store Connect.

The manual **Store screenshots** workflow renders the localized marketing
assets. Run one locale when reviewing copy or artwork:

```bash
gh workflow run store-screenshots.yml -f locale=ka
```

A single-locale run builds and validates one downloadable artifact. It never
publishes a partial store listing.

Run the complete matrix when preparing both store listings:

```bash
gh workflow run store-screenshots.yml -f locale=all
```

An all-locale run renders 34 locales with at most four jobs in parallel. Draft
publication runs only when the workflow is dispatched from `main` and every
render succeeds. Google Play and App Store Connect publication jobs then run
independently.

Google Play stages 34 metadata packages, seven phone screenshots per locale,
and 34 feature graphics. Fastlane uploads the listing with
`changes_not_sent_for_review: true`; it uploads no APK or AAB and changes no
release track.

App Store Connect stages 28 supported metadata packages, seven iPhone and seven
iPad screenshots per locale. Fastlane uses `skip_binary_upload: true`,
`submit_for_review: false`, and `automatic_release: false`. Before an
all-locale run, `MARKETING_VERSION` in
`iosApp/Configuration/Config.xcconfig` must identify the editable App Store
version being prepared.

Both store changes remain drafts until a human explicitly submits them for
review.

### Listing secrets

Configure these GitHub repository secrets by name. Do not put their values in
documentation, workflow inputs, logs, or committed files.

- `PLAY_STORE_JSON_KEY`
- `APP_STORE_CONNECT_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_KEY_CONTENT`

### Runtime and artifacts

A complete cold render and both draft uploads are expected to take approximately
20–45 minutes. Cached runs can be faster. The workflow writes actual complete-job
and render durations to the GitHub Actions summary.

Each locale artifact contains its timing manifest and the canonical upload
assets: seven 6.9-inch iPhone screenshots, seven 13-inch iPad screenshots,
seven Android phone screenshots, and one Google Play feature graphic. GitHub
retains these artifacts for 14 days. Complete generated locale bundles are not
kept permanently in Git; the repository retains only the intentional visual
baselines and README gallery assets.

## Tag-driven releases

Pushing a `vX.Y.Z` tag starts Android and iOS release jobs independently. The
tag supplies version `X.Y.Z`; the GitHub run number supplies the monotonically
increasing build/version code.

The Android job builds a signed AAB and APK, uploads the AAB as a draft to the
Google Play internal track, and attaches the APK to the GitHub Release. It
requires:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `GOOGLE_SERVICES_JSON`
- `PLAY_STORE_JSON_KEY`

The iOS job builds an App Store-signed IPA and uploads the build to App Store
Connect/TestFlight. It does not distribute the build to testers, attach it to
an App Store version, submit it for review, or release it automatically. The
IPA and dSYM are retained as GitHub Actions artifacts for 14 days. It requires:

- `GOOGLE_SERVICE_INFO_PLIST`
- `IOS_DIST_CERT_P12`
- `IOS_DIST_CERT_PASSWORD`
- `IOS_PROVISIONING_PROFILE`
- `IOS_PROVISIONING_PROFILE_NAME`
- `APP_STORE_CONNECT_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_KEY_CONTENT`

The certificate must be an Apple Distribution certificate, and the provisioning
profile must target `ge.yet3.blokblast.BlockBlast` for team `3KKQ642Q9H`. CI
generates a random temporary-keychain password for every run. The keychain and
installed provisioning profile are removed by an `always()` cleanup step even
when signing or upload fails.
