#!/usr/bin/env python3
"""Create the mod's uranium-235 item sprites and worn-equipment textures.

The palette and crystalline accents are based on the generated uranium armour
concept kept in art_concepts/uranium_armor_reference.png.
"""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/hbk/textures"
ITEMS = ASSETS / "item"
HUMANOID = ASSETS / "entity/equipment/humanoid"
LEGGINGS = ASSETS / "entity/equipment/humanoid_leggings"
T = (0, 0, 0, 0)
OUTLINE = "#0b1010"
METAL_DARK = "#1a2424"
METAL = "#435151"
STEEL = "#889b90"
URANIUM = "#66d91a"
GLOW = "#caff4b"
WHITE = "#edffd0"

# Alpha silhouettes of the vanilla humanoid equipment UV layout. Keeping the
# shape in data makes this generator self-contained while every visible pixel,
# plate and radioactive seam below is original artwork.
HUMANOID_MASK = (
    "00ff000000000000", "00ff000000000000", "00ff000000000000", "00ff000000000000",
    "00ff000000000000", "00ff000000000000", "00ff000000000000", "00ff000000000000",
    "ffffffff00000000", "ffffffff00000000", "ffffffff00000000", "ff99ffff00000000",
    "f399cfff00000000", "018180ff00000000", "01c3803c00000000", "01c3800000000000",
    "00f00000000f0000", "00f00000000f0000", "00f00000000f0000", "00f00000000f0000",
    "0000fc3fffffff00", "0000fc3fffffff00", "0000fe7fffffff00", "0000ffffffffff00",
    "0000ffffffffff00", "0000ffffffff9f00", "1f80ffffff6a0400", "ffffffffff000000",
    "ffffffffff000000", "ffff0ff0ff000000", "ffff07e07e000000", "ffff03c03c000000",
)
LEGGINGS_MASK = (
    "0000000000000000", "0000000000000000", "0000000000000000", "0000000000000000",
    "0000000000000000", "0000000000000000", "0000000000000000", "0000000000000000",
    "0000000000000000", "0000000000000000", "0000000000000000", "0000000000000000",
    "0000000000000000", "0000000000000000", "0000000000000000", "0000000000000000",
    "0f00000000000000", "0f00000000000000", "0f00000000000000", "0f00000000000000",
    "ffff000000000000", "ffff000000000000", "ffff000000000000", "ffff000000000000",
    "ffff000000000000", "ffff000000000000", "ffff000000000000", "ffffc3c3ff000000",
    "ffffffffff000000", "0000ffffff000000", "0000ffffff000000", "0000ffffff000000",
)


def save(image: Image.Image, folder: Path, name: str) -> None:
    folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f"{name}.png")


def item_canvas() -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGBA", (16, 16), T)
    return image, ImageDraw.Draw(image)


def uranium_235() -> Image.Image:
    image, d = item_canvas()
    d.polygon([(2, 7), (7, 3), (14, 5), (11, 12), (4, 13)], fill=OUTLINE)
    d.polygon([(3, 7), (7, 4), (13, 6), (10, 11), (4, 12)], fill=METAL_DARK)
    d.polygon([(5, 6), (8, 4), (12, 6), (9, 8)], fill=METAL)
    d.line([(4, 10), (7, 7), (10, 10), (12, 7)], fill=URANIUM, width=2)
    d.line([(7, 7), (8, 5)], fill=GLOW)
    d.point((8, 5), fill=WHITE)
    return image


def helmet() -> Image.Image:
    image, d = item_canvas()
    d.polygon([(2, 4), (5, 1), (11, 1), (14, 4), (13, 13), (10, 15), (6, 15), (3, 13)], fill=OUTLINE)
    d.polygon([(3, 5), (5, 2), (11, 2), (13, 5), (12, 12), (10, 14), (6, 14), (4, 12)], fill=METAL_DARK)
    d.rectangle((5, 4, 10, 9), fill=METAL)
    d.rectangle((7, 5, 8, 10), fill=URANIUM)
    d.point((7, 5), fill=WHITE)
    d.line([(3, 8), (5, 7), (5, 12)], fill=GLOW)
    d.line([(12, 8), (10, 7), (10, 12)], fill=GLOW)
    return image


def chestplate() -> Image.Image:
    image, d = item_canvas()
    d.polygon([(2, 3), (5, 1), (7, 3), (9, 3), (11, 1), (14, 3), (13, 13), (10, 15), (6, 15), (3, 13)], fill=OUTLINE)
    d.polygon([(3, 4), (5, 2), (7, 4), (9, 4), (11, 2), (13, 4), (12, 12), (10, 14), (6, 14), (4, 12)], fill=METAL_DARK)
    d.polygon([(5, 5), (7, 6), (8, 12), (6, 11)], fill=METAL)
    d.polygon([(11, 5), (9, 6), (8, 12), (10, 11)], fill=METAL)
    d.line([(4, 5), (6, 7), (8, 10), (10, 7), (12, 5)], fill=URANIUM, width=1)
    d.line([(6, 13), (8, 11), (10, 13)], fill=GLOW)
    d.point((8, 10), fill=WHITE)
    return image


def leggings() -> Image.Image:
    image, d = item_canvas()
    d.rectangle((3, 2, 12, 14), fill=OUTLINE)
    d.rectangle((4, 3, 11, 9), fill=METAL_DARK)
    d.rectangle((4, 10, 6, 14), fill=METAL_DARK)
    d.rectangle((9, 10, 11, 14), fill=METAL_DARK)
    d.rectangle((5, 4, 10, 6), fill=METAL)
    d.line([(4, 5), (6, 7), (5, 12)], fill=URANIUM)
    d.line([(11, 5), (9, 7), (10, 12)], fill=GLOW)
    d.point((5, 11), fill=WHITE)
    return image


def boots() -> Image.Image:
    image, d = item_canvas()
    for x in (2, 9):
        d.rectangle((x, 3, x + 4, 13), fill=OUTLINE)
        d.rectangle((x - 1, 12, x + 5, 14), fill=OUTLINE)
        d.rectangle((x + 1, 4, x + 3, 11), fill=METAL_DARK)
        d.rectangle((x, 12, x + 4, 13), fill=METAL)
        d.line([(x + 1, 6), (x + 3, 9), (x + 2, 11)], fill=URANIUM)
        d.point((x + 2, 7), fill=GLOW)
    return image


def mask_pixel(mask: tuple[str, ...], x: int, y: int) -> bool:
    return bool(int(mask[y], 16) & (1 << (63 - x)))


def is_radioactive_seam(x: int, y: int, leggings_layer: bool) -> bool:
    if leggings_layer:
        # Waist reactor band and asymmetric glowing channels down both legs.
        return (
            (y in (21, 22) and 0 <= x <= 15)
            or (20 <= y <= 31 and x in (3 + (y % 3), 10 - (y % 3)))
            or (27 <= y <= 31 and x in (20, 27, 36, 43))
        )
    # Helmet brow/crest, chest reactor, shoulder rings and boot channels.
    return (
        (y in (9, 10) and 7 <= x <= 23)
        or (2 <= y <= 7 and x in (11, 12))
        or (20 <= y <= 31 and x in (22, 23, 24, 25) and abs(x - 23.5) <= (31 - y) / 2 + 1)
        or (y in (21, 22) and 40 <= x <= 55)
        or (25 <= y <= 31 and x in (42 + y % 2, 52 - y % 2))
        or (27 <= y <= 31 and x in (3, 12, 27, 36))
    )


def armour_layer(size: tuple[int, int], leggings_layer: bool) -> Image.Image:
    image = Image.new("RGBA", size, T)
    mask = LEGGINGS_MASK if leggings_layer else HUMANOID_MASK
    pixels = image.load()
    metal_palette = ((18, 25, 27, 255), (28, 38, 40, 255), (43, 55, 56, 255), (65, 78, 76, 255))
    for y in range(32):
        for x in range(64):
            if not mask_pixel(mask, x, y):
                continue
            edge = not all(
                0 <= nx < 64 and 0 <= ny < 32 and mask_pixel(mask, nx, ny)
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))
            )
            if is_radioactive_seam(x, y, leggings_layer):
                pixels[x, y] = (218, 255, 101, 255) if (x + y) % 5 == 0 else (91, 235, 22, 255)
            elif edge:
                pixels[x, y] = (8, 13, 14, 255)
            else:
                pixels[x, y] = metal_palette[(x * 3 + y * 5) % len(metal_palette)]
    return image


def main() -> None:
    for name, factory in {
        "uranium_235": uranium_235,
        "uranium_helmet": helmet,
        "uranium_chestplate": chestplate,
        "uranium_leggings": leggings,
        "uranium_boots": boots,
    }.items():
        save(factory(), ITEMS, name)
    save(armour_layer((64, 32), False), HUMANOID, "uranium")
    save(armour_layer((64, 32), True), LEGGINGS, "uranium")
    print("Generated uranium-235 armour textures")


if __name__ == "__main__":
    main()
