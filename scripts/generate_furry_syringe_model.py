#!/usr/bin/env python3
"""Build a native cuboid syringe using only existing vanilla block textures."""

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
        box([5.5, 0.5, 6.5], [10.5, 1.5, 9.5], "white"),
        box([7.5, 1.5, 7.5], [8.5, 4, 8.5], "metal"),
        box([5, 4, 6.5], [11, 4.75, 9.5], "white"),
        box([6.5, 5.25, 6.5], [9.5, 10.75, 9.5], "glass"),
        box([7, 5.5, 7], [9, 10.5, 9], "serum"),
        box([6.5, 4.75, 6.5], [9.5, 5.25, 9.5], "white"),
        box([6.5, 10.75, 6.5], [9.5, 11.25, 9.5], "white"),
        box([7, 11.25, 7], [9, 12.25, 9], "metal"),
        box([7.8, 12.25, 7.8], [8.2, 16, 8.2], "metal"),
    ]
    for index in range(4):
        y = 6 + index
        parts.append(box([6.75, y, 6.4], [7.75 if index % 2 else 8.5, y + 0.2, 6.5], "mark"))
    write("models/item/furry_syringe.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "minecraft:block/iron_block", "metal": "minecraft:block/iron_block",
                     "white": "minecraft:block/quartz_block_side", "glass": "minecraft:block/light_blue_stained_glass",
                     "serum": "minecraft:block/lime_concrete", "mark": "minecraft:block/black_concrete"},
        "elements": parts,
        "display": {
            "gui": {"rotation": [15, -35, -35], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
            "ground": {"rotation": [0, 0, -35], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"rotation": [0, 0, -35], "translation": [0, 0, 0], "scale": [0.75, 0.75, 0.75]},
            "firstperson_righthand": {"rotation": [0, -90, 20], "translation": [1, 2, 0], "scale": [0.55, 0.55, 0.55]},
            "firstperson_lefthand": {"rotation": [0, 90, -20], "translation": [1, 2, 0], "scale": [0.55, 0.55, 0.55]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        },
    })
    write("items/furry_syringe.json", {"model": {"type": "minecraft:model", "model": "hbk:item/furry_syringe"}})


if __name__ == "__main__":
    main()
