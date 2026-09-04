from pathlib import Path
from tempfile import TemporaryDirectory
import sys
import unittest

from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.store_screenshots.verify_exports import (
    EXPECTED,
    build_contact_sheet,
    verify_exports,
)


class ExportVerifierTest(unittest.TestCase):
    def make_inventory(self, root: Path):
        for relative, contract in EXPECTED.items():
            directory = root / relative
            directory.mkdir(parents=True)
            width, height = contract["size"]
            for index in range(1, contract["count"] + 1):
                Image.new("RGB", (width, height), (23 + index, 23, 21)).save(
                    directory / f"{index:02d}-shot.png"
                )

    def test_rejects_missing_required_deck(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            self.make_inventory(root)
            for file in (root / "android/1080x1920").glob("*.png"):
                file.unlink()
            with self.assertRaisesRegex(ValueError, "android/1080x1920"):
                verify_exports(root)

    def test_rejects_wrong_dimensions(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            self.make_inventory(root)
            Image.new("RGB", (100, 100)).save(root / "iphone/1320x2868/01-shot.png")
            with self.assertRaisesRegex(ValueError, "wrong size"):
                verify_exports(root)

    def test_rejects_alpha_channel(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            self.make_inventory(root)
            Image.new("RGBA", (1024, 500), (23, 23, 21, 255)).save(
                root / "feature-graphic/1024x500/01-shot.png"
            )
            with self.assertRaisesRegex(ValueError, "must be opaque RGB"):
                verify_exports(root)

    def test_accepts_complete_export_inventory(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            self.make_inventory(root)
            self.assertEqual(len(verify_exports(root)), 50)

    def test_contact_sheet_uses_primary_decks_only(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            self.make_inventory(root)
            output = root / "contact-sheet.png"
            self.assertEqual(build_contact_sheet(root, output), output)
            with Image.open(output) as image:
                self.assertEqual(image.mode, "RGB")
                self.assertIsNotNone(
                    ImageChops.difference(
                        image, Image.new("RGB", image.size, "#EEE9E1")
                    ).getbbox()
                )


if __name__ == "__main__":
    unittest.main()
