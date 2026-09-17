#!/usr/bin/env python3
"""Generate Kirill's vanilla-compatible 64x64 humanoid skin."""

from pathlib import Path
import random

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/hbk/textures/entity/kirill.png"

SKIN = (239, 204, 153, 255)
HAIR = (112, 67, 43, 255)
GREEN = (24, 157, 61, 255)
BLUE = (43, 64, 190, 255)
SHOE = (190, 193, 201, 255)
BLACK = (19, 21, 24, 255)
LENS = (51, 78, 145, 255)
WHITE = (244, 242, 226, 255)


def shade(color: tuple[int, int, int, int], delta: int) -> tuple[int, int, int, int]:
    return tuple(max(0, min(255, channel + delta)) for channel in color[:3]) + (color[3],)


def textured_rect(
    image: Image.Image,
    box: tuple[int, int, int, int],
    color: tuple[int, int, int, int],
    seed: int,
) -> None:
    """Paint restrained one-pixel variation without antialiasing."""
    draw = ImageDraw.Draw(image)
    draw.rectangle(box, fill=color)
    rng = random.Random(seed)
    for y in range(box[1], box[3] + 1):
        for x in range(box[0], box[2] + 1):
            roll = rng.randrange(9)
            if roll == 0:
                image.putpixel((x, y), shade(color, 8))
            elif roll == 1:
                image.putpixel((x, y), shade(color, -8))


def paint_head(image: Image.Image) -> None:
    # Base layer: top, bottom, right, front, left, back.
    textured_rect(image, (8, 0, 15, 7), HAIR, 1)
    textured_rect(image, (16, 0, 23, 7), SKIN, 2)
    textured_rect(image, (0, 8, 7, 15), SKIN, 3)
    textured_rect(image, (8, 8, 15, 15), SKIN, 4)
    textured_rect(image, (16, 8, 23, 15), SKIN, 5)
    textured_rect(image, (24, 8, 31, 15), HAIR, 6)

    draw = ImageDraw.Draw(image)
    # Hairline on the face and temples.
    draw.rectangle((8, 8, 15, 9), fill=HAIR)
    draw.point((8, 10), fill=HAIR)
    draw.point((9, 10), fill=shade(HAIR, 8))
    draw.point((15, 10), fill=HAIR)
    draw.rectangle((0, 8, 7, 11), fill=HAIR)
    draw.rectangle((16, 8, 23, 11), fill=HAIR)

    # Black sunglasses with deep-blue lenses.
    draw.rectangle((8, 11, 15, 12), fill=BLACK)
    draw.rectangle((9, 11, 10, 12), fill=LENS)
    draw.rectangle((13, 11, 14, 12), fill=shade(LENS, 8))
    draw.point((9, 11), fill=shade(LENS, 22))
    draw.point((13, 11), fill=shade(LENS, 22))

    # Confident white smile outlined in dark pixels.
    draw.line((10, 14, 14, 14), fill=BLACK)
    draw.line((11, 15, 13, 15), fill=BLACK)
    draw.line((11, 14, 13, 14), fill=WHITE)

    # Raised outer hair layer for the chunky silhouette from the reference.
    textured_rect(image, (40, 0, 47, 7), HAIR, 7)
    draw.rectangle((32, 8, 39, 10), fill=shade(HAIR, -7))
    draw.rectangle((40, 8, 47, 9), fill=HAIR)
    draw.point((40, 10), fill=HAIR)
    draw.point((41, 10), fill=shade(HAIR, 7))
    draw.point((46, 10), fill=HAIR)
    draw.point((47, 10), fill=shade(HAIR, -7))
    draw.rectangle((48, 8, 55, 10), fill=HAIR)
    textured_rect(image, (56, 8, 63, 13), HAIR, 8)


def paint_body(image: Image.Image) -> None:
    textured_rect(image, (20, 16, 27, 19), GREEN, 10)
    textured_rect(image, (28, 16, 35, 19), shade(GREEN, -10), 11)
    textured_rect(image, (16, 20, 19, 31), shade(GREEN, -12), 12)
    textured_rect(image, (20, 20, 27, 31), GREEN, 13)
    textured_rect(image, (28, 20, 31, 31), shade(GREEN, 4), 14)
    textured_rect(image, (32, 20, 39, 31), shade(GREEN, -7), 15)

    draw = ImageDraw.Draw(image)
    draw.rectangle((23, 20, 24, 20), fill=SKIN)
    draw.point((23, 21), fill=shade(SKIN, -4))


def paint_limb(
    image: Image.Image,
    x: int,
    y: int,
    cloth: tuple[int, int, int, int],
    end: tuple[int, int, int, int],
    end_height: int,
    seed: int,
) -> None:
    # Standard classic-width limb UV: top/bottom followed by four 4x12 sides.
    textured_rect(image, (x + 4, y, x + 7, y + 3), cloth, seed)
    textured_rect(image, (x + 8, y, x + 11, y + 3), shade(cloth, -10), seed + 1)
    side_colors = (shade(cloth, -12), cloth, shade(cloth, 5), shade(cloth, -7))
    for index, side_color in enumerate(side_colors):
        side_x = x + index * 4
        textured_rect(image, (side_x, y + 4, side_x + 3, y + 15), side_color, seed + 2 + index)
        ImageDraw.Draw(image).rectangle((side_x, y + 16 - end_height, side_x + 3, y + 15), fill=end)


def make_skin() -> Image.Image:
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    paint_head(image)
    paint_body(image)
    paint_limb(image, 40, 16, GREEN, SKIN, 3, 20)  # right arm
    paint_limb(image, 32, 48, GREEN, SKIN, 3, 30)  # left arm
    paint_limb(image, 0, 16, BLUE, SHOE, 3, 40)    # right leg
    paint_limb(image, 16, 48, BLUE, SHOE, 3, 50)   # left leg
    return image


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    make_skin().save(OUTPUT)
    print("Generated", OUTPUT)


if __name__ == "__main__":
    main()
