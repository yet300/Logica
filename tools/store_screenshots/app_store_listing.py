#!/usr/bin/env python3
"""Validate and stage the complete localized App Store listing."""

from __future__ import annotations


TITLE = "Logica — Block Puzzle"

APP_STORE_LOCALES: dict[str, str] = {
    "ar": "ar-SA", "bn": "bn-BD", "da": "da", "de": "de-DE",
    "el": "el", "en": "en-US", "es": "es-ES", "fi": "fi",
    "fr": "fr-FR", "he": "he", "hi": "hi", "hu": "hu",
    "id": "id", "it": "it", "ja": "ja", "ko": "ko",
    "nb": "no", "nl": "nl-NL", "pl": "pl", "pt": "pt-BR",
    "ro": "ro", "ru": "ru", "sv": "sv", "th": "th",
    "tr": "tr", "uk": "uk", "vi": "vi", "zh": "zh-Hant",
}


def app_store_locale(locale: str) -> str:
    try:
        return APP_STORE_LOCALES[locale]
    except KeyError as error:
        raise ValueError(f"Unsupported App Store locale: {locale}") from error
