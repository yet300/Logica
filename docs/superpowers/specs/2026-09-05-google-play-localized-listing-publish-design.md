# Google Play Localized Listing Draft Publish Design

## Goal

Extend the manual store-screenshot workflow so one all-locale invocation
generates every screenshot deck and then uploads a complete, localized Google
Play listing edit for Logica. The edit must remain in Play Console as changes
not sent for review. App Store upload is explicitly out of scope.

The triggering command remains:

```bash
gh workflow run store-screenshots.yml -f locale=all
```

A single-locale invocation remains generation-only. It must never publish a
partial set of listing localizations.

## Product positioning and copy

The listing must represent the current product as a growing collection of
puzzle games rather than a single Block Blast game or a fixed collection of
three games.

The title is identical in every locale and is not translated:

```text
Logica — Block Puzzle
```

The English source short description is:

```text
A growing collection of relaxing puzzle games for focus, strategy, and fun.
```

The English source full description follows this message hierarchy:

1. Logica is one place for a growing collection of puzzle games.
2. Block Blast, 2048, and Fruit Merge are current examples, not the product's
   permanent limit.
3. Sessions are quick to start, controls are simple, progress and personal
   bests are retained, and the application works offline.
4. The closing call to action invites the player to choose their next puzzle.

The description must not make unverified claims about cognitive improvement,
awards, user counts, ratings, being ad-free, or the exact number of games.

## Localizations

The metadata locale set exactly matches the application's 37 supported
locales:

`ar`, `az`, `be`, `bn`, `da`, `de`, `el`, `en`, `es`, `fi`, `fr`, `he`, `hi`,
`hu`, `hy`, `id`, `it`, `ja`, `ka`, `kk`, `ko`, `ky`, `nb`, `nl`, `pl`, `pt`,
`ro`, `ru`, `sv`, `tg`, `th`, `tk`, `tr`, `uk`, `uz`, `vi`, and `zh`.

Each locale receives a culturally natural short and full description rather
than a word-for-word mechanical translation. Brand names and current game names
remain recognizable. The title remains the exact English title in every
locale. Right-to-left scripts remain plain UTF-8 text and require no special
Fastlane representation.

Tracked metadata uses Fastlane's conventional layout:

```text
fastlane/metadata/android/<play-locale>/
├── title.txt
├── short_description.txt
└── full_description.txt
```

Existing English changelogs remain untouched. Generated `images/` directories
are staging output and are not committed.

## Validation boundary

A repository validator owns the application-locale to Google-Play-locale map
and verifies the entire metadata package before any network call. Validation
must reject:

- a missing, extra, or duplicate locale;
- a title that differs from `Logica — Block Puzzle`;
- a title longer than 30 characters;
- an empty or over-80-character short description;
- an empty or over-4000-character full description;
- invalid UTF-8 or missing required files;
- missing Android screenshots or feature graphic for any locale.

The workflow must stage and validate all 37 locales before invoking Fastlane.
This makes publication atomic from the workflow's perspective: a partial render
cannot produce a partial listing upload.

## Workflow architecture

The existing `prepare` and `render` jobs remain responsible for resolving the
locale matrix, rendering 50 PNG variants per locale, validating dimensions and
color mode, and retaining canonical artifacts.

A new `publish-play-draft` job:

1. runs only when the workflow input is exactly `locale=all`;
2. depends on every matrix render job succeeding;
3. downloads all `store-screenshots-*` artifacts into isolated directories;
4. stages seven Android phone screenshots and one feature graphic per locale
   under the matching Fastlane metadata locale;
5. validates the complete 37-locale metadata and image inventory;
6. calls one Fastlane lane with the existing `PLAY_STORE_JSON_KEY` secret;
7. uploads no APK or AAB and does not modify release tracks;
8. commits the Google Play edit with `changes_not_sent_for_review: true`;
9. writes the staged locale and asset counts to the Actions summary.

The publish job is deliberately singular. Running one Supply edit avoids 37
parallel jobs competing to replace the same listing's screenshots and metadata.

## Fastlane behavior

Add an Android lane dedicated to listing publication. It calls
`upload_to_play_store` with:

- package `ge.yet.blokblast`;
- the staged Android metadata directory;
- `skip_upload_apk: true` and `skip_upload_aab: true`;
- metadata, images, and screenshots enabled;
- changelog upload disabled;
- SHA-256 image synchronization enabled;
- `changes_not_sent_for_review: true`;
- no binary, rollout, or track mutation.

Fastlane Supply replaces the existing screenshots in filename order rather
than appending duplicates. The zero-padded screenshot filenames therefore
remain authoritative for carousel ordering.

## Credentials and failure behavior

The workflow reuses the repository secret `PLAY_STORE_JSON_KEY`. It is passed
directly as `json_key_data` and must never be written to a tracked file or
printed. The service account needs permission to edit the application's store
listing but does not need production-release authority for this workflow.

If the secret is absent, a render fails, metadata is incomplete, an artifact is
missing, or Google rejects the edit, the workflow fails visibly. It must not
silently fall back to publishing only the available locales. Render artifacts
remain downloadable for diagnosis.

`changes_not_sent_for_review: true` means the committed edit stays pending in
Play Console until a human explicitly sends it for review. The workflow does
not promise that Google will label every UI state with the word “draft”; the
guarantee is that it does not request review.

## Tests and verification

Automated tests cover:

- the exact 37-locale metadata inventory and locale mappings;
- Play title, short-description, and full-description limits;
- staging from the per-locale GitHub artifact directory structure;
- seven ordered phone screenshots plus one feature graphic per locale;
- preservation of existing changelogs;
- Fastlane lane syntax and upload flags;
- workflow gating so only `locale=all` reaches the publish job;
- absence of App Store upload and binary upload behavior;
- failure before Fastlane when any locale or asset is incomplete.

The real Google Play API call cannot be exercised locally without external
credentials. Final operational verification is one manually dispatched run on
the merged workflow, followed by confirming in Play Console that all changes
exist and are not sent for review.

## Expected runtime

The existing measured rendering phase remains approximately 15–25 minutes for
all locales on warm GitHub-hosted runners, or 20–30 minutes for a cold first
run. The single publication job adds artifact download, staging, validation,
and Google Play upload. Its exact duration depends primarily on artifact
transfer and the Play API; budget an additional 5–15 minutes. The expected
end-to-end wall time is therefore approximately 20–40 minutes, with the Actions
summary providing the authoritative measurement after the first real run.
