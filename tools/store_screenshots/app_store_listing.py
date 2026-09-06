#!/usr/bin/env python3
"""Validate and stage the complete localized App Store listing."""

from __future__ import annotations

import argparse
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence
from urllib.parse import urlparse


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

METADATA_FILENAMES = (
    "name.txt",
    "subtitle.txt",
    "description.txt",
    "keywords.txt",
    "promotional_text.txt",
    "support_url.txt",
    "privacy_url.txt",
)


@dataclass(frozen=True)
class MetadataSummary:
    locales: int
    names: int
    subtitles: int
    descriptions: int
    keywords: int
    promotional_texts: int
    support_urls: int
    privacy_urls: int


@dataclass(frozen=True)
class AssetSummary:
    locales: int
    iphone_screenshots: int
    ipad_screenshots: int
    screenshots: int


SCREENSHOT_FILENAMES = (
    "01-hero.png",
    "02-device-bottom.png",
    "03-device-top.png",
    "04-hero.png",
    "05-device-bottom.png",
    "06-three-devices.png",
    "07-hero.png",
)


def app_store_locale(locale: str) -> str:
    try:
        return APP_STORE_LOCALES[locale]
    except KeyError as error:
        raise ValueError(f"Unsupported App Store locale: {locale}") from error


def _read_metadata_file(locale_root: Path, filename: str) -> str:
    path = locale_root / filename
    if not path.is_file():
        raise ValueError(f"Missing {filename} for {locale_root.name}")
    try:
        return path.read_text(encoding="utf-8", errors="strict").strip()
    except UnicodeDecodeError as error:
        raise ValueError(f"Invalid UTF-8 in {filename} for {locale_root.name}") from error


def _is_https_url(value: str) -> bool:
    parsed = urlparse(value)
    return parsed.scheme == "https" and bool(parsed.netloc)


def validate_metadata(metadata_root: Path) -> MetadataSummary:
    expected = set(APP_STORE_LOCALES.values())
    actual = {path.name for path in metadata_root.iterdir() if path.is_dir()}
    if actual != expected:
        raise ValueError(
            f"Metadata locales differ: missing={sorted(expected - actual)}, "
            f"extra={sorted(actual - expected)}"
        )

    for locale in sorted(expected):
        values = {
            filename: _read_metadata_file(metadata_root / locale, filename)
            for filename in METADATA_FILENAMES
        }
        if values["name.txt"] != TITLE or not 2 <= len(values["name.txt"]) <= 30:
            raise ValueError(f"Invalid name for {locale}")
        if not 1 <= len(values["subtitle.txt"]) <= 30:
            raise ValueError(f"Invalid subtitle for {locale}")
        if not 1 <= len(values["description.txt"]) <= 4000:
            raise ValueError(f"Invalid description for {locale}")
        if not 1 <= len(values["keywords.txt"]) <= 100:
            raise ValueError(f"Invalid keywords for {locale}")
        if not 1 <= len(values["promotional_text.txt"]) <= 170:
            raise ValueError(f"Invalid promotional text for {locale}")
        if not _is_https_url(values["support_url.txt"]):
            raise ValueError(f"Invalid support URL for {locale}")
        if not _is_https_url(values["privacy_url.txt"]):
            raise ValueError(f"Invalid privacy URL for {locale}")

    count = len(expected)
    return MetadataSummary(count, count, count, count, count, count, count, count)


def stage_all_app_store_assets(
    artifacts_root: Path,
    metadata_root: Path,
    screenshots_root: Path,
) -> AssetSummary:
    validate_metadata(metadata_root)
    sources: dict[str, dict[str, list[Path]]] = {}
    for locale, apple_locale in APP_STORE_LOCALES.items():
        artifact = artifacts_root / f"store-screenshots-{locale}"
        if not artifact.is_dir():
            raise ValueError(f"Missing artifact for {locale}")
        decks: dict[str, list[Path]] = {}
        for device, size in (("iphone", "1320x2868"), ("ipad", "2064x2752")):
            files = sorted((artifact / device / size).glob("*.png"))
            if len(files) != 7:
                raise ValueError(
                    f"Expected 7 {device} screenshots for {locale}, found {len(files)}"
                )
            if tuple(path.name for path in files) != SCREENSHOT_FILENAMES:
                raise ValueError(f"Unexpected {device} screenshot names for {locale}")
            decks[device] = files
        sources[apple_locale] = decks

    if screenshots_root.exists():
        shutil.rmtree(screenshots_root)
    screenshots_root.mkdir(parents=True)
    iphone_count = 0
    ipad_count = 0
    for apple_locale, decks in sources.items():
        destination = screenshots_root / apple_locale
        destination.mkdir()
        for device, size in (("iphone", "1320x2868"), ("ipad", "2064x2752")):
            for source in decks[device]:
                slide, rest = source.name.split("-", 1)
                shutil.copy2(source, destination / f"{slide}-{device}-{size}-{rest}")
                if device == "iphone":
                    iphone_count += 1
                else:
                    ipad_count += 1

    return AssetSummary(
        locales=len(sources),
        iphone_screenshots=iphone_count,
        ipad_screenshots=ipad_count,
        screenshots=iphone_count + ipad_count,
    )


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(required=True)
    stage_parser = subparsers.add_parser("stage")
    stage_parser.add_argument("--artifacts-root", type=Path, required=True)
    stage_parser.add_argument("--metadata-root", type=Path, required=True)
    stage_parser.add_argument("--screenshots-root", type=Path, required=True)
    args = parser.parse_args(argv)

    summary = stage_all_app_store_assets(
        args.artifacts_root,
        args.metadata_root,
        args.screenshots_root,
    )
    print(
        f"Staged {summary.locales} App Store locales: "
        f"{summary.iphone_screenshots} iPhone screenshots, "
        f"{summary.ipad_screenshots} iPad screenshots, "
        f"{summary.screenshots} total."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
