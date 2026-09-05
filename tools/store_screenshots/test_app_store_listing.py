import unittest

from tools.store_screenshots.app_store_listing import (
    APP_STORE_LOCALES,
    app_store_locale,
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


class AppStoreLocaleContractTest(unittest.TestCase):
    def test_app_store_locale_map_is_exact(self):
        self.assertEqual(APP_STORE_LOCALES, EXPECTED_APP_STORE_LOCALES)
        self.assertEqual(len(APP_STORE_LOCALES), 28)

    def test_rejects_locales_unsupported_by_app_store(self):
        for locale in ("az", "be", "hy", "ka", "kk", "ky", "tg", "tk", "uz"):
            with self.subTest(locale=locale):
                with self.assertRaisesRegex(ValueError, f"Unsupported App Store locale: {locale}"):
                    app_store_locale(locale)


if __name__ == "__main__":
    unittest.main()
