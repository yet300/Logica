import tempfile
import unittest
from pathlib import Path

from tools.store_screenshots.app_store_listing import (
    APP_STORE_LOCALES,
    MetadataSummary,
    TITLE,
    app_store_locale,
    validate_metadata,
)


EXPECTED_APP_STORE_LOCALES = {
    "ar": "ar-SA", "bn": "bn-BD", "da": "da", "de": "de-DE",
    "el": "el", "en": "en-US", "es": "es-ES", "fi": "fi",
    "fr": "fr-FR", "he": "he", "hi": "hi", "hu": "hu",
    "id": "id", "it": "it", "ja": "ja", "ko": "ko",
    "nb": "no", "nl": "nl-NL", "pl": "pl", "pt": "pt-BR",
    "ro": "ro", "ru": "ru", "sv": "sv", "th": "th",
    "tr": "tr", "uk": "uk", "vi": "vi", "zh": "zh-Hant",
}

METADATA_FILES = {
    "name.txt": TITLE,
    "subtitle.txt": "A growing puzzle collection",
    "description.txt": "A thoughtful collection of puzzle games.",
    "keywords.txt": "puzzle,blocks,numbers,merge,logic,offline,casual",
    "promotional_text.txt": "Choose a puzzle and find your flow.",
    "support_url.txt": "https://github.com/yet300/BlockBlast#support-me",
    "privacy_url.txt": "https://github.com/yet300/block_blast/blob/main/privacy_policy.md",
}


def create_complete_metadata(root: Path) -> Path:
    for locale in EXPECTED_APP_STORE_LOCALES.values():
        locale_root = root / locale
        locale_root.mkdir(parents=True)
        for filename, value in METADATA_FILES.items():
            (locale_root / filename).write_text(value + "\n", encoding="utf-8")
    return root


class AppStoreLocaleContractTest(unittest.TestCase):
    def test_app_store_locale_map_is_exact(self):
        self.assertEqual(APP_STORE_LOCALES, EXPECTED_APP_STORE_LOCALES)
        self.assertEqual(len(APP_STORE_LOCALES), 28)

    def test_rejects_locales_unsupported_by_app_store(self):
        for locale in ("az", "be", "hy", "ka", "kk", "ky", "tg", "tk", "uz"):
            with self.subTest(locale=locale):
                with self.assertRaisesRegex(ValueError, f"Unsupported App Store locale: {locale}"):
                    app_store_locale(locale)


class AppStoreMetadataValidationTest(unittest.TestCase):
    def test_accepts_complete_metadata(self):
        with tempfile.TemporaryDirectory() as temp:
            summary = validate_metadata(create_complete_metadata(Path(temp)))
            self.assertEqual(summary, MetadataSummary(28, 28, 28, 28, 28, 28, 28, 28))

    def test_rejects_missing_and_extra_locales(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            for file in (root / "ar-SA").iterdir():
                file.unlink()
            (root / "ar-SA").rmdir()
            (root / "xx-XX").mkdir()
            with self.assertRaisesRegex(ValueError, "missing=.*ar-SA.*extra=.*xx-XX"):
                validate_metadata(root)

    def test_rejects_invalid_name_and_utf8(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "de-DE/name.txt").write_text("Logica", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Invalid name for de-DE"):
                validate_metadata(root)

        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "ja/description.txt").write_bytes(b"\xff")
            with self.assertRaisesRegex(ValueError, "Invalid UTF-8 in description.txt for ja"):
                validate_metadata(root)

    def test_rejects_missing_empty_and_over_limit_fields(self):
        cases = (
            ("subtitle.txt", "", "Invalid subtitle for en-US"),
            ("subtitle.txt", "x" * 31, "Invalid subtitle for en-US"),
            ("keywords.txt", "x" * 101, "Invalid keywords for en-US"),
            ("promotional_text.txt", "x" * 171, "Invalid promotional text for en-US"),
            ("description.txt", "x" * 4001, "Invalid description for en-US"),
        )
        for filename, value, message in cases:
            with self.subTest(filename=filename, length=len(value)), tempfile.TemporaryDirectory() as temp:
                root = create_complete_metadata(Path(temp))
                (root / "en-US" / filename).write_text(value, encoding="utf-8")
                with self.assertRaisesRegex(ValueError, message):
                    validate_metadata(root)

        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            (root / "en-US/subtitle.txt").unlink()
            with self.assertRaisesRegex(ValueError, "Missing subtitle.txt for en-US"):
                validate_metadata(root)

    def test_rejects_non_https_urls(self):
        for filename in ("support_url.txt", "privacy_url.txt"):
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as temp:
                root = create_complete_metadata(Path(temp))
                (root / "en-US" / filename).write_text("http://example.com", encoding="utf-8")
                with self.assertRaisesRegex(ValueError, f"Invalid .* URL for en-US"):
                    validate_metadata(root)


if __name__ == "__main__":
    unittest.main()
