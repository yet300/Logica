# Logica — Block Puzzle

<img src="picture/app_icon.png" width="180" alt="App icon" />

A Kotlin Multiplatform super-app for Android and iOS: one lightweight host,
one catalog, and short local-first game sessions. The production bundle
currently includes Block Blast, 2048, and Fruit Merge; additional games and apps are
independent Gradle modules reviewed and allowlisted at build time.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-blue.svg)
![Compose](https://img.shields.io/badge/Compose-1.12.0-green.svg)
![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS-orange.svg)

## Download

<a href="https://apps.apple.com/us/app/logica-block-puzzle-2027/id6765924581">
  <img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" alt="Download on the App Store" height="80"/>
</a>

<a href="https://play.google.com/store/apps/details?id=ge.yet.blokblast">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play (closed testing)" height="80"/>
</a>

## Screenshots

### iPhone

<p>
  <img src="store-assets/exports/ka/iphone/1320x2868/01-hero.png" width="150" alt="Logica — თავსატეხების მზარდი სამყარო" />
  <img src="store-assets/exports/ka/iphone/1320x2868/02-device-bottom.png" width="150" alt="აირჩიე შემდეგი გამოწვევა Logica-ში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/03-device-top.png" width="150" alt="Block Blast-ის თამაში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/04-hero.png" width="150" alt="2048-ის თამაში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/05-device-bottom.png" width="150" alt="Fruit Merge-ის თამაში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/06-three-devices.png" width="150" alt="Logica-ს თამაშების კოლექცია" />
  <img src="store-assets/exports/ka/iphone/1320x2868/07-hero.png" width="150" alt="შემდეგი თავსატეხი გელოდება" />
</p>

The README shows the Georgian phone gallery. See
[Store and release operations](fastlane/README.md) for screenshot generation,
store-listing drafts, signing, artifacts, and release CI.

## Features

- 🧩 A growing catalog of puzzle games: Block Blast, 2048, and Fruit Merge
- 🎨 Polished, adaptive Material 3 UI across the host and its games
- 📱 Single codebase for Android & iOS via Compose Multiplatform
- 💾 Persistent settings and best-score tracking
- 🎉 Confetti effects on big clears
- 🎵 Bundled and procedural game audio
- 📴 Fully offline — no account required
- ⭐ Shared in-app review policy
- 📊 Firebase Analytics & Crashlytics
- 🧱 Compile-time MiniApp plugin framework with a uniform catalog and host frame
- 🔌 Contributor games discovered locally and shipped only through reviewable allowlisting

The catalog is intentionally compact: cards show the game icon, title and Play
action. Full descriptions remain in each `MiniAppManifest` for metadata,
accessibility and future detail surfaces, but are not repeated in the launcher.

## Tech Stack

- **Kotlin Multiplatform** 2.4.10 — shared business logic
- **Compose Multiplatform** 1.12.0 — declarative Android and iOS UI
- **Material 3** 1.10.0-alpha05 + **Material 3 Adaptive** 1.3.0-beta02 — design system and responsive layouts
- **Decompose** 3.5.0 + **Essenty** 2.5.0 — navigation and lifecycle
- **MVIKotlin** 4.4.0 — predictable state management
- **Metro DI** 1.4.2 — compile-time dependency injection
- **Kotlinx Coroutines** 1.11.0, **Serialization JSON** 1.11.0, and **Datetime** 0.8.0
- **Multiplatform Settings** 1.3.0 — cross-platform persistence
- **Firebase GitLive** 3.0.0-alpha02 — Analytics and Crashlytics
- **Google Mobile Ads** 25.4.0 + **User Messaging Platform** 4.0.0
- **Haze** 1.7.3, **ConfettiKit** 0.9.0, and **AboutLibraries** 15.2.0
- **Turbine** 1.2.1, **Robolectric** 4.16.1, and **Compose UI testing**
- **Baseline Profiles** — Android startup performance
- **Store screenshot editor** — Next.js 15.0.3, Playwright 1.62.1, and `html-to-image`, built from [ParthJadhav/app-store-screenshots](https://github.com/ParthJadhav/app-store-screenshots)

## Project Structure

```
BlockBlast/
├── androidApp/      # Android entry point (Activity, manifest, ads, Firebase)
├── iosApp/          # iOS entry point (SwiftUI host)
├── composeApp/      # Shared host UI, MiniApp frame and platform composition
├── core/
│   ├── common/      # Shared utilities
│   ├── domain/      # Reusable domain contracts
│   ├── data/        # Settings and shared repositories
│   └── telemetry/   # Analytics and crash facade
├── feature/
│   ├── catalog/     # Registry-backed MiniApp catalog
│   ├── root/        # Catalog/running navigation and host lifecycle
│   ├── review/      # App review policy
│   └── settings/    # Host settings
├── game/
│   ├── blockblast/       # Allowlisted Block Blast MiniApp
│   ├── twentyfortyeight/ # Allowlisted 2048 MiniApp
│   └── fruitmerge/       # Allowlisted Fruit Merge MiniApp
├── miniapp/
│   ├── api/         # Stable Compose-free contracts
│   ├── compose/     # Plugin, manifest, session and frame contracts
│   ├── metro/       # Registry and retained child-graph foundation
│   ├── storage/     # Namespaced persistence and safe all-game-data reset
│   ├── audio/       # Portable procedural Music/SFX API and runtime
│   ├── audio-presets/ # Original reusable instruments, soundscapes and SFX
│   ├── bundle/      # Production allowlisted plugins
│   ├── testkit/     # Contributor contract fixtures
│   └── samples/     # Discovered but unshipped examples
├── build-logic/     # MiniApp discovery, scaffold and convention plugins
└── fastlane/        # Store metadata, screenshots, release automation and operations docs
```

## Getting Started

### Prerequisites
- **JDK 17+**
- **Android Studio** Ladybug or later
- **Xcode 15+** (iOS, macOS only)
- Your own Firebase project — see below

### Firebase setup

This repo does **not** ship Firebase config files. Create your own Firebase project and drop in:

- `androidApp/google-services.json`
- `iosApp/iosApp/GoogleService-Info.plist`

### Build and Run

#### Android

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

#### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run.

## Development

### Tests

```bash
./gradlew test
```

### Create a game / MiniApp

If you are a human contributor or an AI agent and the request is to create,
add or port a game, start with the official scaffold workflow below. Do not
create an arbitrary Gradle module and do not add the game to the production
allowlist automatically.

```bash
./gradlew createMiniApp -PminiAppId=game.snake -PminiAppName=Snake
./gradlew :game:snake:verifyMiniApp
```

For a game-oriented starting point with immutable state, typed actions, a pure
engine seam and focused component tests, add `-PminiAppProfile=game` to the
first command. The default profile is intentionally smaller and keeps the
existing basic scaffold behavior.

The first command creates reviewable source. The next Gradle invocation
discovers `game/*` and `miniapp/samples/*`, but discovery does not ship a
plugin. `verifyMiniApp` checks the module boundary, tests and Android/iOS
compilation. After review, a maintainer adds exactly one matching entry to the
root `miniApps` allowlist. The catalog is compiled into the app; there is no
server, runtime download or remote code loading.

The stable policy and rationale are recorded in
[ADR-0001: MiniApp Contribution and Shipping Workflow](docs/adr/0001-miniapp-contribution-and-shipping.md).
Use the [human MiniApp contributor guide](CONTRIBUTING_MINIAPP.md) for the
end-to-end workflow and the [AI contributor protocol](docs/miniapp/AI_CONTRIBUTOR_PROTOCOL.md)
for deterministic agent behavior. The current agent-level architecture and
verification rules are in [AGENTS.md](AGENTS.md). The guides implement the
approved [contributor pipeline design](docs/superpowers/specs/2026-08-23-miniapp-contributor-pipeline-design.md).

Plugins depend only on MiniApp contracts, approved inward core contracts and
typed host capabilities. Root owns Catalog/Running navigation, Back,
Settings/Review, visibility and stale-callback protection. The common frame
owns catalog cards, toolbar controls and ad containers; Replay is intentionally
not part of the initial plugin API.

### Asset and icon timing

Do not spend the beginning of a MiniApp implementation on its product or game
icon. Build and review rules, persistence, lifecycle, compact/wide UI,
accessibility and CI first. Add the final icon only after that surface is
stable. The approved workflow is to write an icon brief, generate/review an
SVG with [QuiverAI](https://app.quiver.ai/), and convert the approved SVG with
[Valkyrie](https://github.com/ComposeGears/Valkyrie) to Compose/Android vector
resources. A Gemini Flash-class model may assist a bounded SVG-to-Android-XML
conversion, but the result still requires human review, Valkyrie validation
and provenance. Record the source, license, tool/version, brief, date and hash
in the MiniApp provenance file. Experimental open SVG models are research
inputs, not build or runtime dependencies; evaluate their license and
reproducibility before adopting one.

For a broader, evidence-based review of the super-app architecture and CI,
use the [architecture audit agent prompt](docs/miniapp/super-app-architecture-audit-agent.md).

Use the session-bound `MiniAppStorage` from `MiniAppSessionContext` for new
persistence. Games supply only local snake-case names; the host owns physical
namespacing, snapshot migrations, legacy aliases and the safe all-game-data
reset. MiniApp modules must not depend on Multiplatform Settings directly.
Block Blast's legacy physical keys remain unchanged for save compatibility.

### Create procedural music and SFX

MiniApps can declare original, asset-free Music and SFX in shared Kotlin for
Android and iOS. Start with the
[procedural audio getting-started guide](docs/miniapp/audio/getting-started.md),
then use the [Kotlin DSL reference](docs/miniapp/audio/kotlin-dsl.md),
[shared presets](docs/miniapp/audio/instruments.md), and
[mobile budgets](docs/miniapp/audio/performance-budgets.md).

AI agents must use the repo-local
[MiniApp procedural audio skill](.agents/skills/miniapp-procedural-audio/SKILL.md).
It prioritizes preset reuse, original declarations, deterministic seeds,
session lifecycle correctness and compiled examples. Engine maintainers should
read the approved
[architecture design](docs/superpowers/specs/2026-08-23-kotlin-pattern-audio-design.md)
and [implementation plan](docs/superpowers/plans/2026-08-23-miniapp-procedural-audio.md);
MiniApp authors must not depend on internal DSP or platform sinks.

Block Blast deliberately keeps its original bundled MP3 music and voice clips
through the app-owned legacy `AudioRepository`. This is not a reusable MiniApp
capability or a contributor pattern; new MiniApps use the procedural API unless
a separate architecture decision introduces a general asset-audio contract.

## Contributing

Contributions are welcome. MiniApps generated by the command above remain
unshipped until their source, dependency boundary and allowlist change are
reviewed.

<a id="support-me"></a>

## Sponsor Logica

Logica is looking for sponsors who want to help fund independent development.
Direct financial support is welcome, as are AI API credits/tokens, CI capacity,
test devices, localization, and design support. Sponsorship does not grant
control over the production MiniApp allowlist or project direction.

For sponsorship or in-kind support, contact
[ryaeh7282@gmail.com](mailto:ryaeh7282@gmail.com).

- **ton**: UQCi1XMdZP2fBfTK-O6rsAX3fXEm5iBpjO1D6FDekdUDQnaw
- **btc**: bc1qv2m03vg23227yfnlu0c0jx2ps5yg8v8kvy748s
- **eth**: 0xdF196759E996Fe684c33416282F30d6B9A0b325e
- **usdt(erc20)**: 0xdF196759E996Fe684c33416282F30d6B9A0b325e
- **bnb**: 0xdF196759E996Fe684c33416282F30d6B9A0b325e
- **usdt(trc20)**: TYrBMc4yN4k8im2Qq2G17hv9VdmcYFngpT

## License

This project is open source and available under the MIT License.

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/kotlin-multiplatform/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [MVIKotlin](https://github.com/arkivanov/MVIKotlin)
- [Decompose](https://github.com/arkivanov/Decompose)
- [Metro](https://github.com/ZacSweers/metro)
