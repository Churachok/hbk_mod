#!/usr/bin/env python3
"""Generate improved boats from the exact vanilla plank and blast-furnace textures."""

import argparse
import json
import shutil
import struct
import subprocess
import tempfile
import zipfile
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/hbk"
DATA = ROOT / "src/main/resources/data/hbk"

WOODS = {
    "oak": "minecraft:oak_boat",
    "spruce": "minecraft:spruce_boat",
    "birch": "minecraft:birch_boat",
    "jungle": "minecraft:jungle_boat",
    "acacia": "minecraft:acacia_boat",
    "cherry": "minecraft:cherry_boat",
    "dark_oak": "minecraft:dark_oak_boat",
    "pale_oak": "minecraft:pale_oak_boat",
    "mangrove": "minecraft:mangrove_boat",
    "bamboo": "minecraft:bamboo_raft",
}


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


class PixelImage:
    """Tiny dependency-free PNG writer used only for the item silhouette mask."""

    def __init__(self, width: int, height: int) -> None:
        self.width = width
        self.height = height
        self.pixels = [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]

    def polygon(self, points: list[tuple[int, int]]) -> None:
        min_x = min(point[0] for point in points)
        max_x = max(point[0] for point in points)
        min_y = min(point[1] for point in points)
        max_y = max(point[1] for point in points)
        for y in range(min_y, max_y + 1):
            for x in range(min_x, max_x + 1):
                inside = False
                previous = points[-1]
                for current in points:
                    x1, y1 = previous
                    x2, y2 = current
                    if (y1 > y) != (y2 > y):
                        crossing = (x2 - x1) * (y - y1) / (y2 - y1) + x1
                        if x < crossing:
                            inside = not inside
                    previous = current
                if inside and 0 <= x < self.width and 0 <= y < self.height:
                    self.pixels[y][x] = (255, 255, 255, 255)

    def save(self, path: Path) -> None:
        def chunk(kind: bytes, data: bytes) -> bytes:
            payload = kind + data
            return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)

        raw = bytearray()
        for row in self.pixels:
            raw.append(0)
            for color in row:
                raw.extend(color)
        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        png += chunk(b"IEND", b"")
        path.write_bytes(png)


def find_minecraft_jar(explicit: str | None) -> Path:
    if explicit is not None:
        path = Path(explicit).resolve()
        if not path.is_file():
            raise SystemExit(f"Minecraft client JAR not found: {path}")
        return path
    candidates = sorted((ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft").glob(
        "minecraft-clientOnly-*/26.2/minecraft-clientOnly-*-26.2.jar"
    ))
    if not candidates:
        raise SystemExit("Run Gradle once or pass --minecraft-jar PATH")
    return candidates[-1]


def extract_texture(archive: zipfile.ZipFile, name: str, destination: Path) -> None:
    archive_name = f"assets/minecraft/textures/block/{name}.png"
    destination.write_bytes(archive.read(archive_name))


def run_magick(executable: str, *arguments: object) -> None:
    subprocess.run([executable, *(str(argument) for argument in arguments)], check=True)


def make_entity_texture(
    executable: str,
    planks: Path,
    furnace_top: Path,
    furnace_side: Path,
    furnace_front: Path,
    output: Path,
    work: Path,
) -> None:
    base = work / "base.png"
    stack = work / "stack.png"
    run_magick(executable, "-size", "128x128", f"tile:{planks}", base)
    run_magick(executable, "-size", "64x32", f"tile:{furnace_side}", stack)
    # A 16-cube's vanilla UV layout: two top faces, then four side faces.
    run_magick(
        executable,
        base,
        stack, "-geometry", "+64+96", "-composite",
        furnace_top, "-geometry", "+80+64", "-composite",
        furnace_top, "-geometry", "+96+64", "-composite",
        furnace_side, "-geometry", "+64+80", "-composite",
        furnace_front, "-geometry", "+80+80", "-composite",
        furnace_side, "-geometry", "+96+80", "-composite",
        furnace_side, "-geometry", "+112+80", "-composite",
        output,
    )


def make_item_texture(
    executable: str,
    planks: Path,
    furnace_side: Path,
    furnace_front: Path,
    output: Path,
    work: Path,
) -> None:
    mask = work / "item_mask.png"
    hull = work / "item_hull.png"
    furnace = work / "item_furnace.png"
    chimney = work / "item_chimney.png"
    silhouette = PixelImage(16, 16)
    silhouette.polygon([(0, 8), (2, 13), (12, 14), (15, 10), (15, 8)])
    silhouette.save(mask)
    run_magick(executable, planks, mask, "-alpha", "off", "-compose", "CopyOpacity", "-composite", hull)
    run_magick(executable, furnace_front, "-filter", "point", "-resize", "5x6!", furnace)
    run_magick(executable, furnace_side, "-filter", "point", "-resize", "2x4!", chimney)
    run_magick(
        executable,
        "-size", "16x16", "canvas:none",
        hull, "-geometry", "+0+0", "-composite",
        furnace, "-geometry", "+2+4", "-composite",
        chimney, "-geometry", "+3+1", "-composite",
        output,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--minecraft-jar", help="Minecraft client JAR containing vanilla textures")
    args = parser.parse_args()
    minecraft_jar = find_minecraft_jar(args.minecraft_jar)
    executable = shutil.which("magick") or shutil.which("convert")
    if executable is None:
        raise SystemExit("ImageMagick is required")

    entity_dir = ASSETS / "textures/entity/improved_boat"
    item_dir = ASSETS / "textures/item"
    entity_dir.mkdir(parents=True, exist_ok=True)
    item_dir.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix="hbk-improved-boats-") as temporary, zipfile.ZipFile(minecraft_jar) as archive:
        temporary_dir = Path(temporary)
        furnace_top = temporary_dir / "blast_furnace_top.png"
        furnace_side = temporary_dir / "blast_furnace_side.png"
        furnace_front = temporary_dir / "blast_furnace_front.png"
        extract_texture(archive, "blast_furnace_top", furnace_top)
        extract_texture(archive, "blast_furnace_side", furnace_side)
        extract_texture(archive, "blast_furnace_front", furnace_front)

        for wood, vanilla_boat in WOODS.items():
            suffix = "raft" if wood == "bamboo" else "boat"
            name = f"improved_{wood}_{suffix}"
            planks = temporary_dir / f"{wood}_planks.png"
            extract_texture(archive, f"{wood}_planks", planks)
            variant_work = temporary_dir / wood
            variant_work.mkdir()
            make_entity_texture(executable, planks, furnace_top, furnace_side, furnace_front,
                                entity_dir / f"{wood}.png", variant_work)
            make_item_texture(executable, planks, furnace_side, furnace_front,
                              item_dir / f"{name}.png", variant_work)

            write_json(ASSETS / "models/item" / f"{name}.json", {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"hbk:item/{name}"},
            })
            write_json(ASSETS / "items" / f"{name}.json", {
                "model": {"type": "minecraft:model", "model": f"hbk:item/{name}"},
            })
            write_json(DATA / "recipe" / f"{name}.json", {
                "type": "minecraft:crafting_shapeless",
                "category": "misc",
                "group": "hbk:improved_boats",
                "ingredients": [vanilla_boat, "minecraft:blast_furnace"],
                "result": {"id": f"hbk:{name}"},
            })

    print(f"Generated {len(WOODS)} improved boats from vanilla block textures in {minecraft_jar.name}")


if __name__ == "__main__":
    main()
