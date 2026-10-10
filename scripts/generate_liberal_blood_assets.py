#!/usr/bin/env python3
"""Recolor only vanilla water pixels and draw the white-blue-white effect icon.

Requires Pillow. Pass --minecraft-jar if the local Loom client archive is absent.
The archive is only an asset source, not a shipped dependency.
"""

import argparse
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/hbk/textures"
WATER_TO_BLOOD = {
    (35, 79, 204, 255): (204, 35, 45, 255),
    (46, 88, 211, 255): (211, 46, 53, 255),
    (52, 95, 218, 255): (218, 52, 58, 255),
    (68, 111, 233, 255): (233, 68, 69, 255),
    (90, 130, 243, 255): (243, 90, 80, 255),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", type=Path)
    args = parser.parse_args()
    candidates = sorted((ROOT / ".gradle/loom-cache/minecraftMaven").glob(
        "net/minecraft/minecraft-clientOnly-*/26.2/*.jar"))
    archive = args.minecraft_jar or (candidates[0] if candidates else None)
    if archive is None:
        parser.error("Pass the Minecraft 26.2 client archive with --minecraft-jar")
    with ZipFile(archive) as jar:
        original = Image.open(BytesIO(jar.read("assets/minecraft/textures/item/water_bucket.png"))).convert("RGBA")
    bucket = original.copy()
    for y in range(bucket.height):
        for x in range(bucket.width):
            pixel = original.getpixel((x, y))
            bucket.putpixel((x, y), WATER_TO_BLOOD.get(pixel, pixel))
    (ASSETS / "item").mkdir(parents=True, exist_ok=True)
    bucket.save(ASSETS / "item/liberal_blood_bucket.png")
    flag = Image.new("RGBA", (18, 18))
    draw = ImageDraw.Draw(flag)
    draw.rectangle((1, 3, 16, 14), fill="#ffffff")
    draw.rectangle((1, 7, 16, 10), fill="#3088f0")
    (ASSETS / "mob_effect").mkdir(parents=True, exist_ok=True)
    flag.save(ASSETS / "mob_effect/young_liberal.png")
    print("Generated exact vanilla bucket recolor and white-blue-white flag")


if __name__ == "__main__":
    main()
