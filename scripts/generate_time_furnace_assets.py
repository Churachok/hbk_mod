#!/usr/bin/env python3
"""Reproduce the time furnace's pixel textures and cuboid models using only Python's standard library."""

import json
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk"


def write_json(path, data):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def texture(name, base, seed, kind="stone"):
    rng = random.Random(seed)
    pixels = [[tuple(max(0, min(255, c + rng.choice([-5, -2, 0, 0, 2, 4]))) for c in base)
               for x in range(32)] for y in range(32)]

    def rect(x1, y1, x2, y2, color):
        for y in range(y1, y2):
            for x in range(x1, x2):
                pixels[y][x] = color

    if kind == "mouth":
        rect(0, 0, 32, 2, (86, 83, 79))
        rect(0, 2, 2, 32, (77, 74, 70))
        rect(3, 3, 29, 29, (40, 39, 37))
        rect(5, 5, 27, 27, (13, 13, 12))
        rect(6, 25, 26, 27, (20, 19, 17))
    elif kind == "panel":
        rect(0, 0, 32, 2, (67, 66, 65))
        rect(0, 30, 32, 32, (18, 18, 19))
        digits = {"2": ["111", "001", "111", "100", "111"],
                  "0": ["111", "101", "101", "101", "111"],
                  "3": ["111", "001", "111", "001", "111"]}
        for i, digit in enumerate("2023"):
            for y, row in enumerate(digits[digit]):
                for x, value in enumerate(row):
                    if value == "1":
                        rect(1 + i * 8 + x * 2, 11 + y * 2,
                             3 + i * 8 + x * 2, 13 + y * 2, (220, 219, 214))
    elif kind == "top":
        rect(3, 3, 29, 29, (74, 72, 68))
        rect(6, 6, 26, 26, (46, 45, 43))
        rect(9, 9, 23, 23, (24, 24, 23))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    raw = b"".join(b"\0" + bytes(c for pixel in row for c in pixel) for row in pixels)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    target = ROOT / "textures/block" / f"time_furnace_{name}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(png)


def box(start, end, material="stone", front=None, top=None):
    return {"from": start, "to": end, "faces": {
        direction: {"texture": "#" + (front if direction == "north" and front else
                                     top if direction == "up" and top else material),
                    "uv": [0, 0, 16, 16]}
        for direction in ["north", "south", "east", "west", "up", "down"]}}


def main():
    texture("stone", (113, 110, 104), 2023)
    texture("metal", (76, 74, 70), 2024)
    texture("mouth", (56, 54, 51), 2025, "mouth")
    texture("panel", (39, 38, 40), 2026, "panel")
    texture("top", (100, 97, 91), 2027, "top")
    textures = {name: f"hbk:block/time_furnace_{name}"
                for name in ["stone", "metal", "mouth", "panel", "top"]}
    textures["particle"] = textures["stone"]
    body = [box([0, 0, 0], [16, 7, 16]), box([0, 14, 0], [16, 16, 16]),
            box([0, 7, 0], [3, 14, 16]), box([13, 7, 0], [16, 14, 16]),
            box([3, 7, 2], [13, 14, 16]),
            box([3, 7, -0.125], [13, 14, 1], "metal", "mouth")]
    chimney = [box([6, 0, 6], [10, 6, 10]),
               box([5, 6, 5], [11, 8, 11], "metal"),
               box([3, 8, 3], [13, 10, 13]),
               box([2, 10, 2], [14, 12, 14], top="top"),
               box([5, 1, 5.25], [11, 5, 6], "metal", "panel")]
    for name, elements in [("body", body), ("chimney", chimney)]:
        write_json(f"models/block/time_furnace_{name}.json", {
            "parent": "minecraft:block/block", "textures": textures, "elements": elements})
    variants = {}
    for facing, angle in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
        for half, model in [("lower", "body"), ("upper", "chimney")]:
            variants[f"facing={facing},half={half}"] = {"model": f"hbk:block/time_furnace_{model}", "y": angle}
    write_json("blockstates/time_furnace.json", {"variants": variants})
    item_chimney = []
    for element in chimney:
        element = {**element, "from": [element["from"][0], element["from"][1] + 16, element["from"][2]],
                   "to": [element["to"][0], element["to"][1] + 16, element["to"][2]]}
        item_chimney.append(element)
    write_json("models/item/time_furnace.json", {
        "parent": "minecraft:block/block", "textures": textures, "elements": body + item_chimney,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.43, 0.43, 0.43]},
            "ground": {"translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"translation": [0, -3, 0], "scale": [0.45, 0.45, 0.45]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0], "scale": [0.3, 0.3, 0.3]}}})
    write_json("items/time_furnace.json", {"model": {"type": "minecraft:model", "model": "hbk:item/time_furnace"}})


if __name__ == "__main__":
    main()
