"""Generate the hand-built, two-storey Stalin dacha structure without external packages."""

from __future__ import annotations

import gzip
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/data/hbk/structure/stalin_dacha.nbt"
SIZE = (41, 20, 45)
blocks: dict[tuple[int, int, int], tuple[str, tuple[tuple[str, str], ...]]] = {}


def put(x, y, z, name, **props):
    blocks[(x, y, z)] = ("minecraft:" + name, tuple(sorted((k, str(v).lower()) for k, v in props.items())))


def box(x1, y1, z1, x2, y2, z2, name, **props):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                put(x, y, z, name, **props)


def lantern(x, y, z):
    put(x, y, z, "lantern", hanging="false", waterlogged="false")


# Raised stone platform, shallow lakeside basin, pier and garden.
box(1, 0, 1, 39, 1, 33, "stone_bricks")
box(2, 2, 2, 38, 2, 32, "smooth_stone")
box(4, 2, 26, 36, 2, 32, "stone_bricks")
box(9, 0, 33, 31, 0, 44, "stone_bricks")
box(10, 1, 34, 30, 1, 43, "water", level="0")
for x in range(9, 32):
    for z in (33, 44):
        put(x, 1, z, "stone_bricks")
for z in range(33, 45):
    for x in (9, 31):
        put(x, 1, z, "stone_bricks")
box(17, 2, 33, 23, 2, 44, "spruce_planks")
for x in (17, 23):
    for z in (35, 39, 43):
        put(x, 1, z, "spruce_log", axis="y")
        put(x, 3, z, "oak_fence")
        if z in (35, 43):
            lantern(x, 4, z)
for x in range(4, 37, 4):
    for z in (2, 31):
        box(x, 3, z, x, 3, z, "stone_brick_wall")
        lantern(x, 4, z)
for x in (3, 37):
    for z in range(4, 31, 5):
        box(x, 3, z, x, 4, z, "oak_leaves", persistent="true", distance="1", waterlogged="false")
for x in (8, 12, 28, 32):
    box(x, 3, 29, x, 4, 29, "oak_leaves", persistent="true", distance="1", waterlogged="false")
for x in (6, 11, 29, 34):
    box(x, 3, 27, x, 5, 27, "polished_andesite")
    lantern(x, 6, 27)

# Main building: dark-green facade, log cornices, stone base, two furnished floors.
box(6, 2, 5, 34, 3, 25, "stone_bricks")
box(7, 4, 6, 33, 4, 24, "dark_oak_planks")
box(7, 5, 6, 33, 13, 24, "air")
for y in (9, 14):
    box(6, y, 5, 34, y, 25, "dark_oak_planks")
for x in range(6, 35):
    for z in (5, 25):
        box(x, 4, z, x, 13, z, "dark_prismarine")
for z in range(5, 26):
    for x in (6, 34):
        box(x, 4, z, x, 13, z, "dark_prismarine")
for y in (4, 8, 9, 13):
    for x in range(6, 35):
        for z in (5, 25):
            put(x, y, z, "spruce_planks")
    for z in range(5, 26):
        for x in (6, 34):
            put(x, y, z, "spruce_planks")
for x in (6, 14, 25, 34):
    for z in (5, 16, 25):
        box(x, 4, z, x, 13, z, "stripped_dark_oak_log", axis="y")
for y in (6, 7, 11, 12):
    for x in (9, 12, 18, 22, 28, 31):
        put(x, y, 5, "glass")
        if x not in (18, 22):
            put(x, y, 25, "glass")
    for z in (9, 12, 19, 22):
        put(6, y, z, "glass")
        put(34, y, z, "glass")

# Wide porch and balcony supported by columns.
box(15, 3, 25, 25, 3, 29, "polished_andesite")
box(14, 9, 25, 26, 9, 28, "spruce_planks")
for x in (15, 20, 25):
    box(x, 4, 28, x, 8, 28, "polished_andesite")
for x in range(14, 27):
    put(x, 10, 28, "spruce_fence")
for z in (26, 27):
    put(14, 10, z, "spruce_fence")
    put(26, 10, z, "spruce_fence")
for x in (19, 20):
    for y in (4, 5):
        put(x, y, 25, "air")
    put(x, 4, 25, "dark_oak_door", facing="south", half="lower", hinge="left" if x == 19 else "right", open="false", powered="false")
    put(x, 5, 25, "dark_oak_door", facing="south", half="upper", hinge="left" if x == 19 else "right", open="false", powered="false")
    lantern(x, 6, 28)

# First floor central hall, office, dining room, kitchen and two lounges.
for x in (15, 25):
    box(x, 5, 7, x, 8, 23, "spruce_planks")
    for z in (11, 20):
        box(x, 5, z, x, 6, z, "air")
for x1, x2 in ((7, 14), (26, 33)):
    box(x1, 5, 16, x2, 8, 16, "spruce_planks")
    box((x1 + x2) // 2, 5, 16, (x1 + x2) // 2, 6, 16, "air")
for x, z in ((8, 8), (13, 13), (8, 20), (13, 22), (27, 8), (32, 13), (27, 20), (32, 22)):
    put(x, 5, z, "bookshelf")
    put(x, 6, z, "bookshelf")
for x in range(18, 23):
    put(x, 5, 10, "dark_oak_stairs", facing="south", half="bottom", shape="straight", waterlogged="false")
for x, z in ((10, 10), (11, 10), (29, 10), (30, 10), (10, 20), (29, 20)):
    put(x, 5, z, "spruce_slab", type="bottom", waterlogged="false")
for x in (10, 20, 30):
    for z in (10, 20):
        lantern(x, 8, z)
for x, z in ((8, 14), (12, 21), (29, 14), (31, 21)):
    put(x, 5, z, "chest", facing="south", type="single", waterlogged="false")

# A corner stairwell, with a landing opening in the upper floor.
for step in range(5):
    x = 28 + step
    put(x, 5 + step, 23, "spruce_stairs", facing="east", half="bottom", shape="straight", waterlogged="false")
    for y in range(10, 14):
        put(x, y, 23, "air")
put(31, 9, 23, "air")

# Second-floor bedrooms, corridor, cabinet and storage.
for x in (15, 25):
    box(x, 10, 7, x, 13, 23, "spruce_planks")
    for z in (11, 20):
        box(x, 10, z, x, 11, z, "air")
for x1, x2 in ((7, 14), (26, 33)):
    box(x1, 10, 16, x2, 13, 16, "spruce_planks")
    box((x1 + x2) // 2, 10, 16, (x1 + x2) // 2, 11, 16, "air")
for x, z in ((10, 10), (29, 10), (10, 20), (29, 20)):
    box(x, 10, z, x + 1, 10, z + 2, "red_carpet")
for x, z in ((8, 9), (13, 13), (8, 20), (13, 22), (27, 9), (32, 13), (27, 20), (32, 22)):
    put(x, 10, z, "bookshelf")
for x, z in ((8, 13), (12, 20), (28, 13), (31, 21)):
    put(x, 10, z, "chest", facing="south", type="single", waterlogged="false")
for x in (10, 20, 30):
    for z in (10, 20):
        lantern(x, 13, z)

# Tiered dark roof, eaves and two masonry chimneys.
for ring in range(5):
    x1, x2 = 4 + ring, 36 - ring
    z1, z2 = 3 + ring, 27 - ring
    box(x1, 14 + ring, z1, x2, 14 + ring, z2, "deepslate_tiles")
box(15, 18, 11, 25, 18, 19, "deepslate_tiles")
for x, z in ((11, 10), (29, 18)):
    box(x, 16, z, x + 1, 18, z + 1, "stone_bricks")
    box(x, 19, z, x + 1, 19, z + 1, "campfire", facing="north", lit="true", signal_fire="false", waterlogged="false")


def named(tag, name, payload):
    return bytes((tag,)) + struct.pack(">H", len(name.encode())) + name.encode() + payload


def string(value):
    b = value.encode()
    return struct.pack(">H", len(b)) + b


def compound(values):
    return b"".join(named(tag, key, payload) for key, tag, payload in values) + b"\x00"


def list_tag(tag, values):
    return bytes((tag,)) + struct.pack(">i", len(values)) + b"".join(values)


def ints(values):
    return list_tag(3, [struct.pack(">i", value) for value in values])


palette = sorted(set(blocks.values()))
indices = {state: index for index, state in enumerate(palette)}
palette_entries = []
for name, props in palette:
    entries = [("Name", 8, string(name))]
    if props:
        entries.append(("Properties", 10, compound([(key, 8, string(value)) for key, value in props])))
    palette_entries.append(compound(entries))

block_entries = []
for pos, state in sorted(blocks.items()):
    block_entries.append(compound([
        ("state", 3, struct.pack(">i", indices[state])),
        ("pos", 9, ints(pos)),
    ]))

root = compound([
    ("size", 9, ints(SIZE)),
    ("blocks", 9, list_tag(10, block_entries)),
    ("palette", 9, list_tag(10, palette_entries)),
    ("entities", 9, list_tag(10, [])),
    ("DataVersion", 3, struct.pack(">i", 4903)),
])
OUT.parent.mkdir(parents=True, exist_ok=True)
with gzip.open(OUT, "wb", compresslevel=9) as target:
    target.write(named(10, "", root))
print(f"{OUT}: {len(blocks)} blocks, {len(palette)} states")
