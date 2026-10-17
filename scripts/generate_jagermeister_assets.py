#!/usr/bin/env python3
"""Generate native Jagermeister bottles, pixel labels and the empty-glass recipe.

Requires Pillow. All geometry and artwork are editable, deterministic repository
sources; the Models-chat concept is not a generator or runtime dependency.
"""

import json
from pathlib import Path
import random

from PIL import Image, ImageDraw


RESOURCES = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = RESOURCES / "assets/hbk"
GLYPHS = {
    "J": ["111", "001", "001", "101", "111"],
    "ä": ["101", "000", "111", "001", "111", "101", "111"],
    "g": ["111", "101", "111", "001", "110"],
    "e": ["111", "100", "110", "100", "111"],
    "r": ["110", "101", "100", "100", "100"],
    "m": ["000", "111", "111", "101", "101"],
    "i": ["010", "000", "010", "010", "010"],
    "s": ["111", "100", "111", "001", "111"],
    "t": ["010", "111", "010", "010", "011"],
    "5": ["111", "100", "111", "001", "111"],
    "6": ["111", "100", "111", "101", "111"],
}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def pixel_text(draw, value, x, y, color, scale=1):
    for index, letter in enumerate(value):
        rows = GLYPHS[letter]
        baseline_offset = 0 if len(rows) == 7 else 2
        for row, bits in enumerate(rows):
            for column, bit in enumerate(bits):
                if bit == "1":
                    left = x + (index * 4 + column) * scale
                    top = y + (row + baseline_offset) * scale
                    draw.rectangle((left, top, left + scale - 1, top + scale - 1), fill=color)


def texture():
    image = Image.new("RGBA", (128, 128), "#073b22")
    draw = ImageDraw.Draw(image)
    rng = random.Random(56)
    for y in range(0, 96, 2):
        for x in range(0, 128, 2):
            shade = rng.choice((-6, -2, 0, 5, 9))
            base = (8, 62, 32) if x < 64 else (6, 45, 26)
            color = tuple(max(0, min(255, channel + shade)) for channel in base) + (255,)
            draw.rectangle((x, y, x + 1, y + 1), fill=color)
    draw.rectangle((1, 1, 3, 93), fill="#176536")
    draw.rectangle((60, 1, 62, 93), fill="#0a4929")
    draw.rectangle((2, 93, 61, 95), fill="#042a19")

    # Cream paper with green/gold borders, stag/cross medallion, orange brand band.
    draw.rectangle((5, 9, 58, 92), fill="#d4b258")
    draw.rectangle((7, 11, 56, 90), fill="#eee7c9")
    draw.rectangle((9, 13, 54, 88), outline="#567c38", width=2)
    draw.rectangle((12, 15, 51, 86), outline="#d5cba6")
    for y in range(15, 88, 5):
        draw.point((10, y), fill="#e2bb56")
        draw.point((53, y), fill="#e2bb56")
    draw.ellipse((14, 14, 49, 46), fill="#d1a54a")
    draw.ellipse((16, 16, 47, 44), fill="#0c4c28")
    draw.rectangle((30, 19, 32, 28), fill="#f8f2d8")
    draw.rectangle((27, 22, 35, 24), fill="#f8f2d8")
    # Antlers are intentionally stepped pixels, not extra model geometry.
    for points in (
        [(28, 34), (24, 30), (22, 26), (21, 20)],
        [(24, 29), (18, 26), (17, 22)],
        [(22, 25), (25, 22), (25, 19)],
        [(35, 34), (39, 30), (41, 26), (42, 20)],
        [(39, 29), (45, 26), (46, 22)],
        [(41, 25), (38, 22), (38, 19)],
    ):
        draw.line(points, fill="#efe4bc", width=2)
    draw.polygon([(26, 31), (37, 31), (35, 36), (34, 42), (29, 42), (28, 36)], fill="#80532d")
    draw.rectangle((29, 34, 33, 38), fill="#c7ad76")
    draw.rectangle((30, 38, 33, 41), fill="#f2e5bf")
    draw.point((29, 34), fill="#1b2718")
    draw.point((34, 34), fill="#1b2718")
    draw.rectangle((31, 40, 32, 41), fill="#342c1c")
    for x in (15, 18, 21, 42, 45, 48):
        draw.line((x, 40, 31, 47), fill="#c4a247", width=1)
    draw.rectangle((2, 46, 61, 59), fill="#c64c10")
    draw.rectangle((3, 47, 60, 57), fill="#ed6a13")
    draw.line((4, 47, 59, 47), fill="#ff8f24")
    pixel_text(draw, "Jägermeister", 8, 49, "#14251a")

    # Large, legible 56 and simplified fine print/barrel motifs from the concept.
    pixel_text(draw, "56", 25, 61, "#dc5619", scale=2)
    draw.rectangle((13, 68, 21, 69), fill="#c8a145")
    draw.rectangle((43, 68, 50, 69), fill="#c8a145")
    for x in range(17, 49, 4):
        draw.rectangle((x, 76, x + 1, 77), fill="#bbb69e")
    for x, top in ((19, 80), (29, 78), (39, 80)):
        draw.rectangle((x, top, x + 6, 85), outline="#b7b39d")
        draw.line((x + 2, top + 1, x + 2, 84), fill="#cbc6ad")
        draw.line((x + 4, top + 1, x + 4, 84), fill="#cbc6ad")
    draw.rectangle((15, 88, 48, 90), fill="#d65b18")

    # Four disjoint material patches: neck glass, cap, gold, dark open-neck inside.
    draw.rectangle((0, 96, 31, 127), fill="#0b4d29")
    for x in range(2, 32, 6):
        draw.rectangle((x, 97, x + 2, 126), fill="#155e31")
    draw.rectangle((32, 96, 63, 127), fill="#063c21")
    for x in range(34, 64, 6):
        draw.rectangle((x, 97, x + 2, 126), fill="#10572c")
    draw.rectangle((32, 117, 63, 122), fill="#e96b16")
    draw.line((32, 117, 63, 117), fill="#ff962c")
    draw.rectangle((64, 96, 95, 127), fill="#d5a52e")
    for x in range(65, 96, 5):
        draw.rectangle((x, 97, x + 1, 126), fill="#f6d76a")
    draw.rectangle((96, 96, 127, 127), fill="#071e14")
    for x in range(98, 128, 8):
        draw.line((x, 98, x, 125), fill="#0e3821")

    target = ASSETS / "textures/item/jagermeister_bottle.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)


def models():
    front, back = [0, 0, 8, 12], [8, 0, 16, 12]
    glass, cap, gold, inside = [0, 12, 4, 16], [4, 12, 8, 16], [8, 12, 12, 16], [12, 12, 16, 16]

    def cube(start, end, material, front_material=None):
        faces = {side: {"texture": "#bottle", "uv": material}
                 for side in ("up", "down", "north", "south", "east", "west")}
        if front_material is not None:
            faces["north"]["uv"] = front_material
        return {"from": start, "to": end, "faces": faces}

    body = [
        cube([4.5, 0.6, 6.2], [11.5, 1.2, 9.8], glass),
        cube([4.6, 1.2, 6.3], [11.4, 10.3, 9.7], back, front),
        cube([4.9, 10.3, 6.5], [11.1, 10.9, 9.5], glass),
        cube([5.6, 10.9, 6.8], [10.4, 11.5, 9.2], glass),
    ]
    display = {
        "gui": {"rotation": [15, 225, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.65, 0.65, 0.65]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.7, 0.7, 0.7]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.7, 0.7, 0.7]},
        "firstperson_righthand": {"rotation": [0, -20, 0], "translation": [0, 1, 0], "scale": [0.8, 0.8, 0.8]},
        "firstperson_lefthand": {"rotation": [0, 20, 0], "translation": [0, 1, 0], "scale": [0.8, 0.8, 0.8]},
    }
    full = body + [
        cube([6.8, 11.5, 6.9], [9.2, 12.7, 9.1], glass),
        cube([6.5, 12.7, 6.6], [9.5, 14.8, 9.4], cap),
        cube([6.5, 14.8, 6.6], [9.5, 15.15, 9.4], gold),
    ]
    empty = list(body)
    for start, end, inner_face in (
        ([6.8, 11.5, 6.9], [9.2, 12.7, 7.15], "south"),
        ([6.8, 11.5, 8.85], [9.2, 12.7, 9.1], "north"),
        ([6.8, 11.5, 7.15], [7.05, 12.7, 8.85], "east"),
        ([8.95, 11.5, 7.15], [9.2, 12.7, 8.85], "west"),
    ):
        wall = cube(start, end, glass)
        wall["faces"][inner_face]["uv"] = inside
        empty.append(wall)

    for name, elements in (("jagermeister_bottle", full), ("empty_jagermeister_bottle", empty)):
        write_json(ASSETS / f"models/item/{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {"bottle": "hbk:item/jagermeister_bottle", "particle": "hbk:item/jagermeister_bottle"},
            "elements": elements,
            "display": display,
        })
        write_json(ASSETS / f"items/{name}.json", {
            "model": {"type": "minecraft:model", "model": f"hbk:item/{name}"},
        })


def recipe():
    recipe_id = "hbk:empty_jagermeister_bottle"
    write_json(RESOURCES / "data/hbk/recipe/empty_jagermeister_bottle.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["# #", "# #", "###"],
        "key": {"#": "minecraft:green_stained_glass"},
        "result": {"id": recipe_id, "count": 1},
    })
    write_json(RESOURCES / "data/hbk/advancement/recipes/empty_jagermeister_bottle.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_green_glass": {
                "trigger": "minecraft:inventory_changed",
                "conditions": {"items": [{"items": "minecraft:green_stained_glass"}]},
            },
            "has_the_recipe": {
                "trigger": "minecraft:recipe_unlocked",
                "conditions": {"recipe": recipe_id},
            },
        },
        "requirements": [["has_green_glass", "has_the_recipe"]],
        "rewards": {"recipes": [recipe_id]},
    })


if __name__ == "__main__":
    texture()
    models()
    recipe()
    print("Generated full/empty Jagermeister bottle models, shared pixel atlas and seven-glass recipe")
