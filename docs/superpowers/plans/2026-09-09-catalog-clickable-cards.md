# Catalog Clickable Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make each catalog card the sole launch action and remove the unused MiniApp cover-art contract and obsolete details-dialog expectations.

**Architecture:** `MiniAppManifest` remains a static identity contract containing only metadata rendered or used by the host. `MiniAppListItemCard` uses the Material 3 clickable `Card` overload and contains no nested action or trailing affordance. Catalog navigation continues to flow through the existing `CatalogComponent.onPlayClicked` callback.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, Decompose, Compose UI tests, Gradle.

---

### Task 1: Specify the single card action

**Files:**
- Modify: `feature/catalog/src/commonTest/kotlin/ge/yet/game/feature/catalog/ui/CatalogContentTest.kt`

- [x] **Step 1: Replace the Play-button test with a card-click test**

Assert that `catalog_card_game.blockblast` has a click action, clicking it forwards the exact `MiniAppId`, and `catalog_play_game.blockblast` does not exist. Remove the obsolete long-press/details-dialog test and the icon-to-button alignment test.

- [x] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :feature:catalog:allTests`

Expected: FAIL because the card currently has no click action and the Play button still exists.

### Task 2: Implement the clickable card

**Files:**
- Modify: `feature/catalog/src/commonMain/kotlin/ge/yet/game/feature/catalog/ui/MiniAppListItemCard.kt`
- Modify: `feature/catalog/src/commonMain/composeResources/values/strings.xml`

- [x] **Step 1: Use the clickable Material Card overload**

Pass `onClick = onPlay` to `Card`. Keep the `ListItem`, title and decorative icon, but delete `trailingContent`, the nested `Button`, Play resource imports, and the unused one-line overflow constraint if no longer needed by the final layout.

- [x] **Step 2: Remove the catalog-only Play string when unused**

Delete `<string name="play">Play</string>` from catalog resources if repository search confirms there are no other consumers.

- [x] **Step 3: Run the focused test and verify GREEN**

Run: `./gradlew :feature:catalog:allTests`

Expected: PASS.

### Task 3: Remove cover art from the MiniApp contract

**Files:**
- Modify: `miniapp/compose/src/commonMain/kotlin/ge/yet/game/miniapp/compose/MiniAppManifest.kt`
- Modify: every Kotlin constructor of `MiniAppManifest` in `game/`, `miniapp/`, `feature/`, and `build-logic/`
- Modify: contract/scaffold tests that assert the generated manifest shape

- [x] **Step 1: Add or update contract/scaffold expectations**

Update tests so a manifest is constructed without `cover` and generated MiniApp source is expected not to contain `cover = null`.

- [x] **Step 2: Run contract tests and verify RED**

Run: `./gradlew :miniapp:compose:allTests`

Run: `./gradlew -p build-logic :convention:test --tests '*CreateMiniAppTaskTest' --tests '*MiniAppScaffoldRendererTest'`

Expected: at least one focused expectation fails until the contract and renderer are changed; if a requested test pattern is absent, list the matching convention tests and run the exact available scaffold test class.

- [x] **Step 3: Remove the property and all constructor arguments**

Delete `cover: DrawableResource?` from `MiniAppManifest`, remove `cover = null` from production plugins, previews, fixtures and generated scaffold templates, and clean unused imports.

- [x] **Step 4: Run contract tests and verify GREEN**

Run: `./gradlew :miniapp:compose:allTests :miniapp:metro:allTests :miniapp:testkit:allTests`

Run the exact matching build-logic scaffold test class discovered in Step 2.

Expected: PASS.

### Task 4: Align architecture documentation and verify scope

**Files:**
- Modify: `AGENTS.md`

- [x] **Step 1: Update the catalog contract description**

Replace the documented trailing Play action with a single full-card launch action, and state that covers, details dialogs and per-game catalog color metadata are outside the public MiniApp contract.

- [x] **Step 2: Search for stale implementation references**

Run: `rg 'cover\s*=|val cover|catalog_play_|catalog_details_dialog|menu_details|trailing Play' game miniapp feature build-logic AGENTS.md`

Expected: no active source, test, scaffold or architecture-guide matches.

- [x] **Step 3: Run final focused verification**

Run: `./gradlew :feature:catalog:allTests :miniapp:compose:allTests :miniapp:metro:allTests :miniapp:testkit:allTests :game:blockblast:compileAndroidMain :game:twentyfortyeight:compileAndroidMain :game:fruitmerge:compileAndroidMain`

Run: `./gradlew -p build-logic :convention:test --tests '*CreateMiniAppTaskTest'`

Expected: BUILD SUCCESSFUL for both commands.
