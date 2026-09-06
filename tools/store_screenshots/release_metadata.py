#!/usr/bin/env python3
"""Generate complete localized release metadata for both stores.

The checked-in JSON is the translation source of truth.  The command updates
all App Store Connect ``release_notes.txt`` files and the Google Play
``changelogs/<version-code>.txt`` files in one repeatable step.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


IOS_LOCALES = (
    "ar-SA", "bn-BD", "da", "de-DE", "el", "en-US", "es-ES", "fi",
    "fr-FR", "he", "hi", "hu", "id", "it", "ja", "ko", "nl-NL", "no",
    "pl", "pt-BR", "ro", "ru", "sv", "th", "tr", "uk", "vi", "zh-Hant",
)
PLAY_LOCALES = (
    "ar", "az-AZ", "be", "bn-BD", "da-DK", "de-DE", "el-GR", "en-US",
    "es-ES", "fi-FI", "fr-FR", "hi-IN", "iw-IL", "hu-HU", "hy-AM", "id",
    "it-IT", "ja-JP", "ka-GE", "kk", "ko-KR", "ky-KG", "no-NO", "nl-NL",
    "pl-PL", "pt-BR", "ro", "ru-RU", "sv-SE", "th", "tr-TR", "uk", "vi",
    "zh-TW",
)
PLAY_TO_IOS = {
    "ar": "ar-SA", "bn-BD": "bn-BD", "da-DK": "da", "de-DE": "de-DE",
    "el-GR": "el", "en-US": "en-US", "es-ES": "es-ES", "fi-FI": "fi",
    "fr-FR": "fr-FR", "hi-IN": "hi", "hu-HU": "hu", "id": "id",
    "it-IT": "it", "ja-JP": "ja", "ko-KR": "ko", "nl-NL": "nl-NL",
    "no-NO": "no", "pl-PL": "pl", "pt-BR": "pt-BR", "ro": "ro",
    "ru-RU": "ru", "sv-SE": "sv", "th": "th", "tr-TR": "tr",
    "uk": "uk", "vi": "vi", "zh-TW": "zh-Hant", "iw-IL": "he",
}


def read_notes(path: Path) -> dict[str, str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or not data:
        raise ValueError("translation source must be a non-empty JSON object")
    notes = {str(locale): str(value).strip() for locale, value in data.items()}
    missing = sorted(set(IOS_LOCALES) - notes.keys())
    empty = sorted(locale for locale, value in notes.items() if not value)
    if missing:
        raise ValueError(f"missing iOS release notes locales: {', '.join(missing)}")
    if empty:
        raise ValueError(f"empty release notes locales: {', '.join(empty)}")
    return notes


def bump_versions(root: Path, version_name: str, version_code: str) -> None:
    if not re.fullmatch(r"\d+\.\d+\.\d+", version_name):
        raise ValueError("version name must look like X.Y.Z")
    if not version_code.isdigit() or int(version_code) <= 0:
        raise ValueError("version code must be a positive integer")

    properties = root / "gradle.properties"
    text = properties.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^appVersionName=.*$", f"appVersionName={version_name}", text)
    text = re.sub(r"(?m)^appVersionCode=.*$", f"appVersionCode={version_code}", text)
    properties.write_text(text, encoding="utf-8")

    config = root / "iosApp/Configuration/Config.xcconfig"
    text = config.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^MARKETING_VERSION=.*$", f"MARKETING_VERSION={version_name}", text)
    text = re.sub(r"(?m)^CURRENT_PROJECT_VERSION=.*$", f"CURRENT_PROJECT_VERSION={version_code}", text)
    config.write_text(text, encoding="utf-8")


def generate(root: Path, version_code: str, notes_path: Path) -> None:
    notes = read_notes(notes_path)
    ios_root = root / "fastlane/metadata/ios"
    play_root = root / "fastlane/metadata/android"

    for locale in IOS_LOCALES:
        destination = ios_root / locale / "release_notes.txt"
        destination.write_text(notes[locale] + "\n", encoding="utf-8")

    english = notes["en-US"]
    for locale in PLAY_LOCALES:
        destination = play_root / locale / "changelogs" / f"{version_code}.txt"
        destination.parent.mkdir(parents=True, exist_ok=True)
        # Play uses the same release-notes API shape. Reuse the closest iOS
        # translation for locales that have a different store locale code.
        ios_locale = PLAY_TO_IOS.get(locale)
        destination.write_text(notes.get(ios_locale, english) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", required=True)
    parser.add_argument("--notes", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    bump_versions(root, args.version_name, args.version_code)
    generate(root, args.version_code, args.notes.resolve())
    print(f"Updated version {args.version_name} ({args.version_code}) and localized release notes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
