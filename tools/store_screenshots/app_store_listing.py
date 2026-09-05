#!/usr/bin/env python3
"""Validate and stage the complete localized App Store listing."""

from __future__ import annotations

import argparse
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


def _print_report(metadata_root: Path) -> None:
    validate_metadata(metadata_root)
    print("locale\tname\tsubtitle\tdescription\tkeywords\tpromotional")
    for locale in sorted(APP_STORE_LOCALES.values()):
        locale_root = metadata_root / locale
        lengths = [
            len(_read_metadata_file(locale_root, filename))
            for filename in METADATA_FILENAMES[:5]
        ]
        print(f"{locale}\t" + "\t".join(str(length) for length in lengths))


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    for command in ("validate-metadata", "report"):
        command_parser = subparsers.add_parser(command)
        command_parser.add_argument("--metadata-root", type=Path, required=True)
    args = parser.parse_args(argv)

    if args.command == "report":
        _print_report(args.metadata_root)
        return 0

    summary = validate_metadata(args.metadata_root)
    print(
        f"Validated {summary.locales} App Store metadata locales: "
        f"{summary.names} names, {summary.subtitles} subtitles, "
        f"{summary.descriptions} descriptions, {summary.keywords} keyword sets, "
        f"{summary.promotional_texts} promotional texts."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
