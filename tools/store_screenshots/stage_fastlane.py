#!/usr/bin/env python3
"""Stage one generated screenshot locale into Fastlane's store conventions."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path


APP_STORE_LOCALES: dict[str, str | None] = {
    "ar": "ar-SA", "az": None, "be": None, "bn": "bn-BD", "da": "da",
    "de": "de-DE", "el": "el", "en": "en-US", "es": "es-ES", "fi": "fi",
    "fr": "fr-FR", "he": "he", "hi": "hi", "hu": "hu", "hy": None,
    "id": "id", "it": "it", "ja": "ja", "ka": None, "kk": None,
    "ko": "ko", "ky": None, "nb": "no", "nl": "nl-NL", "pl": "pl",
    "pt": "pt-BR", "ro": "ro", "ru": "ru", "sv": "sv", "tg": None,
    "th": "th", "tk": None, "tr": "tr", "uk": "uk", "uz": None,
    "vi": "vi", "zh": "zh-Hant",
}

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

DEVICE_SIZES = {
    "iphone": ("1320x2868", "1284x2778", "1206x2622", "1125x2436"),
    "ipad": ("2064x2752", "2048x2732"),
    "android": ("1080x1920",),
    "feature-graphic": ("1024x500",),
}


def app_store_locale(locale: str) -> str | None:
    if locale not in APP_STORE_LOCALES:
        raise ValueError(f"Unsupported locale: {locale}")
    return APP_STORE_LOCALES[locale]


def play_store_locale(locale: str) -> str:
    try:
        return PLAY_STORE_LOCALES[locale]
    except KeyError as error:
        raise ValueError(f"Unsupported locale: {locale}") from error


def expected_pngs(locale_root: Path) -> list[Path]:
    expected: list[Path] = []
    for device, sizes in DEVICE_SIZES.items():
        slide_count = 1 if device == "feature-graphic" else 7
        for size in sizes:
            directory = locale_root / device / size
            files = sorted(directory.glob("*.png")) if directory.is_dir() else []
            if len(files) != slide_count:
                return []
            expected.extend(files)
    return expected


def replace_directory(destination: Path) -> None:
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True, exist_ok=True)


def stage_locale(input_root: Path, fastlane_root: Path, locale: str) -> dict[str, int]:
    locale_root = input_root.resolve() / locale
    source_pngs = expected_pngs(locale_root)
    if len(source_pngs) != 50:
        raise ValueError(f"Expected 50 PNGs for {locale}, found {len(source_pngs)}")

    fastlane_root = fastlane_root.resolve()
    apple_code = app_store_locale(locale)
    play_code = play_store_locale(locale)
    summary = {"apple": 0, "androidPhone": 0, "featureGraphic": 0}

    if apple_code:
        apple_destination = fastlane_root / "screenshots" / apple_code
        replace_directory(apple_destination)
        for device in ("iphone", "ipad"):
            for size in DEVICE_SIZES[device]:
                for source in sorted((locale_root / device / size).glob("*.png")):
                    slide_prefix, rest = source.name.split("-", 1)
                    destination = apple_destination / f"{slide_prefix}-{device}-{size}-{rest}"
                    shutil.copy2(source, destination)
                    summary["apple"] += 1

    images_root = fastlane_root / "metadata" / "android" / play_code / "images"
    phone_destination = images_root / "phoneScreenshots"
    replace_directory(phone_destination)
    for source in sorted((locale_root / "android" / "1080x1920").glob("*.png")):
        shutil.copy2(source, phone_destination / source.name)
        summary["androidPhone"] += 1

    images_root.mkdir(parents=True, exist_ok=True)
    shutil.copy2(
        locale_root / "feature-graphic" / "1024x500" / "01-feature-graphic.png",
        images_root / "featureGraphic.png",
    )
    summary["featureGraphic"] = 1
    return summary


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--fastlane-root", type=Path, default=Path("fastlane"))
    parser.add_argument("--locale", required=True)
    args = parser.parse_args()
    summary = stage_locale(args.input, args.fastlane_root, args.locale)
    print(
        f"Staged {args.locale}: {summary['apple']} Apple screenshots, "
        f"{summary['androidPhone']} Android screenshots, "
        f"{summary['featureGraphic']} feature graphic."
    )


if __name__ == "__main__":
    main()
