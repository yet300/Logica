#!/usr/bin/env python3
"""Stage one generated screenshot locale into Fastlane's store conventions."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from tools.store_screenshots.app_store_listing import APP_STORE_LOCALES, app_store_locale
from tools.store_screenshots.play_listing import play_store_locale

DEVICE_SIZES = {
    "iphone": ("1320x2868", "1284x2778", "1206x2622", "1125x2436"),
    "ipad": ("2064x2752", "2048x2732"),
    "android": ("1080x1920",),
    "feature-graphic": ("1024x500",),
}

APPLE_REQUIRED_SIZES = {
    "iphone": ("1320x2868",),
    "ipad": ("2064x2752",),
}


def canonical_pngs(locale_root: Path) -> list[Path]:
    expected: list[Path] = []
    canonical_sizes = {
        **APPLE_REQUIRED_SIZES,
        "android": ("1080x1920",),
        "feature-graphic": ("1024x500",),
    }
    for device, sizes in canonical_sizes.items():
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
    source_pngs = canonical_pngs(locale_root)
    if len(source_pngs) != 22:
        raise ValueError(f"Expected 22 canonical PNGs for {locale}, found {len(source_pngs)}")

    fastlane_root = fastlane_root.resolve()
    apple_code = APP_STORE_LOCALES.get(locale)
    play_code = play_store_locale(locale)
    summary = {"apple": 0, "androidPhone": 0, "featureGraphic": 0}

    if apple_code:
        apple_destination = fastlane_root / "screenshots" / apple_code
        replace_directory(apple_destination)
        for device, sizes in APPLE_REQUIRED_SIZES.items():
            for size in sizes:
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
