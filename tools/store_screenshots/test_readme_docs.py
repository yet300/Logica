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
        for wallet in (
            "**ton**",
            "**btc**",
            "**eth**",
            "**usdt(erc20)**",
            "**bnb**",
            "**usdt(trc20)**",
        ):
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
        readme = (
            ROOT / "store-assets/screenshot-editor/README.md"
        ).read_text(encoding="utf-8")

        self.assertIn("../../fastlane/README.md", readme)
        self.assertNotIn("IOS_DIST_CERT_P12", readme)
        self.assertNotIn("stage_store_assets", readme)

    def test_only_retained_store_tool_entrypoints_exist(self):
        tools = ROOT / "tools/store_screenshots"
        self.assertFalse((tools / "stage_fastlane.py").exists())
        self.assertFalse((tools / "test_stage_fastlane.py").exists())

        fastfile = (ROOT / "fastlane/Fastfile").read_text(encoding="utf-8")
        self.assertNotIn("stage_store_assets", fastfile)
        self.assertNotIn("stage_fastlane.py", fastfile)


if __name__ == "__main__":
    unittest.main()
