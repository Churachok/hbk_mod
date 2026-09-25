#!/usr/bin/env python3
"""Generate the pink furry wolf's reproducible UV atlas and spawn egg."""
from pathlib import Path

from PIL import Image, ImageDraw

from generate_kirill_texture import paint_limb, shade, textured_rect


ROOT = Path(__file__).resolve().parents[1]
ENTITY_TEXTURE = ROOT / "src/main/resources/assets/hbk/textures/entity/pink_furry_wolf.png"
EGG_TEXTURE = ROOT / "src/main/resources/assets/hbk/textures/item/pink_furry_wolf_spawn_egg.png"

PINK = (239, 91, 157, 255)
LIGHT_PINK = (255, 157, 199, 255)
WHITE = (252, 241, 244, 255)
PANTS = (239, 224, 226, 255)
DARK = (48, 38, 49, 255)
RAINBOW = [
    (226, 46, 54, 255),
    (241, 129, 35, 255),
    (246, 211, 61, 255),
    (51, 161, 75, 255),
    (52, 99, 190, 255),
    (126, 62, 164, 255),
]
DYE_COLORS = {
    "white": (234, 236, 237, 255), "orange": (241, 118, 20, 255),
    "magenta": (189, 68, 179, 255), "light_blue": (58, 175, 217, 255),
    "yellow": (249, 198, 40, 255), "lime": (112, 185, 25, 255),
    "pink": PINK, "gray": (62, 68, 71, 255),
    "light_gray": (142, 142, 134, 255), "cyan": (21, 137, 145, 255),
    "purple": (121, 42, 172, 255), "blue": (53, 57, 157, 255),
    "brown": (114, 71, 40, 255), "green": (84, 109, 27, 255),
    "red": (161, 39, 34, 255), "black": (29, 29, 33, 255),
}


def lighter(color: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    if color == PINK:
        return LIGHT_PINK
    return tuple(round(channel + (255 - channel) * 0.34) for channel in color[:3]) + (255,)


def paint_head(image: Image.Image, fur: tuple[int, int, int, int]) -> None:
	regions = [
		((8, 0, 15, 7), fur), ((16, 0, 23, 7), shade(fur, -8)),
		((0, 8, 7, 15), shade(fur, -10)), ((8, 8, 15, 15), fur),
		((16, 8, 23, 15), shade(fur, 6)), ((24, 8, 31, 15), shade(fur, -12)),
    ]
	for seed, (box, color) in enumerate(regions, 1):
		textured_rect(image, box, color, seed)


def paint_body(image: Image.Image) -> None:
    regions = [
        (20, 16, 27, 19), (28, 16, 35, 19),
        (16, 20, 19, 31), (20, 20, 27, 31),
        (28, 20, 31, 31), (32, 20, 39, 31),
    ]
    for seed, box in enumerate(regions, 20):
        textured_rect(image, box, WHITE if seed % 2 else shade(WHITE, -6), seed)


def wolf_texture(fur: tuple[int, int, int, int] = PINK) -> Image.Image:
	image = Image.new("RGBA", (128, 128))
	light_fur = lighter(fur)
	paint_head(image, fur)
	paint_body(image)
	paint_limb(image, 40, 16, fur, WHITE, 4, 40)
	paint_limb(image, 32, 48, fur, WHITE, 4, 50)
	paint_limb(image, 0, 16, PANTS, fur, 3, 60)
	paint_limb(image, 16, 48, PANTS, fur, 3, 70)
	draw = ImageDraw.Draw(image)
	# Four broad UV swatches used by the added block geometry.
	for x, color in ((0, fur), (32, light_fur), (64, WHITE), (96, DARK)):
		textured_rect(image, (x, 64, x + 31, 95), color, 100 + x)
	# Six wide swatches keep every face of each one-pixel armband ring in one colour.
	for index, color in enumerate(RAINBOW):
		x = index * 21
		draw.rectangle((x, 96, min(127, x + 20), 127), fill=color)
	return image


def spawn_egg() -> Image.Image:
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(6, 1), (9, 1), (12, 5), (13, 10), (11, 14), (4, 14), (2, 10), (3, 5)], fill=PINK)
    draw.polygon([(6, 2), (8, 2), (5, 5), (4, 8), (3, 8), (4, 5)], fill=LIGHT_PINK)
    draw.rectangle((5, 11, 7, 13), fill=WHITE)
    draw.rectangle((10, 4, 11, 5), fill=WHITE)
    widths = [(4, 11), (3, 12), (3, 12), (3, 12), (4, 11), (5, 10)]
    for y, (color, (left, right)) in enumerate(zip(RAINBOW, widths), 5):
        draw.line((left, y, right, y), fill=color)
    draw.point((4, 4), fill=shade(LIGHT_PINK, 18))
    draw.point((11, 11), fill=shade(PINK, -22))
    return image


def main() -> None:
	ENTITY_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
	EGG_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
	wolf_texture().save(ENTITY_TEXTURE)
	for name, color in DYE_COLORS.items():
		wolf_texture(color).save(ENTITY_TEXTURE.with_name(f"pink_furry_wolf_{name}.png"))
	spawn_egg().save(EGG_TEXTURE)


if __name__ == "__main__":
    main()
