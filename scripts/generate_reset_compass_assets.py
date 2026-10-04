"""Recolor Minecraft's compass needle and generate the 32 matching item models.

Run after Gradle has downloaded the project's Minecraft version. The source jar is
found in this repository's Loom cache; no image libraries or personal paths needed.
"""

import copy
import json
from pathlib import Path
import struct
import zipfile
import zlib


ROOT = Path(__file__).resolve().parents[1]
version = next(line.split("=", 1)[1].strip() for line in (ROOT / "gradle.properties").read_text().splitlines()
               if line.startswith("minecraft_version="))
jars = sorted((ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft").glob(
    f"minecraft-clientOnly-*/{version}/minecraft-clientOnly-*-{version}.jar"))
if not jars:
    raise SystemExit("Minecraft client assets missing: run ./gradlew build first")

assets = ROOT / "src/main/resources/assets/hbk"
texture_dir = assets / "textures/item"
model_dir = assets / "models/item"
item_dir = assets / "items"


def green_needle(png):
    """Change only the red compass palette entries; preserve all original pixels."""
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("Expected PNG")
    output = bytearray(png[:8])
    offset = 8
    changed = False
    while offset < len(png):
        length = struct.unpack_from(">I", png, offset)[0]
        tag = png[offset + 4:offset + 8]
        data = bytearray(png[offset + 8:offset + 8 + length])
        offset += 12 + length
        if tag == b"PLTE":
            for index in range(0, len(data), 3):
                red, green, blue = data[index:index + 3]
                if red > 150 and red > 2 * green and red > 2 * blue:
                    data[index:index + 3] = bytes((green, red, blue))
                    changed = True
        output += struct.pack(">I", len(data)) + tag + data
        output += struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    if not changed:
        raise ValueError("Compass frame had no red needle palette")
    return bytes(output)


with zipfile.ZipFile(jars[0]) as minecraft:
    vanilla_item = json.loads(minecraft.read("assets/minecraft/items/compass.json"))
    model = copy.deepcopy(vanilla_item["model"]["on_true"])
    for entry in model["entries"]:
        name = entry["model"]["model"].rsplit("/", 1)[1]
        entry["model"]["model"] = f"hbk:item/reset_{name}"
    (item_dir / "reset_compass.json").write_text(
        json.dumps({"model": model}, indent=2) + "\n")

    for frame in range(32):
        suffix = f"{frame:02d}"
        name = f"reset_compass_{suffix}"
        source = f"assets/minecraft/textures/item/compass_{suffix}.png"
        (texture_dir / f"{name}.png").write_bytes(green_needle(minecraft.read(source)))
        (model_dir / f"{name}.json").write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"hbk:item/{name}"},
        }, indent=2) + "\n")
