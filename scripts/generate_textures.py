#!/usr/bin/env python3
"""Original pixel-art textures for the hbk Fabric mod."""

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hbk"

SKIN = (224, 184, 150, 255)
SKIN_SHADOW = (196, 150, 118, 255)
HAIR = (58, 42, 28, 255)
MUSTACHE = (20, 14, 10, 255)
EYE = (36, 24, 16, 255)
EYE_WHITE = (236, 228, 214, 255)
CAP = (22, 22, 22, 255)
CAP_BAND = (139, 30, 30, 255)
STAR = (212, 160, 23, 255)
OLIVE = (74, 92, 50, 255)
OLIVE_DARK = (52, 68, 36, 255)
RED = (139, 30, 30, 255)
GOLD = (201, 162, 39, 255)
BELT = (43, 36, 24, 255)
PANTS = (61, 58, 50, 255)
PANTS_DARK = (42, 40, 35, 255)
BOOT = (18, 18, 18, 255)
EGG_SHELL = (110, 92, 48, 255)
EGG_HIGHLIGHT = (150, 126, 72, 255)
EGG_SPOT = (36, 32, 24, 255)


def fill(img: Image.Image, x: int, y: int, w: int, h: int, color: tuple[int, int, int, int]) -> None:
    draw = ImageDraw.Draw(img)
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def pixel(img: Image.Image, x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def paint_head(skin: Image.Image) -> None:
    fill(skin, 0, 8, 8, 8, SKIN_SHADOW)
    fill(skin, 8, 8, 8, 8, SKIN)
    fill(skin, 16, 8, 8, 8, SKIN)
    fill(skin, 24, 8, 8, 8, SKIN_SHADOW)
    fill(skin, 8, 0, 8, 8, HAIR)
    fill(skin, 16, 0, 8, 8, SKIN)

    fill(skin, 8, 8, 8, 2, HAIR)
    pixel(skin, 8, 10, HAIR)
    pixel(skin, 15, 10, HAIR)

    pixel(skin, 10, 12, EYE_WHITE)
    pixel(skin, 13, 12, EYE_WHITE)
    pixel(skin, 10, 13, EYE)
    pixel(skin, 13, 13, EYE)
    pixel(skin, 11, 14, SKIN_SHADOW)
    pixel(skin, 12, 14, SKIN_SHADOW)
    fill(skin, 9, 15, 6, 1, MUSTACHE)
    pixel(skin, 8, 15, MUSTACHE)
    pixel(skin, 15, 15, MUSTACHE)

    fill(skin, 0, 8, 8, 2, HAIR)
    fill(skin, 16, 8, 8, 2, HAIR)
    fill(skin, 24, 8, 8, 3, HAIR)

    # Hat overlay only — leave unused overlay pixels transparent.
    fill(skin, 32, 8, 8, 4, CAP)
    fill(skin, 40, 8, 8, 4, CAP)
    fill(skin, 48, 8, 8, 4, CAP)
    fill(skin, 56, 8, 8, 4, CAP)
    fill(skin, 40, 0, 8, 8, CAP)
    fill(skin, 48, 0, 8, 8, CAP)
    fill(skin, 40, 11, 8, 1, CAP_BAND)
    fill(skin, 43, 9, 2, 2, STAR)
    pixel(skin, 42, 10, STAR)
    pixel(skin, 45, 10, STAR)


def paint_body(skin: Image.Image) -> None:
    fill(skin, 16, 20, 4, 12, OLIVE_DARK)
    fill(skin, 20, 20, 8, 12, OLIVE)
    fill(skin, 28, 20, 4, 12, OLIVE)
    fill(skin, 32, 20, 8, 12, OLIVE_DARK)
    fill(skin, 20, 16, 8, 4, OLIVE)
    fill(skin, 28, 16, 8, 4, OLIVE_DARK)
    fill(skin, 20, 20, 8, 2, RED)
    fill(skin, 16, 20, 4, 2, RED)
    fill(skin, 28, 20, 4, 2, RED)
    pixel(skin, 22, 23, GOLD)
    pixel(skin, 25, 23, GOLD)
    pixel(skin, 22, 26, GOLD)
    pixel(skin, 25, 26, GOLD)
    fill(skin, 20, 29, 8, 2, BELT)
    pixel(skin, 23, 29, GOLD)
    pixel(skin, 24, 30, GOLD)


def paint_arm(skin: Image.Image, x: int, y: int) -> None:
    fill(skin, x, y, 16, 4, OLIVE)
    fill(skin, x, y + 4, 4, 12, OLIVE_DARK)
    fill(skin, x + 4, y + 4, 4, 12, OLIVE)
    fill(skin, x + 8, y + 4, 4, 12, OLIVE)
    fill(skin, x + 12, y + 4, 4, 12, OLIVE_DARK)
    fill(skin, x + 4, y + 13, 8, 3, SKIN)


def paint_leg(skin: Image.Image, x: int, y: int) -> None:
    fill(skin, x, y, 16, 4, PANTS)
    fill(skin, x, y + 4, 4, 12, PANTS_DARK)
    fill(skin, x + 4, y + 4, 4, 12, PANTS)
    fill(skin, x + 8, y + 4, 4, 12, PANTS)
    fill(skin, x + 12, y + 4, 4, 12, PANTS_DARK)
    fill(skin, x + 4, y + 12, 8, 4, BOOT)


def make_stalin_skin() -> Image.Image:
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    paint_head(skin)
    paint_body(skin)
    paint_arm(skin, 40, 16)
    paint_arm(skin, 32, 48)
    paint_leg(skin, 0, 16)
    paint_leg(skin, 16, 48)
    return skin


def make_spawn_egg() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    fill(img, 5, 1, 6, 1, EGG_HIGHLIGHT)
    fill(img, 4, 2, 8, 12, EGG_SHELL)
    fill(img, 3, 4, 10, 8, EGG_SHELL)
    fill(img, 5, 2, 6, 2, EGG_HIGHLIGHT)
    fill(img, 6, 5, 4, 3, CAP)
    fill(img, 6, 7, 4, 1, CAP_BAND)
    pixel(img, 7, 6, STAR)
    pixel(img, 8, 6, STAR)
    fill(img, 6, 9, 4, 2, SKIN)
    fill(img, 6, 10, 4, 1, MUSTACHE)
    pixel(img, 4, 5, EGG_SPOT)
    pixel(img, 11, 8, EGG_SPOT)
    pixel(img, 5, 12, EGG_SPOT)
    return img


def make_icon() -> Image.Image:
    icon = Image.new("RGBA", (64, 64), (48, 62, 34, 255))
    draw = ImageDraw.Draw(icon)
    draw.rectangle([8, 6, 55, 28], fill=CAP)
    draw.rectangle([8, 24, 55, 30], fill=CAP_BAND)
    draw.rectangle([28, 12, 35, 19], fill=STAR)
    draw.rectangle([10, 31, 53, 57], fill=SKIN)
    draw.rectangle([16, 36, 22, 42], fill=EYE)
    draw.rectangle([41, 36, 47, 42], fill=EYE)
    draw.rectangle([12, 48, 51, 55], fill=MUSTACHE)
    return icon


def main() -> None:
    entity_dir = ASSETS / "textures" / "entity"
    item_dir = ASSETS / "textures" / "item"
    entity_dir.mkdir(parents=True, exist_ok=True)
    item_dir.mkdir(parents=True, exist_ok=True)

    make_stalin_skin().save(entity_dir / "stalin.png")
    make_spawn_egg().save(item_dir / "stalin_spawn_egg.png")
    make_icon().save(ASSETS / "icon.png")
    print("Wrote textures to", ASSETS)


if __name__ == "__main__":
    main()
