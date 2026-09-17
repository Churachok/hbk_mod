#!/usr/bin/env python3
"""Generate Liza's vanilla-compatible 64x64 humanoid skin."""

from pathlib import Path
import random

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/hbk/textures/entity/liza.png"

SKIN = (246, 215, 169, 255)
HAIR = (246, 207, 18, 255)
BLACK = (24, 25, 27, 255)
EYE_OUTLINE = (30, 38, 46, 255)
EYE_BLUE = (132, 194, 235, 255)
EYE_LIGHT = (231, 247, 252, 255)


def shade(color: tuple[int, int, int, int], delta: int) -> tuple[int, int, int, int]:
    return tuple(max(0, min(255, channel + delta)) for channel in color[:3]) + (color[3],)


def textured_rect(
    image: Image.Image,
    box: tuple[int, int, int, int],
    color: tuple[int, int, int, int],
    seed: int,
) -> None:
    draw = ImageDraw.Draw(image)
    draw.rectangle(box, fill=color)
    rng = random.Random(seed)
    for y in range(box[1], box[3] + 1):
        for x in range(box[0], box[2] + 1):
            roll = rng.randrange(10)
            if roll == 0:
                image.putpixel((x, y), shade(color, 7))
            elif roll == 1:
                image.putpixel((x, y), shade(color, -7))


def paint_head(image: Image.Image) -> None:
    # Base layer: top, bottom, right, front, left, back.
    textured_rect(image, (8, 0, 15, 7), HAIR, 1)
    textured_rect(image, (16, 0, 23, 7), SKIN, 2)
    textured_rect(image, (0, 8, 7, 15), SKIN, 3)
    textured_rect(image, (8, 8, 15, 15), SKIN, 4)
    textured_rect(image, (16, 8, 23, 15), SKIN, 5)
    textured_rect(image, (24, 8, 31, 15), HAIR, 6)

    draw = ImageDraw.Draw(image)
    # Asymmetrical bob and side-swept fringe.
    draw.rectangle((8, 8, 15, 9), fill=HAIR)
    draw.line((8, 10, 12, 10), fill=HAIR)
    draw.line((8, 11, 10, 11), fill=shade(HAIR, -5))
    draw.point((8, 12), fill=HAIR)
    draw.point((15, 10), fill=HAIR)
    draw.rectangle((0, 8, 7, 13), fill=HAIR)
    draw.rectangle((16, 8, 23, 12), fill=HAIR)
    draw.rectangle((22, 13, 23, 14), fill=shade(HAIR, -8))

    # Large pale-blue eyes with white glints.
    draw.rectangle((9, 11, 10, 12), fill=EYE_OUTLINE)
    draw.rectangle((13, 11, 14, 12), fill=EYE_OUTLINE)
    draw.point((9, 11), fill=EYE_LIGHT)
    draw.point((10, 12), fill=EYE_BLUE)
    draw.point((13, 11), fill=EYE_LIGHT)
    draw.point((14, 12), fill=EYE_BLUE)

    draw.line((11, 14, 13, 14), fill=EYE_OUTLINE)
    draw.point((10, 13), fill=shade(SKIN, -8))
    draw.point((14, 13), fill=shade(SKIN, -8))

    # Raised second layer gives the haircut its chunky vanilla silhouette.
    textured_rect(image, (40, 0, 47, 7), HAIR, 7)
    draw.rectangle((32, 8, 39, 13), fill=shade(HAIR, -6))
    draw.rectangle((40, 8, 47, 9), fill=HAIR)
    draw.line((40, 10, 44, 10), fill=HAIR)
    draw.line((40, 11, 42, 11), fill=shade(HAIR, -5))
    draw.point((40, 12), fill=HAIR)
    draw.point((47, 10), fill=HAIR)
    draw.rectangle((48, 8, 55, 12), fill=HAIR)
    draw.rectangle((54, 13, 55, 14), fill=shade(HAIR, -8))
    textured_rect(image, (56, 8, 63, 14), HAIR, 8)


def paint_body(image: Image.Image) -> None:
    textured_rect(image, (20, 16, 27, 19), BLACK, 10)
    textured_rect(image, (28, 16, 35, 19), shade(BLACK, -4), 11)
    textured_rect(image, (16, 20, 19, 31), shade(BLACK, -5), 12)
    textured_rect(image, (20, 20, 27, 31), BLACK, 13)
    textured_rect(image, (28, 20, 31, 31), shade(BLACK, 4), 14)
    textured_rect(image, (32, 20, 39, 31), shade(BLACK, -3), 15)

    draw = ImageDraw.Draw(image)
    draw.rectangle((23, 20, 24, 20), fill=SKIN)
    draw.point((23, 21), fill=shade(SKIN, -5))


def paint_limb(
    image: Image.Image,
    x: int,
    y: int,
    cloth: tuple[int, int, int, int],
    end: tuple[int, int, int, int],
    end_height: int,
    seed: int,
) -> None:
    textured_rect(image, (x + 4, y, x + 7, y + 3), cloth, seed)
    textured_rect(image, (x + 8, y, x + 11, y + 3), shade(cloth, -4), seed + 1)
    side_colors = (shade(cloth, -5), cloth, shade(cloth, 4), shade(cloth, -3))
    for index, side_color in enumerate(side_colors):
        side_x = x + index * 4
        textured_rect(image, (side_x, y + 4, side_x + 3, y + 15), side_color, seed + 2 + index)
        if end_height:
            ImageDraw.Draw(image).rectangle((side_x, y + 16 - end_height, side_x + 3, y + 15), fill=end)


def make_skin() -> Image.Image:
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    paint_head(image)
    paint_body(image)
    paint_limb(image, 40, 16, BLACK, SKIN, 3, 20)   # right arm
    paint_limb(image, 32, 48, BLACK, SKIN, 3, 30)   # left arm
    paint_limb(image, 0, 16, BLACK, BLACK, 0, 40)   # right leg
    paint_limb(image, 16, 48, BLACK, BLACK, 0, 50)  # left leg
    return image


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    make_skin().save(OUTPUT)
    print("Generated", OUTPUT)


if __name__ == "__main__":
    main()
