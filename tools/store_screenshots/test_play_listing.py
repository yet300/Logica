import tempfile
import unittest
from pathlib import Path

from tools.store_screenshots.play_listing import (
    PLAY_STORE_LOCALES,
    TITLE,
    MetadataSummary,
    play_store_locale,
    validate_metadata,
)


EXPECTED_PLAY_LOCALES = {
    "ar": "ar", "az": "az-AZ", "be": "be-BY", "bn": "bn-BD",
    "da": "da-DK", "de": "de-DE", "el": "el-GR", "en": "en-US",
    "es": "es-ES", "fi": "fi-FI", "fr": "fr-FR", "he": "he-IL",
    "hi": "hi-IN", "hu": "hu-HU", "hy": "hy-AM", "id": "id-ID",
    "it": "it-IT", "ja": "ja-JP", "ka": "ka-GE", "kk": "kk-KZ",
    "ko": "ko-KR", "ky": "ky-KG", "nb": "nb-NO", "nl": "nl-NL",
    "pl": "pl-PL", "pt": "pt-BR", "ro": "ro-RO", "ru": "ru-RU",
    "sv": "sv-SE", "tg": "tg-TJ", "th": "th-TH", "tk": "tk-TM",
    "tr": "tr-TR", "uk": "uk-UA", "uz": "uz-UZ", "vi": "vi-VN",
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


class PlayLocaleContractTest(unittest.TestCase):
    def test_play_locale_map_is_exact(self):
        self.assertEqual(PLAY_STORE_LOCALES, EXPECTED_PLAY_LOCALES)
        self.assertEqual(len(PLAY_STORE_LOCALES), 37)

    def test_maps_supported_locale_and_rejects_unknown_locale(self):
        self.assertEqual(play_store_locale("ka"), "ka-GE")
        self.assertEqual(play_store_locale("zh"), "zh-TW")
        with self.assertRaisesRegex(ValueError, "Unsupported locale: xx"):
            play_store_locale("xx")


class PlayMetadataValidationTest(unittest.TestCase):
    def test_accepts_complete_metadata(self):
        with tempfile.TemporaryDirectory() as temp:
            summary = validate_metadata(create_complete_metadata(Path(temp)))
            self.assertEqual(summary, MetadataSummary(37, 37, 37, 37))

    def test_rejects_missing_locale(self):
        with tempfile.TemporaryDirectory() as temp:
            root = create_complete_metadata(Path(temp))
            for file in (root / "ka-GE").iterdir():
                file.unlink()
            (root / "ka-GE").rmdir()
            with self.assertRaisesRegex(ValueError, "missing=.*ka-GE"):
                validate_metadata(root)

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


if __name__ == "__main__":
    unittest.main()
