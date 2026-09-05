#!/usr/bin/env python3
"""Validate and stage the complete localized Google Play listing."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


TITLE = "Logica — Block Puzzle"

PLAY_STORE_LOCALES: dict[str, str] = {
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


@dataclass(frozen=True)
class MetadataSummary:
    locales: int
    titles: int
    short_descriptions: int
    full_descriptions: int


def play_store_locale(locale: str) -> str:
    try:
        return PLAY_STORE_LOCALES[locale]
    except KeyError as error:
        raise ValueError(f"Unsupported locale: {locale}") from error


def _read_metadata_file(locale_root: Path, filename: str) -> str:
    path = locale_root / filename
    if not path.is_file():
        raise ValueError(f"Missing {filename} for {locale_root.name}")
    try:
        return path.read_text(encoding="utf-8", errors="strict").strip()
    except UnicodeDecodeError as error:
        raise ValueError(f"Invalid UTF-8 in {filename} for {locale_root.name}") from error


def validate_metadata(metadata_root: Path) -> MetadataSummary:
    expected = set(PLAY_STORE_LOCALES.values())
    actual = {path.name for path in metadata_root.iterdir() if path.is_dir()}
    if actual != expected:
        raise ValueError(
            f"Metadata locales differ: missing={sorted(expected - actual)}, "
            f"extra={sorted(actual - expected)}"
        )

    for play_locale in sorted(expected):
        locale_root = metadata_root / play_locale
        title = _read_metadata_file(locale_root, "title.txt")
        short = _read_metadata_file(locale_root, "short_description.txt")
        full = _read_metadata_file(locale_root, "full_description.txt")
        if title != TITLE or len(title) > 30:
            raise ValueError(f"Invalid title for {play_locale}")
        if not 1 <= len(short) <= 80:
            raise ValueError(f"Invalid short description for {play_locale}")
        if not 1 <= len(full) <= 4000:
            raise ValueError(f"Invalid full description for {play_locale}")

    count = len(expected)
    return MetadataSummary(count, count, count, count)
