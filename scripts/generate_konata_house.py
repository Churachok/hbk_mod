#!/usr/bin/env python3
"""Generate the small Konata house structure without external dependencies."""

import gzip
import struct
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/data/hbk/structure/konata_house.nbt"


def text(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def named(tag: int, name: str, payload: bytes) -> bytes:
    return bytes((tag,)) + text(name) + payload


def compound(entries: list[bytes]) -> bytes:
    return b"".join(entries) + b"\0"


def string_tag(name: str, value: str) -> bytes:
    return named(8, name, text(value))


def int_tag(name: str, value: int) -> bytes:
    return named(3, name, struct.pack(">i", value))


def list_tag(name: str, element_type: int, values: list[bytes]) -> bytes:
    return named(9, name, bytes((element_type,)) + struct.pack(">i", len(values)) + b"".join(values))


def palette_entry(name: str, properties: dict[str, str] | None = None) -> bytes:
    entries = [string_tag("Name", name)]
    if properties:
        entries.append(named(10, "Properties", compound([
            string_tag(key, value) for key, value in properties.items()
        ])))
    return compound(entries)


def main() -> None:
    size = (9, 7, 9)
    palette = [
        ("minecraft:air", None),
        ("minecraft:stone_bricks", None),
        ("minecraft:cherry_planks", None),
        ("minecraft:glass", None),
        ("minecraft:spruce_planks", None),
        ("minecraft:pink_carpet", None),
        ("minecraft:bookshelf", None),
        ("minecraft:crafting_table", None),
        ("minecraft:glowstone", None),
        ("minecraft:pink_bed", {"facing": "east", "occupied": "false", "part": "foot"}),
        ("minecraft:pink_bed", {"facing": "east", "occupied": "false", "part": "head"}),
        ("minecraft:cherry_door", {"facing": "south", "half": "lower", "hinge": "left", "open": "false", "powered": "false"}),
        ("minecraft:cherry_door", {"facing": "south", "half": "upper", "hinge": "left", "open": "false", "powered": "false"}),
    ]
    blocks = {(x, y, z): 0 for x in range(size[0]) for y in range(size[1]) for z in range(size[2])}

    # Stone foundation and warm cherry floor.
    for x in range(1, 8):
        for z in range(1, 8):
            blocks[x, 0, z] = 1 if x in (1, 7) or z in (1, 7) else 2
    for x in range(3, 6):
        blocks[x, 0, 0] = 2

    # Three-block-high walls with an open front door and full-block windows.
    for y in range(1, 4):
        for x in range(1, 8):
            blocks[x, y, 1] = 2
            blocks[x, y, 7] = 2
        for z in range(1, 8):
            blocks[1, y, z] = 2
            blocks[7, y, z] = 2
    blocks[4, 1, 1] = 11
    blocks[4, 2, 1] = 12
    for pos in ((2, 2, 1), (6, 2, 1), (3, 2, 7), (4, 2, 7), (5, 2, 7),
                (1, 2, 3), (1, 2, 5), (7, 2, 3), (7, 2, 5)):
        blocks[pos] = 3

    # Compact stepped dark roof with a lit centre tile.
    for x in range(9):
        for z in range(9):
            blocks[x, 4, z] = 4
    blocks[4, 4, 4] = 8
    for x in range(1, 8):
        for z in range(1, 8):
            blocks[x, 5, z] = 4
    for x in range(2, 7):
        for z in range(2, 7):
            blocks[x, 6, z] = 4

    # Pink carpet and a small lived-in interior.
    for x in range(2, 7):
        for z in range(2, 6):
            blocks[x, 1, z] = 5
    blocks[2, 1, 6] = 6
    blocks[2, 2, 6] = 6
    blocks[6, 1, 6] = 7
    blocks[3, 1, 5] = 9
    blocks[4, 1, 5] = 10

    block_entries = []
    for (x, y, z), state in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        block_entries.append(compound([
            list_tag("pos", 3, [struct.pack(">i", value) for value in (x, y, z)]),
            int_tag("state", state),
        ]))

    root = compound([
        list_tag("size", 3, [struct.pack(">i", value) for value in size]),
        list_tag("blocks", 10, block_entries),
        list_tag("palette", 10, [palette_entry(name, properties) for name, properties in palette]),
        list_tag("entities", 10, []),
        int_tag("DataVersion", 4903),
    ])
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as compressed:
            compressed.write(bytes((10,)) + text("") + root)


if __name__ == "__main__":
    main()
