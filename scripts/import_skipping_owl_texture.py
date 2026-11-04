#!/usr/bin/env python3
"""Import the checked-in imagegen material atlas; no generation service is needed to rebuild it."""

from pathlib import Path
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art/skipping_owl/material-atlas.png"
OUTPUT = ROOT / "src/main/resources/assets/hbk/textures/entity/skipping_owl.png"


def main() -> None:
    with Image.open(SOURCE) as source:
        if source.width != source.height:
            raise ValueError("The owl's four material quadrants require a square source atlas")
        atlas = source.convert("RGB").resize((128, 128), Image.Resampling.NEAREST)
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        atlas.save(OUTPUT)


if __name__ == "__main__":
    main()
