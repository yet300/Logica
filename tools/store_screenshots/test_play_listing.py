import tempfile
import unittest
import re
from contextlib import redirect_stderr, redirect_stdout
from io import StringIO
from pathlib import Path

from tools.store_screenshots.play_listing import (
    PLAY_STORE_LOCALES,
    TITLE,
    AssetSummary,
    MetadataSummary,
    main,
    play_store_locale,
    stage_all_play_assets,
    validate_metadata,
)


EXPECTED_PLAY_LOCALES = {
    "ar": "ar", "az": "az-AZ", "be": "be", "bn": "bn-BD",
    "da": "da-DK", "de": "de-DE", "el": "el-GR", "en": "en-US",
    "es": "es-ES", "fi": "fi-FI", "fr": "fr-FR", "he": "iw-IL",
    "hi": "hi-IN", "hu": "hu-HU", "hy": "hy-AM", "id": "id",
    "it": "it-IT", "ja": "ja-JP", "ka": "ka-GE", "kk": "kk",
    "ko": "ko-KR", "ky": "ky-KG", "nb": "no-NO", "nl": "nl-NL",
    "pl": "pl-PL", "pt": "pt-BR", "ro": "ro", "ru": "ru-RU",
    "sv": "sv-SE", "th": "th", "tr": "tr-TR", "uk": "uk",
    "vi": "vi",
    "zh": "zh-TW",
}


def create_complete_metadata(root: Path) -> Path:
    for play_locale in EXPECTED_PLAY_LOCALES.values():
        locale_root = root / play_locale
        locale_root.mkdir(parents=True)
        (locale_root / "title.txt").write_text(TITLE + "\n", encoding="utf-8")
        (locale_root / "short_description.txt").write_text(
            f"Puzzle collection for {play_locale}.\n", encoding="utf-8"
        )
        (locale_root / "full_description.txt").write_text(
            f"A growing collection of puzzle games for {play_locale}.\n",
            encoding="utf-8",
        )
    return root


SCREENSHOT_FILENAMES = [
    "01-hero.png",
    "02-device-bottom.png",
    "03-device-top.png",
    "04-hero.png",
    "05-device-bottom.png",
    "06-three-devices.png",
    "07-hero.png",
]


def create_complete_artifacts(root: Path) -> Path:
    for locale in EXPECTED_PLAY_LOCALES:
        artifact = root / f"store-screenshots-{locale}"
        phone_root = artifact / "android/1080x1920"
        phone_root.mkdir(parents=True)
        for filename in SCREENSHOT_FILENAMES:
            (phone_root / filename).write_bytes(f"{locale}:{filename}".encode())
        tablet_root = artifact / "ipad/2064x2752"
        tablet_root.mkdir(parents=True)
        for filename in SCREENSHOT_FILENAMES:
            (tablet_root / filename).write_bytes(f"{locale}:ipad:{filename}".encode())
        feature = artifact / "feature-graphic/1024x500/01-feature-graphic.png"
        feature.parent.mkdir(parents=True)
        feature.write_bytes(f"{locale}:feature".encode())
    return root


class PlayLocaleContractTest(unittest.TestCase):
    def test_play_locale_map_is_exact(self):
        self.assertEqual(PLAY_STORE_LOCALES, EXPECTED_PLAY_LOCALES)
        self.assertEqual(len(PLAY_STORE_LOCALES), 34)

    def test_maps_supported_locale_and_rejects_unknown_locale(self):
        self.assertEqual(play_store_locale("ka"), "ka-GE")
        self.assertEqual(play_store_locale("zh"), "zh-TW")
        with self.assertRaisesRegex(ValueError, "Unsupported locale: xx"):
            play_store_locale("xx")


class PlayMetadataValidationTest(unittest.TestCase):
    def test_accepts_complete_metadata(self):
        with tempfile.TemporaryDirectory() as temp:
            summary = validate_metadata(create_complete_metadata(Path(temp)))
            self.assertEqual(summary, MetadataSummary(34, 34, 34, 34))

    def test_rejects_missing_locale(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            for file in (root / "ka-GE").iterdir():
                file.unlink()
            (root / "ka-GE").rmdir()
            with self.assertRaisesRegex(ValueError, "missing=.*ka-GE"):
                validate_metadata(root)


class PlayMetadataCliTest(unittest.TestCase):
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
                ])
            self.assertEqual(result, 0)
            self.assertEqual(
                output.getvalue(),
                "Staged 34 Play Store locales: 238 phone screenshots, 476 tablet screenshots, "
                "34 feature graphics.\n",
            )

    def test_removed_commands_are_not_accepted(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            for command in ("report", "validate-metadata"):
                with self.subTest(command=command), redirect_stderr(StringIO()):
                    with self.assertRaises(SystemExit):
                        main([command, "--metadata-root", str(root)])

    def test_rejects_extra_locale(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "xx-XX").mkdir()
            with self.assertRaisesRegex(ValueError, "extra=.*xx-XX"):
                validate_metadata(root)

    def test_rejects_title_translation(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "ka-GE/title.txt").write_text("ლოგიკა\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Invalid title for ka-GE"):
                validate_metadata(root)

    def test_rejects_short_description_over_limit(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "de-DE/short_description.txt").write_text("x" * 81, encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "short description for de-DE"):
                validate_metadata(root)

    def test_rejects_empty_and_over_limit_full_description(self):
        for value in ("", "x" * 4001):
            with self.subTest(length=len(value)), tempfile.TemporaryDirectory() as temp:
                root = create_complete_metadata(Path(temp))
                (root / "fr-FR/full_description.txt").write_text(value, encoding="utf-8")
                with self.assertRaisesRegex(ValueError, "full description for fr-FR"):
                    validate_metadata(root)

    def test_rejects_missing_file_and_invalid_utf8(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "ja-JP/short_description.txt").unlink()
            with self.assertRaisesRegex(ValueError, "Missing short_description.txt for ja-JP"):
                validate_metadata(root)

        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "ja-JP/full_description.txt").write_bytes(b"\xff")
            with self.assertRaisesRegex(ValueError, "Invalid UTF-8 in full_description.txt for ja-JP"):
                validate_metadata(root)


class PlayAssetStagingTest(unittest.TestCase):
    def test_stages_complete_artifact_set_and_preserves_changelog(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            metadata = create_complete_metadata(root / "metadata")
            changelog = metadata / "en-US/changelogs/14.txt"
            changelog.parent.mkdir()
            changelog.write_text("Existing release notes.\n", encoding="utf-8")
            artifacts = create_complete_artifacts(root / "artifacts")

            summary = stage_all_play_assets(artifacts, metadata)

            self.assertEqual(summary, AssetSummary(34, 238, 476, 34))
            self.assertEqual(changelog.read_text(encoding="utf-8"), "Existing release notes.\n")
            self.assertEqual(
                len(list((metadata / "ka-GE/images/phoneScreenshots").glob("*.png"))),
                7,
            )
            for directory_name in ("sevenInchScreenshots", "tenInchScreenshots"):
                self.assertEqual(
                    len(list((metadata / f"ka-GE/images/{directory_name}").glob("*.png"))),
                    7,
                )
            self.assertEqual(
                (metadata / "ka-GE/images/featureGraphic.png").read_bytes(),
                b"ka:feature",
            )

    def test_rejects_missing_locale_before_touching_destination(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            metadata = create_complete_metadata(root / "metadata")
            marker = metadata / "en-US/images/marker.txt"
            marker.parent.mkdir()
            marker.write_text("keep", encoding="utf-8")
            artifacts = create_complete_artifacts(root / "artifacts")
            missing = artifacts / "store-screenshots-ka"
            for file in sorted(missing.rglob("*"), reverse=True):
                if file.is_file():
                    file.unlink()
                else:
                    file.rmdir()
            missing.rmdir()

            with self.assertRaisesRegex(ValueError, "Missing artifact for ka"):
                stage_all_play_assets(artifacts, metadata)
            self.assertEqual(marker.read_text(encoding="utf-8"), "keep")

    def test_rejects_incomplete_images_before_touching_destination(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            metadata = create_complete_metadata(root / "metadata")
            marker = metadata / "en-US/images/marker.txt"
            marker.parent.mkdir()
            marker.write_text("keep", encoding="utf-8")
            artifacts = create_complete_artifacts(root / "artifacts")
            (artifacts / "store-screenshots-de/android/1080x1920/07-hero.png").unlink()

            with self.assertRaisesRegex(ValueError, "Expected 7 phone screenshots for de"):
                stage_all_play_assets(artifacts, metadata)
            self.assertEqual(marker.read_text(encoding="utf-8"), "keep")

    def test_rejects_missing_feature_graphic_before_touching_destination(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            metadata = create_complete_metadata(root / "metadata")
            marker = metadata / "en-US/images/marker.txt"
            marker.parent.mkdir()
            marker.write_text("keep", encoding="utf-8")
            artifacts = create_complete_artifacts(root / "artifacts")
            feature = artifacts / "store-screenshots-fr/feature-graphic/1024x500/01-feature-graphic.png"
            feature.unlink()

            with self.assertRaisesRegex(ValueError, "Missing feature graphic for fr"):
                stage_all_play_assets(artifacts, metadata)
            self.assertEqual(marker.read_text(encoding="utf-8"), "keep")

    def test_rejects_wrong_screenshot_filename(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            metadata = create_complete_metadata(root / "metadata")
            artifacts = create_complete_artifacts(root / "artifacts")
            phone_root = artifacts / "store-screenshots-en/android/1080x1920"
            (phone_root / "07-hero.png").rename(phone_root / "99-wrong.png")

            with self.assertRaisesRegex(ValueError, "Unexpected phone screenshot names for en"):
                stage_all_play_assets(artifacts, metadata)


class RepositoryIgnoreContractTest(unittest.TestCase):
    def test_generated_fastlane_images_are_ignored_without_ignoring_metadata(self):
        gitignore = Path(".gitignore").read_text(encoding="utf-8").splitlines()
        self.assertIn("fastlane/metadata/android/*/images/", gitignore)
        self.assertNotIn("fastlane/metadata/android/", gitignore)


class PlayListingFastfileTest(unittest.TestCase):
    def test_release_lane_uploads_localized_changelogs(self):
        fastfile = Path("fastlane/Fastfile").read_text(encoding="utf-8")
        lanes = re.findall(r"lane :release do(?P<body>.*?)\n  end", fastfile, re.DOTALL)
        self.assertGreaterEqual(len(lanes), 2)
        self.assertIn("skip_upload_changelogs: false", lanes[0])

    def test_draft_lane_uploads_listing_only_and_never_requests_review(self):
        fastfile = Path("fastlane/Fastfile").read_text(encoding="utf-8")
        match = re.search(
            r"lane :publish_store_listing_draft do(?P<body>.*?)\n  end",
            fastfile,
            re.DOTALL,
        )
        self.assertIsNotNone(match)
        body = match.group("body")
        for setting in (
            "skip_upload_apk: true",
            "skip_upload_aab: true",
            "skip_upload_metadata: false",
            "skip_upload_changelogs: true",
            "skip_upload_images: false",
            "skip_upload_screenshots: false",
            "sync_image_upload: true",
            "changes_not_sent_for_review: true",
            "rescue_changes_not_sent_for_review: true",
        ):
            self.assertIn(setting, body)
        self.assertIn('metadata_path: "fastlane/metadata/android"', body)
        self.assertIn('json_key_data: play_store_json_key', body)
        self.assertIn("PLAY_STORE_JSON_KEY env is not set", body)
        self.assertNotRegex(body, r"\b(?:aab|apk|track):")


if __name__ == "__main__":
    unittest.main()
