#!/usr/bin/env python3
"""Generate a silver, amethyst-inset plug as a native Minecraft cuboid model."""

import json
from pathlib import Path


ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk"
FACES = ("down", "up", "north", "south", "west", "east")


def box(start, end, texture):
    return {"from": start, "to": end, "faces": {
        face: {"texture": "#" + texture, "uv": [0, 0, 16, 16]} for face in FACES
    }}


def write(relative, value):
    target = ASSETS / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def main():
    parts = [
        # Broad stepped metal rim; purple inset visible from both sides of the base.
        box([4, 1, 5], [12, 2, 11], "metal"),
        box([3, 1, 6], [4, 2, 10], "metal"),
        box([12, 1, 6], [13, 2, 10], "metal"),
        box([5, 0.5, 6], [11, 1, 10], "gem"),
        box([5, 2, 6], [11, 2.5, 10], "gem"),
        box([6, 2.5, 6.5], [10, 2.8, 9.5], "gem_light"),
        box([7, 2.8, 7], [9, 6, 9], "metal"),
        # A faceted teardrop: widest at the bottom, tapered toward the rounded tip.
        box([5.5, 6, 5.5], [10.5, 7, 10.5], "metal"),
        box([4.5, 7, 4.5], [11.5, 10, 11.5], "metal"),
        box([5, 10, 5], [11, 11.5, 11], "metal"),
        box([5.5, 11.5, 5.5], [10.5, 13, 10.5], "metal"),
        box([6, 13, 6], [10, 14, 10], "metal"),
        box([6.5, 14, 6.5], [9.5, 15, 9.5], "metal"),
        box([7, 15, 7], [9, 15.5, 9], "metal"),
    ]
    write("models/item/furry_plug.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "minecraft:block/iron_block", "metal": "minecraft:block/iron_block",
                     "gem": "minecraft:block/amethyst_block", "gem_light": "minecraft:block/purple_stained_glass"},
        "elements": parts,
        "display": {
            "gui": {"rotation": [25, -35, -30], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.32, 0.32, 0.32]},
            "fixed": {"rotation": [0, 0, -30], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            "firstperson_righthand": {"rotation": [0, -90, 20], "translation": [1, 1, 0], "scale": [0.48, 0.48, 0.48]},
            "firstperson_lefthand": {"rotation": [0, 90, -20], "translation": [1, 1, 0], "scale": [0.48, 0.48, 0.48]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.48, 0.48, 0.48]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.48, 0.48, 0.48]},
        },
    })
    write("items/furry_plug.json", {"model": {"type": "minecraft:model", "model": "hbk:item/furry_plug"}})


if __name__ == "__main__":
    main()
