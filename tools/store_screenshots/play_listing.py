#!/usr/bin/env python3
"""Validate and stage the complete localized Google Play listing."""

from __future__ import annotations

import argparse
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


TITLE = "Logica — Block Puzzle"

PLAY_STORE_LOCALES: dict[str, str] = {
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

PHONE_SCREENSHOT_FILENAMES = (
    "01-hero.png",
    "02-device-bottom.png",
    "03-device-top.png",
    "04-hero.png",
    "05-device-bottom.png",
    "06-three-devices.png",
    "07-hero.png",
)


@dataclass(frozen=True)
class MetadataSummary:
    locales: int
    titles: int
    short_descriptions: int
    full_descriptions: int


@dataclass(frozen=True)
class AssetSummary:
    locales: int
    phone_screenshots: int
    feature_graphics: int


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


def stage_all_play_assets(
    artifacts_root: Path,
    metadata_root: Path,
) -> AssetSummary:
    validate_metadata(metadata_root)
    sources: dict[str, tuple[list[Path], Path]] = {}
    for locale, play_locale in PLAY_STORE_LOCALES.items():
        artifact = artifacts_root / f"store-screenshots-{locale}"
        if not artifact.is_dir():
            raise ValueError(f"Missing artifact for {locale}")
        phones = sorted((artifact / "android/1080x1920").glob("*.png"))
        feature = artifact / "feature-graphic/1024x500/01-feature-graphic.png"
        if len(phones) != 7:
            raise ValueError(f"Expected 7 phone screenshots for {locale}, found {len(phones)}")
        if tuple(path.name for path in phones) != PHONE_SCREENSHOT_FILENAMES:
            raise ValueError(f"Unexpected phone screenshot names for {locale}")
        if not feature.is_file():
            raise ValueError(f"Missing feature graphic for {locale}")
        sources[play_locale] = (phones, feature)

    for play_locale, (phones, feature) in sources.items():
        images_root = metadata_root / play_locale / "images"
        if images_root.exists():
            shutil.rmtree(images_root)
        phone_destination = images_root / "phoneScreenshots"
        phone_destination.mkdir(parents=True)
        for source in phones:
            shutil.copy2(source, phone_destination / source.name)
        shutil.copy2(feature, images_root / "featureGraphic.png")

    return AssetSummary(
        locales=len(sources),
        phone_screenshots=sum(len(phones) for phones, _ in sources.values()),
        feature_graphics=len(sources),
    )


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(required=True)
    stage_parser = subparsers.add_parser("stage")
    stage_parser.add_argument("--artifacts-root", type=Path, required=True)
    stage_parser.add_argument("--metadata-root", type=Path, required=True)
    args = parser.parse_args(argv)

    summary = stage_all_play_assets(args.artifacts_root, args.metadata_root)
    print(
        f"Staged {summary.locales} Play Store locales: "
        f"{summary.phone_screenshots} phone screenshots, "
        f"{summary.feature_graphics} feature graphics."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
