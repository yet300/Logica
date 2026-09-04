from argparse import ArgumentParser
from pathlib import Path
import re

from PIL import Image, ImageDraw, ImageFont, ImageOps


EXPECTED = {
    "iphone/1320x2868": {"size": (1320, 2868), "count": 7},
    "iphone/1284x2778": {"size": (1284, 2778), "count": 7},
    "iphone/1206x2622": {"size": (1206, 2622), "count": 7},
    "iphone/1125x2436": {"size": (1125, 2436), "count": 7},
    "ipad/2064x2752": {"size": (2064, 2752), "count": 7},
    "ipad/2048x2732": {"size": (2048, 2732), "count": 7},
    "android/1080x1920": {"size": (1080, 1920), "count": 7},
    "feature-graphic/1024x500": {"size": (1024, 500), "count": 1},
}
PRIMARY = (
    "iphone/1320x2868",
    "ipad/2064x2752",
    "android/1080x1920",
    "feature-graphic/1024x500",
)


def verify_exports(export_root: Path) -> list[Path]:
    export_root = Path(export_root)
    verified = []
    for relative, contract in EXPECTED.items():
        directory = export_root / relative
        files = sorted(directory.glob("*.png")) if directory.is_dir() else []
        if len(files) != contract["count"]:
            raise ValueError(
                f"{relative}: expected {contract['count']} PNGs, found {len(files)}"
            )
        expected_prefixes = [
            f"{index:02d}-" for index in range(1, contract["count"] + 1)
        ]
        actual_prefixes = [
            match.group(1) if (match := re.match(r"^(\d{2}-)", file.name)) else ""
            for file in files
        ]
        if actual_prefixes != expected_prefixes:
            raise ValueError(
                f"{relative}: filenames must use ordered zero-padded prefixes"
            )
        for file in files:
            with Image.open(file) as image:
                if image.size != contract["size"]:
                    raise ValueError(
                        f"{file}: wrong size {image.size}, expected {contract['size']}"
                    )
                if image.mode != "RGB" or "transparency" in image.info:
                    raise ValueError(f"{file}: must be opaque RGB")
            verified.append(file)
    return verified


def build_contact_sheet(
    export_root: Path, output_path: Path, thumb_width: int = 180
) -> Path:
    verify_exports(export_root)
    margin, gap, label_height, cell_height = 28, 10, 34, 300
    canvas = Image.new(
        "RGB",
        (margin * 2 + 7 * thumb_width + 6 * gap, margin * 2 + 4 * cell_height),
        "#EEE9E1",
    )
    draw = ImageDraw.Draw(canvas)
    try:
        font = ImageFont.load_default(size=18)
    except TypeError:
        font = ImageFont.load_default()
    for row, relative in enumerate(PRIMARY):
        files = sorted((Path(export_root) / relative).glob("*.png"))
        draw.text(
            (margin, margin + row * cell_height), relative, font=font, fill="#171715"
        )
        for column, file in enumerate(files):
            with Image.open(file) as source:
                thumb = ImageOps.contain(
                    source.convert("RGB"),
                    (thumb_width, cell_height - label_height - gap),
                )
            x = margin + column * (thumb_width + gap)
            y = margin + row * cell_height + label_height
            canvas.paste(thumb, (x + (thumb_width - thumb.width) // 2, y))
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output_path, "PNG", optimize=True)
    return output_path


def main():
    parser = ArgumentParser()
    parser.add_argument("--export-root", type=Path, required=True)
    parser.add_argument("--contact-sheet", type=Path, required=True)
    args = parser.parse_args()
    files = verify_exports(args.export_root)
    build_contact_sheet(args.export_root, args.contact_sheet)
    print(
        f"Verified {len(files)} opaque RGB PNGs; contact sheet: {args.contact_sheet}"
    )


if __name__ == "__main__":
    main()
