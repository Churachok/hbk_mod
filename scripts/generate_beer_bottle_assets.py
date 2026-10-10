#!/usr/bin/env python3
"""Build the blocky green beer bottle from the Models-chat concept.

Requires Pillow. Geometry, labels and palette are repository-local and reproducible;
the original reference/concept image is not a runtime or generator dependency.
"""

import json
from pathlib import Path
import random

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk"
GLYPHS = {
    "H": ["101", "101", "111", "101", "101"],
    "e": ["000", "111", "101", "110", "111"],
    "i": ["010", "000", "010", "010", "010"],
    "n": ["000", "110", "101", "101", "101"],
    "k": ["100", "101", "110", "101", "101"],
}


def texture():
    rng = random.Random(67)
    image = Image.new("RGBA", (64, 64), "#18652b")
    for x in range(0, 64, 2):
        for y in range(0, 64, 2):
            shade = rng.choice((0, 8, 14, 22, -8))
            base = (22, 101, 32) if x < 32 else (17, 82, 28)
            color = tuple(max(0, min(255, value + shade)) for value in base) + (255,)
            ImageDraw.Draw(image).rectangle((x, y, x + 1, y + 1), fill=color)
    d = ImageDraw.Draw(image)
    # Front body, 32 x 48 texels: pale oval, red star and black Heineken band.
    d.line((2, 1, 2, 45), fill="#599e2a", width=2)
    d.ellipse((2, 7, 29, 44), fill="#eef4cc")
    d.ellipse((4, 9, 27, 42), fill="#238938")
    d.ellipse((6, 11, 25, 40), outline="#d7e5bf", width=1)
    d.ellipse((9, 14, 22, 36), fill="#d9e9c4")
    d.polygon([(16, 13), (18, 18), (23, 18), (19, 21), (21, 26),
               (16, 23), (11, 26), (13, 21), (9, 18), (14, 18)], fill="#d52117")
    d.rectangle((0, 25, 31, 34), fill="#ebf1d1")
    d.rectangle((0, 26, 31, 33), fill="#111d17")
    for index, letter in enumerate("Heineken"):
        for row, bits in enumerate(GLYPHS[letter]):
            for column, bit in enumerate(bits):
                if bit == "1":
                    d.point((index * 4 + column, 27 + row), fill="#ffffff")
    d.line((13, 36, 19, 36), fill="#76a579")
    d.rectangle((13, 38, 19, 39), outline="#76a579")
    # Neck-label patch at (0, 48), plain glass at (16, 48), cap at (32, 48).
    d.rectangle((0, 48, 15, 63), fill="#215e2c")
    d.rectangle((3, 49, 12, 62), fill="#4c962d")
    d.polygon([(8, 49), (9, 51), (12, 51), (10, 53), (11, 56),
               (8, 54), (5, 56), (6, 53), (4, 51), (7, 51)], fill="#dc291e")
    d.line((6, 58, 10, 58), fill="#f2f5d8")
    d.line((6, 60, 10, 60), fill="#f2f5d8")
    d.rectangle((32, 48, 47, 63), fill="#165b25")
    for x in range(33, 47, 3):
        d.line((x, 49, x, 62), fill="#319328")
    folder = ASSETS / "textures/item"
    folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / "beer_bottle.png")


def model():
    front, back = [0, 0, 8, 12], [8, 0, 16, 12]
    glass, neck, cap = [4, 12, 8, 16], [0, 12, 4, 16], [8, 12, 12, 16]

    def cube(start, end, surface, front_surface=None):
        faces = {name: {"texture": "#bottle", "uv": surface}
                 for name in ("up", "down", "north", "south", "east", "west")}
        if front_surface is not None:
            # The bottle has a wrapping label, readable from any viewing angle.
            for side in ("north", "south", "east", "west"):
                faces[side]["uv"] = front_surface
        return {"from": start, "to": end, "faces": faces}

    result = {
        "parent": "minecraft:block/block",
        "textures": {"bottle": "hbk:item/beer_bottle", "particle": "hbk:item/beer_bottle"},
        "elements": [
            cube([5.8, 0.7, 5.8], [10.2, 1.5, 10.2], glass),
            cube([6, 1.5, 6], [10, 9, 10], back, front),
            cube([6.3, 9, 6.3], [9.7, 9.75, 9.7], glass),
            cube([6.9, 9.75, 6.9], [9.1, 10.5, 9.1], glass),
            cube([7.15, 10.5, 7.15], [8.85, 14.7, 8.85], glass, neck),
            cube([7, 14.7, 7], [9, 15.3, 9], cap),
        ],
        "display": {
            "gui": {"rotation": [10, -25, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.65, 0.65, 0.65]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.7, 0.7, 0.7]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.7, 0.7, 0.7]},
            "firstperson_righthand": {"rotation": [0, -20, 0], "translation": [0, 1, 0], "scale": [0.8, 0.8, 0.8]},
            "firstperson_lefthand": {"rotation": [0, 20, 0], "translation": [0, 1, 0], "scale": [0.8, 0.8, 0.8]},
        },
    }
    for folder, value in (
        ("models/item", result),
        ("items", {"model": {"type": "minecraft:model", "model": "hbk:item/beer_bottle"}}),
    ):
        target = ASSETS / folder
        target.mkdir(parents=True, exist_ok=True)
        (target / "beer_bottle.json").write_text(json.dumps(value, indent=2) + "\n")


if __name__ == "__main__":
    texture()
    model()
    print("Generated the six-cuboid green beer bottle, pixel labels and item definition")
