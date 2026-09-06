# README and Store Tooling Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the root README product-focused, make `fastlane/README.md` the canonical store/release operations guide, preserve the donation wallets and App Store support link, and remove store-tool CLI surfaces that no retained automation uses.

**Architecture:** Documentation ownership follows the executable boundary: the root README describes the product, stack, contribution entry points, and sponsorship; the screenshot editor README describes authoring/export internals; the Fastlane README describes CI, staging, signing, and release operations. The Python tools retain only the three CI entry points (`verify_exports.py`, `play_listing.py stage`, and `app_store_listing.py stage`), while their reusable validation functions stay directly unit-tested.

**Tech Stack:** Markdown, Python 3 `unittest`, Fastlane/Ruby, GitHub Actions YAML, Next.js 15.0.3, Playwright 1.62.1, Kotlin Multiplatform 2.4.10, Compose Multiplatform 1.12.0.

---

### Task 1: Establish documentation ownership with an executable contract

**Files:**
- Create: `tools/store_screenshots/test_readme_docs.py`
- Modify: `README.md`
- Create: `fastlane/README.md`
- Modify: `store-assets/screenshot-editor/README.md`

- [ ] **Step 1: Write the failing documentation contract**

Create `tools/store_screenshots/test_readme_docs.py` with repository-root-relative checks:

```python
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class ReadmeDocumentationContractTest(unittest.TestCase):
    def test_root_readme_stays_product_focused(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")

        self.assertNotIn("### iPad", readme)
        self.assertNotIn("gh workflow run store-screenshots.yml", readme)
        self.assertNotIn("IOS_DIST_CERT_P12", readme)
        self.assertIn("[Store and release operations](fastlane/README.md)", readme)
        self.assertIn("Fruit Merge", readme)
        self.assertIn("Compose Multiplatform** 1.12.0", readme)
        self.assertIn("ParthJadhav/app-store-screenshots", readme)
        self.assertIn('<a id="support-me"></a>', readme)
        self.assertIn("## Sponsor Logica", readme)
        self.assertIn("ryaeh7282@gmail.com", readme)
        for wallet in ("**ton**", "**btc**", "**eth**", "**usdt(erc20)**", "**bnb**", "**usdt(trc20)**"):
            self.assertIn(wallet, readme)

    def test_fastlane_readme_owns_release_operations(self):
        readme = (ROOT / "fastlane/README.md").read_text(encoding="utf-8")

        for text in (
            "gh workflow run store-screenshots.yml -f locale=ka",
            "gh workflow run store-screenshots.yml -f locale=all",
            "PLAY_STORE_JSON_KEY",
            "APP_STORE_CONNECT_KEY_CONTENT",
            "IOS_DIST_CERT_P12",
            "IOS_PROVISIONING_PROFILE",
            "GOOGLE_SERVICE_INFO_PLIST",
            "vX.Y.Z",
            "14 days",
            "20–45 minutes",
            "submit_for_review: false",
            "automatic_release: false",
        ):
            self.assertIn(text, readme)

    def test_screenshot_editor_delegates_release_operations(self):
        readme = (ROOT / "store-assets/screenshot-editor/README.md").read_text(encoding="utf-8")

        self.assertIn("../../fastlane/README.md", readme)
        self.assertNotIn("IOS_DIST_CERT_P12", readme)
        self.assertNotIn("stage_store_assets", readme)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the contract and confirm it fails for the current layout**

Run:

```bash
python3 -m unittest tools.store_screenshots.test_readme_docs -v
```

Expected: FAIL because `fastlane/README.md` does not exist and the root/editor READMEs still contain operational material.

- [ ] **Step 3: Refocus the root README**

Edit `README.md` so it:

- changes the production catalog sentence to “Block Blast, 2048, and Fruit Merge”;
- changes the Compose badge and stack entry from `1.11.1` to `1.12.0`;
- retains only the seven-image Georgian iPhone gallery and removes the entire `### iPad` gallery;
- replaces the locale, workflow, secret, timing, and signed-iOS sections with one sentence linking `[Store and release operations](fastlane/README.md)`;
- updates the feature wording from a single Block Blast game to the growing puzzle catalog;
- lists the exact major library versions from `gradle/libs.versions.toml`: Material 3 `1.10.0-alpha05`, Material 3 Adaptive `1.3.0-beta02`, Decompose `3.5.0`, Essenty `2.5.0`, MVIKotlin `4.4.0`, Metro `1.4.2`, Coroutines/Serialization `1.11.0`, Datetime `0.8.0`, Multiplatform Settings `1.3.0`, GitLive Firebase `3.0.0-alpha02`, Google Mobile Ads `25.4.0`, UMP `4.0.0`, Haze `1.7.3`, ConfettiKit `0.9.0`, AboutLibraries `15.2.0`, Turbine `1.2.1`, and Robolectric `4.16.1`, plus Compose UI testing;
- adds a screenshot-tooling bullet linking [ParthJadhav/app-store-screenshots](https://github.com/ParthJadhav/app-store-screenshots) and naming Next.js `15.0.3`, Playwright `1.62.1`, and `html-to-image`;
- adds `game/fruitmerge/` and identifies `fastlane/` as metadata, screenshots, release automation, and operations documentation in the repository tree;
- replaces `## Support Me` with the following structure while preserving every existing wallet address unchanged:

```markdown
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
```

The explicit `support-me` anchor is required because every existing App Store `support_url.txt` points to `#support-me`.

- [ ] **Step 4: Create the canonical Fastlane operations guide**

Create `fastlane/README.md` with these sections and facts:

1. `# Store and Release Operations` and a warning that credentials never belong in Git.
2. `## Screenshot and listing workflow` with both exact manual commands:
   - `gh workflow run store-screenshots.yml -f locale=ka` — builds and validates one downloadable artifact only; it never publishes a partial listing.
   - `gh workflow run store-screenshots.yml -f locale=all` — from `main`, builds all 34 screenshot locales, then independently stages Google Play and App Store Connect listings.
3. Explain that Play stages 34 metadata packages, seven phone screenshots per locale, and 34 feature graphics with `changes_not_sent_for_review: true`; Apple stages 28 metadata packages plus seven iPhone and seven iPad screenshots per supported locale with `submit_for_review: false`, `automatic_release: false`, and no binary upload.
4. State that `MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig` must identify the editable App Store version before the all-locale run.
5. List required listing secrets by name only: `PLAY_STORE_JSON_KEY`, `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_KEY_CONTENT`.
6. Document the expected `20–45 minutes` cold end-to-end range, four-way render matrix, workflow summary timing, canonical artifact contents, and `14 days` retention.
7. `## Tag-driven releases` explaining that `vX.Y.Z` starts Android and iOS jobs independently; Android uploads a draft internal-track AAB and attaches the release APK to the GitHub Release; iOS builds an App Store-signed IPA, uploads to App Store Connect/TestFlight, retains IPA/dSYM for 14 days, and does not distribute to testers, attach to a store version, submit for review, or release automatically.
8. List release secret names only: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GOOGLE_SERVICES_JSON`, `PLAY_STORE_JSON_KEY`, `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_KEY_CONTENT`, `IOS_DIST_CERT_P12`, `IOS_DIST_CERT_PASSWORD`, `IOS_PROVISIONING_PROFILE`, `IOS_PROVISIONING_PROFILE_NAME`, and `GOOGLE_SERVICE_INFO_PLIST`.
9. Explain that the Apple Distribution certificate/profile must match `ge.yet3.blokblast.BlockBlast` and team `3KKQ642Q9H`, while the temporary CI keychain uses a per-run random password and is cleaned even on failure.

Verify every claim against `.github/workflows/store-screenshots.yml`, `.github/workflows/release.yml`, and `fastlane/Fastfile` while writing; do not document secret values or shell pipelines that could expose them.

- [ ] **Step 5: Trim the screenshot editor README to editor ownership**

Keep local setup, editor controls, screenshots, export behavior, measured render/storage characteristics, customization, and migration notes in `store-assets/screenshot-editor/README.md`. Remove duplicated publication commands, secret names, release-build behavior, and the obsolete `stage_store_assets` example. End the headless-export section with:

```markdown
For GitHub Actions generation, store-listing drafts, signing requirements,
artifact retention, and tag-driven releases, see the
[Fastlane operations guide](../../fastlane/README.md).
```

- [ ] **Step 6: Run the documentation contract**

Run:

```bash
python3 -m unittest tools.store_screenshots.test_readme_docs -v
```

Expected: PASS, three tests.

- [ ] **Step 7: Commit the documentation ownership change**

```bash
git add README.md fastlane/README.md store-assets/screenshot-editor/README.md tools/store_screenshots/test_readme_docs.py
git commit -m "docs: focus README on product and sponsorship"
```

### Task 2: Remove the unused one-locale Fastlane staging surface

**Files:**
- Modify: `tools/store_screenshots/test_readme_docs.py`
- Delete: `tools/store_screenshots/stage_fastlane.py`
- Delete: `tools/store_screenshots/test_stage_fastlane.py`
- Modify: `fastlane/Fastfile`

- [ ] **Step 1: Extend the tool-surface contract and observe failure**

Add this test to `ReadmeDocumentationContractTest`:

```python
    def test_only_retained_store_tool_entrypoints_exist(self):
        tools = ROOT / "tools/store_screenshots"
        self.assertFalse((tools / "stage_fastlane.py").exists())
        self.assertFalse((tools / "test_stage_fastlane.py").exists())

        fastfile = (ROOT / "fastlane/Fastfile").read_text(encoding="utf-8")
        self.assertNotIn("stage_store_assets", fastfile)
        self.assertNotIn("stage_fastlane.py", fastfile)
```

Run:

```bash
python3 -m unittest tools.store_screenshots.test_readme_docs.ReadmeDocumentationContractTest.test_only_retained_store_tool_entrypoints_exist -v
```

Expected: FAIL because both files and the lane still exist.

- [ ] **Step 2: Delete the orphaned implementation and test**

Delete `tools/store_screenshots/stage_fastlane.py` and `tools/store_screenshots/test_stage_fastlane.py`. Do not delete `verify_exports.py`, either store-listing staging script, or any reusable validation function.

- [ ] **Step 3: Delete the unused Fastlane lane**

Remove the complete `desc "Stage one generated screenshot locale without uploading it"` / `lane :stage_store_assets` block from the Android platform in `fastlane/Fastfile`. Leave `publish_store_listing_draft`, Android `release`, `publish_app_store_listing_draft`, and iOS `release` unchanged.

- [ ] **Step 4: Verify the reduced surface and Fastfile syntax**

Run:

```bash
python3 -m unittest tools.store_screenshots.test_readme_docs -v
bundle exec ruby -c fastlane/Fastfile
```

Expected: all tests PASS and Ruby prints `Syntax OK`.

- [ ] **Step 5: Commit the orphan removal**

```bash
git add fastlane/Fastfile tools/store_screenshots/test_readme_docs.py
git rm tools/store_screenshots/stage_fastlane.py tools/store_screenshots/test_stage_fastlane.py
git commit -m "chore: remove unused screenshot staging lane"
```

### Task 3: Reduce the Google Play CLI to its CI staging command

**Files:**
- Modify: `tools/store_screenshots/test_play_listing.py`
- Modify: `tools/store_screenshots/play_listing.py`

- [ ] **Step 1: Rewrite the CLI test around the retained interface**

Delete `test_validate_metadata_command_prints_summary` and `test_report_command_prints_character_counts` from `PlayMetadataCliTest`. Keep `test_stage_command_prints_complete_asset_summary`, direct `validate_metadata` tests, and staging tests. Add:

```python
    def test_removed_commands_are_not_accepted(self):
        for command in ("report", "validate-metadata"):
            with self.subTest(command=command), self.assertRaises(SystemExit):
                main([command])
```

Run:

```bash
python3 -m unittest tools.store_screenshots.test_play_listing.PlayMetadataCliTest -v
```

Expected: FAIL because both obsolete commands are still registered.

- [ ] **Step 2: Remove the obsolete Play CLI branches**

In `tools/store_screenshots/play_listing.py`:

- remove `_print_report`;
- remove the `validate-metadata` and `report` subparser definitions;
- keep only the required `stage` subparser with `--artifacts-root` and `--metadata-root`;
- simplify `main()` to stage, print `Staged {locales} Play Store locales: {phone_screenshots} phone screenshots, {feature_graphics} feature graphics.`, and return `0`;
- remove imports used only by the deleted reporting path;
- retain `validate_metadata()` because `stage_all_play_assets()` depends on it.

- [ ] **Step 3: Run Play listing tests**

```bash
python3 -m unittest tools.store_screenshots.test_play_listing -v
```

Expected: PASS.

- [ ] **Step 4: Commit the Play CLI reduction**

```bash
git add tools/store_screenshots/play_listing.py tools/store_screenshots/test_play_listing.py
git commit -m "refactor: keep only Play staging CLI"
```

### Task 4: Reduce the App Store CLI to its CI staging command

**Files:**
- Modify: `tools/store_screenshots/test_app_store_listing.py`
- Modify: `tools/store_screenshots/app_store_listing.py`

- [ ] **Step 1: Add tests for the retained and removed App Store commands**

Add `redirect_stdout`, `StringIO`, and `main` imports, then add:

```python
class AppStoreMetadataCliTest(unittest.TestCase):
    def test_stage_command_prints_complete_asset_summary(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            metadata = create_complete_metadata(root / "metadata")
            artifacts = create_complete_artifacts(root / "artifacts")
            output = StringIO()
            with redirect_stdout(output):
                result = main([
                    "stage",
                    "--artifacts-root", str(artifacts),
                    "--metadata-root", str(metadata),
                    "--screenshots-root", str(root / "screenshots"),
                ])
            self.assertEqual(result, 0)
            self.assertEqual(
                output.getvalue(),
                "Staged 28 App Store locales: 196 iPhone screenshots, "
                "196 iPad screenshots (392 total).\n",
            )

    def test_removed_commands_are_not_accepted(self):
        for command in ("report", "validate-metadata"):
            with self.subTest(command=command), self.assertRaises(SystemExit):
                main([command])
```

Run:

```bash
python3 -m unittest tools.store_screenshots.test_app_store_listing.AppStoreMetadataCliTest -v
```

Expected: stage passes and removed-command assertions fail because the old commands remain registered.

- [ ] **Step 2: Remove the obsolete App Store CLI branches**

In `tools/store_screenshots/app_store_listing.py`:

- remove `_print_report`;
- remove the `validate-metadata` and `report` subparser definitions;
- keep only `stage` with `--artifacts-root`, `--metadata-root`, and `--screenshots-root`;
- simplify `main()` to stage, print the existing locale/iPhone/iPad/total summary, and return `0`;
- remove imports used only by the report path;
- retain `validate_metadata()` because `stage_all_app_store_assets()` depends on it.

- [ ] **Step 3: Run App Store listing tests**

```bash
python3 -m unittest tools.store_screenshots.test_app_store_listing -v
```

Expected: PASS.

- [ ] **Step 4: Commit the App Store CLI reduction**

```bash
git add tools/store_screenshots/app_store_listing.py tools/store_screenshots/test_app_store_listing.py
git commit -m "refactor: keep only App Store staging CLI"
```

### Task 5: Verify the complete documentation and automation change

**Files:**
- Verify: `README.md`
- Verify: `fastlane/README.md`
- Verify: `store-assets/screenshot-editor/README.md`
- Verify: `fastlane/Fastfile`
- Verify: `tools/store_screenshots/`
- Verify: `.github/workflows/store-screenshots.yml`
- Verify: `.github/workflows/release.yml`

- [ ] **Step 1: Run the complete Python store-tool suite**

```bash
python3 -m unittest discover -s tools/store_screenshots -p 'test_*.py' -v
```

Expected: PASS with no test importing `stage_fastlane.py` and no CLI test invoking `report` or `validate-metadata` successfully.

- [ ] **Step 2: Run the screenshot-editor contract and build checks**

From `store-assets/screenshot-editor` run:

```bash
bun run test:export-contract
bun run verify:project
bun run verify:localizations
bun run verify:visual
bun run build
```

Expected: all commands exit `0`; Next.js production build completes.

- [ ] **Step 3: Validate Fastlane and workflow syntax**

From the repository root run:

```bash
bundle exec ruby -c fastlane/Fastfile
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/store-screenshots.yml"); YAML.load_file(".github/workflows/release.yml"); puts "workflow YAML OK"'
```

Expected: `Syntax OK` and `workflow YAML OK`.

- [ ] **Step 4: Prove every retained executable is actually referenced**

Run:

```bash
rg -n "verify_exports\.py|play_listing\.py.*stage|app_store_listing\.py.*stage" .github/workflows fastlane
rg -n "stage_fastlane\.py|stage_store_assets|play_listing\.py (report|validate-metadata)|app_store_listing\.py (report|validate-metadata)" .github fastlane README.md store-assets tools
```

Expected: the first command finds all three CI entry points in `store-screenshots.yml`; the second command returns no live documentation, automation, or tool references (historical `docs/superpowers/` records are intentionally outside this check).

- [ ] **Step 5: Inspect the final diff and whitespace**

```bash
git diff --check
git status --short
git diff --stat origin/codex/super-app-store-screenshots...HEAD
git diff origin/codex/super-app-store-screenshots...HEAD -- README.md fastlane/README.md store-assets/screenshot-editor/README.md fastlane/Fastfile tools/store_screenshots
```

Expected: no whitespace errors; only intended tracked files differ. The three existing untracked GitLive SwiftPM subpackage directories remain untouched and uncommitted.

- [ ] **Step 6: Request an independent review and resolve findings**

Review the final diff for documentation truthfulness, preserved support URLs/wallets, removed dead interfaces, CI compatibility, and accidental credential disclosure. If any finding requires a change, add a focused regression test first, implement the correction, and rerun the narrow and complete checks above.

- [ ] **Step 7: Commit any final verification fixes, push, and update PR #13**

If verification produced tracked fixes:

```bash
git add README.md fastlane/README.md store-assets/screenshot-editor/README.md fastlane/Fastfile tools/store_screenshots
git commit -m "test: verify store documentation contracts"
```

Then push the existing branch and confirm PR #13 points to the new HEAD:

```bash
git push origin codex/super-app-store-screenshots
gh pr view 13 --json url,headRefName,headRefOid,mergeable,statusCheckRollup
```

Expected: PR URL is `https://github.com/yet300/BlockBlast/pull/13`, `headRefName` is `codex/super-app-store-screenshots`, and `headRefOid` equals local `git rev-parse HEAD`.
