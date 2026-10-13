#!/usr/bin/env python3
"""Fit a supplied transparent beer icon into Minecraft's 18px effect sprite.

This is only a nearest-neighbour import step; the production PNG is checked in
and neither builds nor the game depend on the original image or this script.
"""

import argparse
from pathlib import Path

from PIL import Image


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="Transparent source PNG")
    args = parser.parse_args()
    with Image.open(args.source) as source:
        source = source.convert("RGBA")
        # Ignore nearly transparent export pixels when locating the sprite;
        # keep the actual cropped image's original alpha channel unchanged.
        bounds = source.getchannel("A").point(lambda alpha: 255 if alpha >= 128 else 0).getbbox()
        if bounds is None:
            raise ValueError("The supplied icon is completely transparent")
        sprite = source.crop(bounds)
        sprite.thumbnail((16, 16), Image.Resampling.NEAREST)
        icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
        icon.paste(sprite, ((18 - sprite.width) // 2, (18 - sprite.height) // 2))
        output = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk/textures/mob_effect/beer.png"
        output.parent.mkdir(parents=True, exist_ok=True)
        icon.save(output)
        print(f"Imported {icon.width}x{icon.height} beer effect sprite")


if __name__ == "__main__":
    main()
