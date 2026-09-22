# Funfolio: Offline Games

<img src="picture/app_icon.png" width="180" alt="App icon" />

An open-source, offline-first game collection for Android and iOS: one
lightweight host, one catalog, and short local-first sessions. The production
bundle currently includes Block Blast, 2048, and Fruit Merge; contributors can
build and propose additional games as independent Gradle modules, which ship
only after review and allowlisting.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-blue.svg)
![Compose](https://img.shields.io/badge/Compose-1.12.0-green.svg)
![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS-orange.svg)

## Screenshots

<p>
  <img src="store-assets/exports/ka/iphone/1320x2868/01-hero.png" width="150" alt="Funfolio — თავსატეხების მზარდი სამყარო" />
  <img src="store-assets/exports/ka/iphone/1320x2868/02-device-bottom.png" width="150" alt="აირჩიე შემდეგი გამოწვევა Funfolio-ში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/03-device-top.png" width="150" alt="Block Blast-ის თამაში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/04-hero.png" width="150" alt="2048-ის თამაში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/05-device-bottom.png" width="150" alt="Fruit Merge-ის თამაში" />
  <img src="store-assets/exports/ka/iphone/1320x2868/06-three-devices.png" width="150" alt="Funfolio-ს თამაშების კოლექცია" />
  <img src="store-assets/exports/ka/iphone/1320x2868/07-hero.png" width="150" alt="შემდეგი თავსატეხი გელოდება" />
</p>

The README shows the Georgian phone gallery. See
[Store and release operations](fastlane/README.md) for screenshot generation,
store-listing drafts, signing, artifacts, and release CI.

## Download

[<img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" alt="Download on the App Store" height="60">](https://apps.apple.com/us/app/id6765924581)
&nbsp;
[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play (closed testing)" height="60">](https://play.google.com/store/apps/details?id=ge.yet.blokblast)

## Features

- 🧩 A growing catalog of puzzle games: Block Blast, 2048, and Fruit Merge
- 🎨 Polished, adaptive Material 3 UI across the host and its games
- 📱 Single codebase for Android & iOS via Compose Multiplatform
- 💾 Persistent settings and best-score tracking
- 🎉 Confetti effects on big clears
- 🎵 Bundled and procedural game audio
- 📴 Fully offline — no account required
- 🛠️ Open source — build and propose the next game
- ⭐ Shared in-app review policy
- 📊 Firebase Analytics & Crashlytics
- 🧱 Compile-time MiniApp plugin framework with a uniform catalog and host frame
- 🔌 Contributor games discovered locally and shipped only through reviewable allowlisting

The catalog is intentionally compact: cards show the game icon, title and Play
action. Full descriptions remain in each `MiniAppManifest` for metadata,
accessibility and future detail surfaces, but are not repeated in the launcher.

## Tech Stack

- **Kotlin Multiplatform** 2.4.20 — shared business logic
- **Compose Multiplatform** 1.12.0 — declarative Android and iOS UI
- **Material 3** 1.10.0-alpha05 + **Material 3 Adaptive** 1.3.0-rc01 — design system and responsive layouts
- **Decompose** 3.5.0 + **Essenty** 2.6.0 — navigation and lifecycle
- **MVIKotlin** 4.4.0 — predictable state management
- **Metro DI** 1.4.3 — compile-time dependency injection
- **Kotlinx Coroutines** 1.11.0, **Serialization JSON** 1.11.0, and **Datetime** 0.8.0
- **Multiplatform Settings** 1.3.0 — cross-platform persistence
- **Firebase GitLive** 3.0.0-alpha02 — Analytics and Crashlytics
- **Google Mobile Ads** 25.4.0 + **User Messaging Platform** 4.0.0
- **Haze** 1.7.3, **ConfettiKit** 0.9.0, and **AboutLibraries** 15.2.0
- **Turbine** 1.2.1, **Robolectric** 4.17, and **Compose UI testing**
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

See [Contributing a MiniApp](CONTRIBUTING_MINIAPP.md) for the end-to-end
workflow: scaffold, boundaries, verification, and review. Generated modules
stay discoverable but unshipped — only a maintainer edits the root `miniApps`
allowlist after review.

```bash
./gradlew createMiniApp -PminiAppId=game.snake -PminiAppName=Snake
./gradlew :game:snake:verifyMiniApp
```

Add `-PminiAppProfile=game` to the first command for the game-oriented
scaffold (immutable state, typed actions, pure engine seam, focused tests).
AI agents also follow the [AI contributor protocol](docs/miniapp/AI_CONTRIBUTOR_PROTOCOL.md);
agent-level architecture rules live in [AGENTS.md](AGENTS.md).

## Contributing

Contributions are welcome. See [Contributing a MiniApp](CONTRIBUTING_MINIAPP.md) —
new games ship only after review and allowlisting.

<a id="support-me"></a>

## Sponsor Funfolio

Funfolio is looking for sponsors who want to help fund independent development.
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
