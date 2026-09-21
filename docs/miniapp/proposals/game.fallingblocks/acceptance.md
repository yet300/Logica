# Falling Blocks Acceptance Evidence

**Shipping status:** NOT ALLOWLISTED  
**Current phase:** UX-polish implementation and automated acceptance complete;
live-device experiential review remains blocked by the non-shipping gate and
the absence of an attached test device or booted simulator.

Statuses are `passed`, `failed`, `blocked-by-environment`, and `not-run`.

| Criterion | Evidence | Status | Remaining work |
|---|---|---|---|
| Module boundary | `validateMiniAppDependencies`; `verifyMiniApp`; isolated Metro registry and graph tests | passed | None |
| Pure deterministic engine | Board, movement, line-clear, scoring, lock, terminal and generated property tests | passed | None |
| Seven-bag and RNG restore | `SevenBagTest`, `RandomStateTest`, snapshot round-trip and uninterrupted-continuation tests | passed | None |
| SRS rotation | Clockwise JLSTZ/I/O transition and wall/floor/stack kick coverage in `SrsRotationTest` | passed | None |
| No Hold | No Hold model/action/resource; Compose semantics assert no Hold control | passed | None |
| Gesture-only play | Density-independent tap, drag, slow drop and fling classification tests | passed | Physical-device gesture feel remains experiential |
| Lock delay | 500 ms boundary and 15-reset cap in `LockDelayTest` | passed | None |
| Lines and scoring | Single through four-line, T-spin, combo, back-to-back, perfect-clear and drop scoring tests | passed | None |
| Top-out | Spawn obstruction and hidden-row lock tests; result is deduplicated by run ID | passed | None |
| One endless mode | Manifest and UI expose direct play only; no mode or difficulty state/resource | passed | None |
| Mandatory tutorial | Four legal-action steps, fixed practice seed, frozen gravity, retrying completion persistence, no Skip | passed | Live gesture pacing review |
| Exact resume | Full snapshot equality plus graph recreation test after visibility checkpoint | passed | None |
| Corrupt snapshot recovery | Version, board, active piece, bag, counters and timing validation tests | passed | None |
| Game-over presentation | Separate retained `ContentOnly` destination, unchanged terminal board with no blur/scrim/dismiss path, adaptive one/two-pane layout and 48 dp primary action | passed | Live compact/wide capture |
| Advertisement continuation | Visibility-aware five-second offer, one revive, bottom-four-row recovery, failure recovery and stale callback tests | passed | Live SDK presentation requires allowlisting/test host |
| New game | Clean incremented run and stale continuation rejection tests | passed | None |
| Banner eligibility | Plugin contract asserts `wantsBanner == true`; module contains no banner renderer | passed | Native creative layout remains host-owned |
| Centered adaptive field | Compose coordinate assertion plus exact `320×568`, `360×640`, `400×800`, `800×400` and `1200×800` geometry matrix; one external Next preview and no in-field metrics | passed | Device screenshots unavailable |
| Theme-only colors | Tetrominoes map only to `MaterialTheme.colorScheme` roles; light/dark tests; no fixed production color literals | passed | Visual contrast review on devices |
| CRT effect | Field-clipped static scanlines/glow; no jitter; reduced-motion policy retains static CRT treatment | passed | Aesthetic inspection unavailable |
| Board event effects | Monotonic typed events; deterministic normalized geometry; one cancellable/pausable board controller; 90+140 ms hard-drop treatment; exact-row flash/shock/captured cells; bounded particles | passed | Live hard-drop and one/four-line clear capture unavailable |
| Motion | Exact normal/reduced-motion duration assertions; reduced mode removes trails, channel displacement, particles and collapse translation | blocked-by-environment | Record move, rotation, drop, clear and result motion on an allowlisted test host |
| Accessibility | English board/result/tutorial semantics, no color-only piece identity, 48 dp result action | passed | Manual screen-reader and switch-control pass unavailable |
| Shared lock SFX | `WoodenPlacementThock` preset plus Block Blast and preset regression suites | passed | None |
| Original music and SFX | Original 126 BPM program; all 13 SFX deterministic, audible, finite, low-DC and below 0.98 peak; bounded declaration budget | passed | Human listening review unavailable |
| Audio experience | Routing, intensity bands, rejection/no-retry and deterministic stress assertions | blocked-by-environment | Listen to base/high loops, overlaps, mute, ad suppression and teardown on Android/iOS |
| Performance | Bounded three-track/13-SFX declaration and no storage/DSP work in Compose callbacks | blocked-by-environment | Android profiler and iOS live producer diagnostics were not available |
| Android verification | Falling Blocks Android compile and shared `composeApp` Android compile | passed | No Android host/device test was attached; repository Android host tests are disabled |
| iOS verification | iOS Simulator compile and common tests, including Compose UI tests | passed | No live simulator app session or physical iOS audio inspection |
| Repository hygiene | `git diff --check`; scoped source only; unrelated generated Firebase SwiftPM directories untouched | passed | Reconfirm after final commit |
| Shipping gate | No `settings.gradle.kts` diff; bundle report contains Block Blast, 2048 and Fruit Merge only | passed | Separate maintainer authorization is required to ship |

## Automated artifacts

- Falling Blocks test report: `game/fallingblocks/build/reports/tests/allTests/index.html`
- iOS Simulator test report: `game/fallingblocks/build/reports/tests/iosSimulatorArm64Test/index.html`
- Audio render assertions: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/audio/FallingBlocksAudioRenderTest.kt`
- Adaptive/theme assertions: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksThemeIntegrationTest.kt`
- Lifecycle/restore assertions: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksLifecycleIntegrationTest.kt`

The test renderer validated base/high music and every SFX in memory. It does not
export WAV files, and this task had no audible playback surface. No listening
claim or visual screenshot claim is made from compilation or pixel assertions.

## Verification run — 2026-09-20

Each required command exited `0`:

| Command | Result |
|---|---|
| `rtk ./gradlew :game:fallingblocks:allTests` | passed; iOS Simulator common and Compose UI suite |
| `rtk ./gradlew :game:fallingblocks:validateMiniAppDependencies` | passed |
| `rtk ./gradlew :game:fallingblocks:compileAndroidMain` | passed |
| `rtk ./gradlew :game:fallingblocks:compileKotlinIosSimulatorArm64` | passed |
| `rtk ./gradlew :game:fallingblocks:verifyMiniApp` | passed |
| `rtk ./gradlew :game:blockblast:allTests` | passed; shared wooden placement regression retained |
| `rtk ./gradlew :miniapp:audio-presets:allTests` | passed |
| `rtk ./gradlew :miniapp:audio-presets:compileAndroidMain` | passed |
| `rtk ./gradlew :miniapp:audio-presets:compileKotlinIosSimulatorArm64` | passed |
| `rtk ./gradlew :composeApp:compileAndroidMain` | passed |
| `rtk git diff --check` | passed |

## UX-polish verification run — 2026-09-21

Each automated command exited `0`:

| Command | Result |
|---|---|
| `rtk ./gradlew :game:fallingblocks:allTests` | passed; 115 common/iOS Simulator tests, including lifecycle, exact layout/theme matrix, ghost contrast and visual-event effects |
| `rtk ./gradlew :game:fallingblocks:validateMiniAppDependencies :game:fallingblocks:compileAndroidMain :game:fallingblocks:compileKotlinIosSimulatorArm64 :game:fallingblocks:verifyMiniApp` | passed |
| `rtk ./gradlew :game:blockblast:allTests :core:uikit:allTests :composeApp:compileAndroidMain` | passed |
| `rtk ./gradlew :miniapp:bundle:dependencies --configuration commonMainApi` | passed; Falling Blocks absent; only Metro, Block Blast, 2048 and Fruit Merge are bundled |
| `rtk git diff --check` | passed |
| `rtk git diff --quiet -- settings.gradle.kts` | passed; no allowlist change |

The regression suite additionally proves that exact checkpoint restore starts
without a stale visual event, inactive visibility pauses the presentation state,
destroyed Result approval cannot mutate a successor, revive returns to the
retained Playing child, New Game destroys the old child, the banner opt-in is
still true, and frame modes remain `Standard` for Playing and `ContentOnly` for
Result.

Additional shipping-boundary evidence:

- `rtk ./gradlew projects` lists `:game:fallingblocks`, proving discovery.
- `rtk ./gradlew :miniapp:bundle:dependencies --configuration commonMainApi`
  lists only `metro`, `blockblast`, `twentyfortyeight`, and `fruitmerge`; it does
  not contain Falling Blocks.
- `settings.gradle.kts` has no working-tree diff.

## Experiential limitations

No Android platform-tools/`adb`, unlocked Android device, Android emulator
session, booted iOS Simulator, or physical iOS device was available for this
non-allowlisted MiniApp. `xcrun simctl list devices booted` returned no devices.
Therefore
the following remain explicitly `blocked-by-environment`, not passed:

- device screenshots for compact portrait, wide, tablet and compact-height;
- live motion recordings and reduced-motion OS-setting inspection;
- TalkBack/VoiceOver focus-order review;
- listening to music/SFX, repeated overlaps, mute, background/resume, fullscreen
  ad suppression and audio teardown;
- Android frame/audio profiling and iOS realtime producer diagnostics.

Falling Blocks remains discoverable for review but absent from the production
bundle. Shipping requires a separate, explicit change to the authoritative
`miniApps` allowlist.
