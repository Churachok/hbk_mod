#!/usr/bin/env python3
"""Generate deterministic 16x16 textures and repetitive resources for Past Gornoslavyansk."""

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets/hbk"
DATA = RESOURCES / "data/hbk"
TEXTURES = ASSETS / "textures/block"
SIZE = 16
BLOCKS = [
    "bright_grass", "rotten_earth", "sperm", "bloody_sperm",
    "ruined_concrete", "building_debris", "crimson_monument",
]
TRANSLUCENT = {"sperm", "bloody_sperm"}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def save(image, name):
    TEXTURES.mkdir(parents=True, exist_ok=True)
    image.save(TEXTURES / f"{name}.png")


def noise(base, shades, seed, alpha=255, accents=()):
    rng = random.Random(seed)
    image = Image.new("RGBA", (SIZE, SIZE), (*base, alpha))
    pixels = image.load()
    for y in range(SIZE):
        for x in range(SIZE):
            color = rng.choice(shades) if rng.random() < 0.56 else base
            pixels[x, y] = (*color, alpha)
    for color, count in accents:
        for _ in range(count):
            x, y = rng.randrange(SIZE), rng.randrange(SIZE)
            pixels[x, y] = (*color, alpha)
    return image


def generate_textures():
    grass_top = noise((16, 184, 22), [(7, 131, 17), (21, 218, 27), (38, 239, 43)], 401,
                      accents=[((4, 91, 12), 13), ((88, 255, 63), 8)])
    save(grass_top, "bright_grass_top")

    dirt = noise((55, 39, 31), [(38, 27, 24), (72, 48, 36), (91, 58, 41)], 402,
                 accents=[((30, 22, 21), 11)])
    save(dirt, "bright_grass_bottom")
    side = dirt.copy()
    side_pixels = side.load()
    top_pixels = grass_top.load()
    rng = random.Random(403)
    for x in range(SIZE):
        depth = 2 + rng.randrange(3)
        for y in range(depth):
            side_pixels[x, y] = top_pixels[x, y]
        if rng.random() < 0.7:
            for y in range(depth, min(SIZE, depth + rng.randrange(2, 7))):
                side_pixels[x, y] = (10, rng.randrange(120, 220), 17, 255)
    save(side, "bright_grass_side")

    save(noise((48, 34, 29), [(29, 25, 25), (67, 44, 35), (81, 51, 37)], 404,
               accents=[((104, 70, 49), 8), ((25, 28, 29), 18)]), "rotten_earth")

    white = noise((211, 207, 216), [(181, 177, 190), (232, 229, 238), (245, 242, 247)], 405, 225)
    draw = ImageDraw.Draw(white)
    draw.line([(0, 4), (4, 3), (8, 5), (12, 4), (15, 5)], fill=(250, 247, 252, 235), width=1)
    draw.line([(0, 12), (5, 11), (10, 13), (15, 11)], fill=(167, 164, 176, 225), width=1)
    save(white, "sperm")

    bloody = white.copy()
    draw = ImageDraw.Draw(bloody)
    draw.line([(0, 3), (3, 5), (6, 4), (9, 7), (13, 6), (15, 8)], fill=(112, 5, 19, 238), width=2)
    draw.line([(2, 15), (4, 11), (8, 12), (11, 9), (15, 10)], fill=(151, 9, 24, 242), width=2)
    for box in [(1, 3, 2, 4), (8, 6, 10, 7), (12, 8, 13, 9), (4, 11, 5, 12)]:
        draw.rectangle(box, fill=(188, 14, 30, 240))
    save(bloody, "bloody_sperm")

    concrete = noise((76, 75, 79), [(53, 54, 58), (91, 90, 94), (109, 106, 108)], 406,
                     accents=[((34, 34, 37), 18)])
    draw = ImageDraw.Draw(concrete)
    draw.line([(1, 0), (5, 5), (4, 10), (9, 15)], fill=(27, 28, 31, 255), width=1)
    draw.line([(15, 2), (11, 6), (13, 11)], fill=(38, 38, 42, 255), width=1)
    save(concrete, "ruined_concrete")

    debris = noise((40, 40, 43), [(21, 22, 24), (58, 57, 61), (75, 73, 76)], 407,
                   accents=[((104, 102, 107), 15), ((15, 15, 17), 25)])
    draw = ImageDraw.Draw(debris)
    for box in [(1, 2, 5, 5), (9, 1, 13, 4), (5, 9, 9, 13), (12, 11, 15, 15)]:
        draw.rectangle(box, outline=(22, 22, 24, 255))
    save(debris, "building_debris")

    crimson = noise((93, 5, 17), [(58, 4, 13), (121, 7, 23), (145, 10, 29)], 408,
                    accents=[((190, 17, 36), 18), ((44, 3, 10), 20)])
    draw = ImageDraw.Draw(crimson)
    draw.line([(0, 6), (5, 5), (9, 7), (15, 5)], fill=(43, 2, 9, 255), width=1)
    draw.line([(4, 0), (5, 5), (3, 10), (6, 15)], fill=(168, 10, 30, 255), width=1)
    save(crimson, "crimson_monument")


def generate_resources():
    for name in BLOCKS:
        write_json(ASSETS / "blockstates" / f"{name}.json", {
            "variants": {"": {"model": f"hbk:block/{name}"}}
        })
        if name == "bright_grass":
            block_model = {
                "parent": "minecraft:block/cube_bottom_top",
                "textures": {
                    "top": "hbk:block/bright_grass_top",
                    "bottom": "hbk:block/bright_grass_bottom",
                    "side": "hbk:block/bright_grass_side",
                },
            }
        else:
            texture = f"hbk:block/{name}"
            if name in TRANSLUCENT:
                texture = {"sprite": texture, "force_translucent": True}
            block_model = {"parent": "minecraft:block/cube_all", "textures": {"all": texture}}
        write_json(ASSETS / "models/block" / f"{name}.json", block_model)
        write_json(ASSETS / "models/item" / f"{name}.json", {"parent": f"hbk:block/{name}"})
        write_json(ASSETS / "items" / f"{name}.json", {
            "model": {"type": "minecraft:model", "model": f"hbk:item/{name}"}
        })
        write_json(DATA / "loot_table/blocks" / f"{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"hbk:{name}"}],
                "rolls": 1.0,
            }],
            "random_sequence": f"hbk:blocks/{name}",
        })


def main():
    generate_textures()
    generate_resources()
    print(f"Generated {len(BLOCKS)} Past Gornoslavyansk resource sets")


if __name__ == "__main__":
    main()
