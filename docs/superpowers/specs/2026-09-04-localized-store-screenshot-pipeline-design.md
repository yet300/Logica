# Localized Store Screenshot Pipeline Design

## Goal

Localize the existing Quiet Editorial store screenshot narrative for every
locale shipped by Logica without committing hundreds of megabytes of generated
PNG files. Keep English and Georgian review imagery available directly in the
README, and make every other store-ready set reproducible through CI.

## Supported locales

The screenshot project follows the exact locale intersection currently present
in `composeApp`, `feature/catalog`, Block Blast, 2048, and Fruit Merge:

`ar`, `az`, `be`, `bn`, `da`, `de`, `el`, `en`, `es`, `fi`, `fr`, `he`, `hi`,
`hu`, `hy`, `id`, `it`, `ja`, `ka`, `kk`, `ko`, `ky`, `nb`, `nl`, `pl`, `pt`,
`ro`, `ru`, `sv`, `tg`, `th`, `tk`, `tr`, `uk`, `uz`, `vi`, and `zh`.

The screenshot locale identifiers are application/editor identifiers. A
Fastlane staging step maps them to platform store identifiers where necessary,
for example `en` to `en-US`, `ka` to Google Play `ka-GE`, `pt` to `pt-BR`, and
`zh` to the explicitly selected Chinese store variant. Georgian is not staged
as App Store metadata because App Store Connect does not support a Georgian
product-page localization.

## Localization contract

Each of the seven-slide decks keeps the approved narrative and layout. Labels
and headlines receive independently authored, concise translations with
intentional line breaks. The Google Play feature graphic uses the localized
hero claim.

English remains the fallback and editor default. Right-to-left locales (`ar`
and `he`) use the editor's RTL presentation. Automated validation requires a
non-empty label and headline for every applicable slide and locale; fallback
text is not considered a completed translation.

The existing English iPhone and iPad captures remain shared visual sources for
this phase. Localization changes the marketing copy around the device rather
than duplicating identical source PNGs 37 times. Fully localized in-app source
captures are a separate future capture pass. Before such a pass, known weak
application translations, including the Georgian 2048 title and description,
must receive native-language QA.

## Source and generated assets

Tracked source of truth:

- `store-assets/screenshot-editor/app-store-screenshots.json` for decks and
  localized copy;
- the screenshot editor and its verification/export scripts;
- one shared set of source application captures;
- English exports used as the canonical visual baseline;
- Georgian iPhone and iPad exports displayed in the README.

Generated exports for other locale/device/size combinations are not committed.
They are ignored by Git and reproduced from the tracked project state.

The README gains a Georgian screenshot section using one canonical iPhone size
and one canonical iPad size. These files are deliberately retained because a
README cannot reliably embed expiring authenticated CI artifacts.

## Headless export

Add a deterministic browser-driven export command around the existing Next.js
editor. It accepts a locale and output directory, renders iPhone, iPad, Android
phone, and feature graphic decks, and produces the same zero-padded filenames
and dimensions as the interactive exporter.

The command must:

1. validate that the requested locale is configured;
2. start from the checked-in project JSON and shared source captures;
3. wait for fonts and images before capture;
4. fail on missing images, blank output, wrong dimensions, or export errors;
5. arrange output in locale-first staging directories usable by the Fastlane
   synchronization step;
6. avoid modifying the checked-in project file.

## GitHub Actions workflow

A manually dispatched workflow supports either one locale or all locales. An
all-locales run fans out by locale with bounded parallelism. Each job installs
the locked JavaScript dependencies and browser runtime, builds the editor,
runs the headless exporter and validators, then uploads one locale artifact.

Artifacts are retained for 14 days. They contain store-ready PNGs and a small
manifest recording locale, platform mapping, dimensions, filenames, source
commit, and generation time. The workflow does not publish to either store and
does not require store credentials.

## Fastlane boundary

`store-assets` remains the authoring system. Fastlane receives only staged
outputs. A local staging command maps generated artifacts into:

- `fastlane/screenshots/<app-store-locale>/` for supported App Store locales;
- `fastlane/metadata/android/<play-locale>/images/phoneScreenshots/`;
- `fastlane/metadata/android/<play-locale>/images/featureGraphic.png`.

Existing release lanes continue to skip screenshots and images. Uploading store
art remains an explicit later action so generating screenshots cannot
accidentally mutate a live listing.

## Verification

Verification covers:

- exact equality between application and screenshot locale sets;
- complete localized copy for all slides and the feature graphic;
- correct RTL classification;
- deterministic platform-locale mapping;
- expected output count, dimensions, RGB color mode, filename ordering, and no
  transparent or blank exports;
- valid English and Georgian README image references;
- a clean Git status apart from known unrelated user files after regeneration.

## Storage decision

Do not commit all localized exports and do not introduce Git LFS. A single
English export set currently occupies about 22 MB, so 37 committed sets would
add roughly 814 MB before Git history overhead. CI artifacts provide bounded,
disposable storage while the small tracked source set keeps every export
reproducible.
