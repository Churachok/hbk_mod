#!/usr/bin/env python3
"""Reproduce HBK's rofl block and stepped mushroom assets (requires Pillow)."""

import json
import random
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk"


def write_json(relative, data):
    target = ROOT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def texture(name, palette, spots=False, seed=67):
    rng = random.Random(seed)
    image = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            image.putpixel((x, y), ImageColor.getrgb(rng.choices(palette, weights=[8, 2, 1])[0]))
    if spots:
        draw = ImageDraw.Draw(image)
        for x, y, w, h in [(2, 2, 3, 2), (10, 1, 2, 3), (7, 7, 3, 2), (1, 11, 2, 3), (12, 12, 3, 2)]:
            draw.rectangle((x, y, x + w - 1, y + h - 1), fill="#fff0a3")
            draw.point((x + w - 1, y + h - 1), fill="#d9ba53")
    target = ROOT / "textures/block" / f"{name}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)


def cuboid(start, end, side, top, bottom):
    return {
        "from": start,
        "to": end,
        "faces": {direction: {"texture": f"#{tex}"} for direction, tex in {
            "north": side, "south": side, "east": side, "west": side,
            "up": top, "down": bottom,
        }.items()},
    }


def main():
    texture("rofl_block", ["#edbd14", "#e3af0c", "#f6cd25"], spots=True)
    texture("rofl_mushroom_cap", ["#f4c415", "#eab80d", "#ffda36"], spots=True)
    texture("rofl_mushroom_side", ["#e7ab09", "#d79708", "#f4bc17"], spots=True, seed=68)
    texture("rofl_mushroom_under", ["#dbc598", "#cfb989", "#e8d6ad"])
    texture("rofl_mushroom_stem", ["#e9d9b7", "#d5c29f", "#f1e3c8"])
    write_json("models/block/rofl_block.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": "hbk:block/rofl_block"},
    })
    write_json("models/block/rofl_mushroom.json", {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": "hbk:block/rofl_mushroom_cap",
            "cap": "hbk:block/rofl_mushroom_cap",
            "side": "hbk:block/rofl_mushroom_side",
            "under": "hbk:block/rofl_mushroom_under",
            "stem": "hbk:block/rofl_mushroom_stem",
        },
        "elements": [
            cuboid([6, 0, 6], [10, 8, 10], "stem", "stem", "stem"),
            cuboid([1, 8, 1], [15, 11, 15], "side", "cap", "under"),
            cuboid([3, 11, 3], [13, 13, 13], "side", "cap", "cap"),
            cuboid([5, 13, 5], [11, 15, 11], "side", "cap", "cap"),
        ],
    })
    for name in ["rofl_block", "rofl_mushroom"]:
        write_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"hbk:block/{name}"}}})
        write_json(f"models/item/{name}.json", {"parent": f"hbk:block/{name}"})
        write_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"hbk:item/{name}"}})


if __name__ == "__main__":
    main()
