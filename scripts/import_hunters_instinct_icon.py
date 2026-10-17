#!/usr/bin/env python3
"""Import the generated transparent stag without redrawing or recoloring pixels.

Only alpha-bounds cropping, nearest-neighbour resizing and transparent padding
are performed. Builds and the game consume the finished PNGs, not this script.
"""

import argparse
from io import BytesIO
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_HUD_SOURCE = ROOT / "docs/assets/hunters-instinct-source-v4.png"
DEFAULT_ANIMATION_SOURCE = ROOT / "docs/assets/hunters-instinct-source-v3.png"
TEXTURES = ROOT / "src/main/resources/assets/hbk/textures"


def fit(sprite, size, content_size):
    scaled = sprite.copy()
    scaled.thumbnail((content_size, content_size), Image.Resampling.NEAREST)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    result.paste(scaled, ((size - scaled.width) // 2, (size - scaled.height) // 2))
    return result


def load_sprite(path):
    with Image.open(path) as source:
        source = source.convert("RGBA")
        # Near-transparent export fringes do not define the crop; the cropped
        # artwork retains the original alpha, including partial-alpha edges.
        bounds = source.getchannel("A").point(lambda alpha: 255 if alpha >= 128 else 0).getbbox()
        if bounds is None:
            raise ValueError(f"The supplied stag is completely transparent: {path}")
        return source.crop(bounds)


def save_if_changed(image, target):
    encoded = BytesIO()
    image.save(encoded, format="PNG")
    payload = encoded.getvalue()
    if target.exists() and target.read_bytes() == payload:
        print(f"Unchanged {image.width}x{image.height} {target.name}")
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(payload)
    print(f"Imported {image.width}x{image.height} {target.name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--hud-source", type=Path, default=DEFAULT_HUD_SOURCE,
                        help="Transparent v4 source for the refined 64px HUD icon only")
    parser.add_argument("--animation-source", type=Path, default=DEFAULT_ANIMATION_SOURCE,
                        help="Original v3 source for the unchanged 128px rescue-animation sprite")
    args = parser.parse_args()
    hud = fit(load_sprite(args.hud_source), 64, 60)
    save_if_changed(hud, TEXTURES / "mob_effect/hunters_instinct.png")
    # Preserve the exact v3 algorithm and input used by the original rescue
    # animation. Refining the HUD icon must never change that sprite.
    animation = fit(load_sprite(args.animation_source), 128, 112)
    save_if_changed(animation, TEXTURES / "item/jagermeister_stag.png")


if __name__ == "__main__":
    main()
