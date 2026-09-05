import tempfile
import unittest
from pathlib import Path

from tools.store_screenshots.stage_fastlane import (
    app_store_locale,
    stage_locale,
)
from tools.store_screenshots.play_listing import play_store_locale


LAYOUTS = [
    "01-hero.png",
    "02-device-bottom.png",
    "03-device-top.png",
    "04-hero.png",
    "05-device-bottom.png",
    "06-three-devices.png",
    "07-hero.png",
]


def create_complete_export(root: Path, locale: str) -> Path:
    locale_root = root / locale
    for device, sizes in {
        "iphone": ["1320x2868", "1284x2778", "1206x2622", "1125x2436"],
        "ipad": ["2064x2752", "2048x2732"],
        "android": ["1080x1920"],
    }.items():
        for size in sizes:
            for filename in LAYOUTS:
                target = locale_root / device / size / filename
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(b"png")
    feature = locale_root / "feature-graphic" / "1024x500" / "01-feature-graphic.png"
    feature.parent.mkdir(parents=True, exist_ok=True)
    feature.write_bytes(b"png")
    return locale_root


def create_canonical_export(root: Path, locale: str) -> Path:
    locale_root = create_complete_export(root, locale)
    for device, sizes_to_remove in {
        "iphone": ["1284x2778", "1206x2622", "1125x2436"],
        "ipad": ["2048x2732"],
    }.items():
        for size in sizes_to_remove:
            for file in (locale_root / device / size).glob("*.png"):
                file.unlink()
            (locale_root / device / size).rmdir()
    return locale_root


class FastlaneLocaleMappingTest(unittest.TestCase):
    def test_maps_store_specific_locale_codes(self):
        self.assertEqual(app_store_locale("en"), "en-US")
        self.assertIsNone(app_store_locale("ka"))
        self.assertEqual(app_store_locale("zh"), "zh-Hant")
        self.assertEqual(play_store_locale("ka"), "ka-GE")
        self.assertEqual(play_store_locale("zh"), "zh-TW")


class StageLocaleTest(unittest.TestCase):
    def test_stages_complete_english_export(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            create_complete_export(root / "input", "en")
            summary = stage_locale(root / "input", root / "fastlane", "en")
            apple = list((root / "fastlane/screenshots/en-US").glob("*.png"))
            phone = list((root / "fastlane/metadata/android/en-US/images/phoneScreenshots").glob("*.png"))
            feature = root / "fastlane/metadata/android/en-US/images/featureGraphic.png"
            self.assertEqual(len(apple), 14)
            self.assertEqual(len(phone), 7)
            self.assertTrue(feature.is_file())
            self.assertEqual(summary, {"apple": 14, "androidPhone": 7, "featureGraphic": 1})

    def test_skips_unsupported_georgian_app_store_locale(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            create_complete_export(root / "input", "ka")
            stage_locale(root / "input", root / "fastlane", "ka")
            self.assertFalse((root / "fastlane/screenshots/ka").exists())
            self.assertTrue((root / "fastlane/metadata/android/ka-GE/images/featureGraphic.png").is_file())

    def test_stages_twenty_two_file_ci_artifact(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            create_canonical_export(root / "input", "en")
            summary = stage_locale(root / "input", root / "fastlane", "en")
            self.assertEqual(summary, {"apple": 14, "androidPhone": 7, "featureGraphic": 1})

    def test_rejects_incomplete_input_without_touching_fastlane(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            incomplete = root / "input/en/android/1080x1920/01-hero.png"
            incomplete.parent.mkdir(parents=True)
            incomplete.write_bytes(b"png")
            with self.assertRaisesRegex(ValueError, "Expected 22 canonical PNGs"):
                stage_locale(root / "input", root / "fastlane", "en")
            self.assertFalse((root / "fastlane").exists())


if __name__ == "__main__":
    unittest.main()
