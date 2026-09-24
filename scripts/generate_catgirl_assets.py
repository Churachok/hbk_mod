#!/usr/bin/env python3
"""Generate the rebuilt catgirl atlas and spawn egg from the final Models concept."""
from pathlib import Path

from PIL import Image, ImageDraw

from generate_kirill_texture import paint_limb, shade, textured_rect


ROOT = Path(__file__).resolve().parents[1]
ENTITY = ROOT / "src/main/resources/assets/hbk/textures/entity/catgirl.png"
EGG = ROOT / "src/main/resources/assets/hbk/textures/item/catgirl_spawn_egg.png"

HAIR = (246, 196, 174, 255)
HAIR_LIGHT = (255, 220, 201, 255)
HAIR_SHADOW = (222, 162, 148, 255)
SKIN = (249, 188, 166, 255)
INNER_EAR = (235, 137, 145, 255)
WHITE = (243, 238, 236, 255)
EYE_DARK = (40, 37, 55, 255)
RED = (190, 53, 73, 255)
SKIRT = (68, 53, 60, 255)
NAVY = (48, 57, 83, 255)
SOCK = (59, 47, 54, 255)
SHOE = (48, 38, 43, 255)


def paint_vanilla_uv(image: Image.Image) -> None:
	# Head UV: top/bottom/right/front/left/back.
	for seed, box in enumerate(((8, 0, 15, 7), (16, 0, 23, 7), (0, 8, 7, 15),
			(8, 8, 15, 15), (16, 8, 23, 15), (24, 8, 31, 15)), 1):
		textured_rect(image, box, SKIN, seed)
	draw = ImageDraw.Draw(image)
	draw.rectangle((8, 8, 15, 9), fill=HAIR)
	draw.rectangle((0, 8, 7, 11), fill=HAIR_SHADOW)
	draw.rectangle((16, 8, 23, 11), fill=HAIR_SHADOW)
	draw.rectangle((24, 8, 31, 15), fill=HAIR)
	draw.rectangle((9, 11, 10, 12), fill=WHITE)
	draw.rectangle((13, 11, 14, 12), fill=WHITE)
	draw.point((10, 12), fill=EYE_DARK)
	draw.point((13, 12), fill=EYE_DARK)

	# White blouse and skin arms; bare thighs underneath the skirt.
	for seed, box in enumerate(((20, 16, 27, 19), (28, 16, 35, 19), (16, 20, 19, 31),
			(20, 20, 27, 31), (28, 20, 31, 31), (32, 20, 39, 31)), 20):
		textured_rect(image, box, WHITE, seed)
	paint_limb(image, 40, 16, WHITE, SKIN, 7, 40)
	paint_limb(image, 32, 48, WHITE, SKIN, 7, 50)
	paint_limb(image, 0, 16, SKIN, SOCK, 6, 60)
	paint_limb(image, 16, 48, SKIN, SOCK, 6, 70)


def catgirl_texture() -> Image.Image:
	image = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
	paint_vanilla_uv(image)

	# Large dedicated swatches prevent neighbouring model parts from bleeding colours.
	for x, color in ((0, HAIR), (32, HAIR_SHADOW), (64, HAIR_LIGHT), (96, HAIR)):
		textured_rect(image, (x, 64, x + 31, 95), color, 100 + x)
	for x, color in ((0, INNER_EAR), (32, SKIN), (64, WHITE), (80, RED), (96, EYE_DARK)):
		width = 15 if x >= 64 else 31
		if x == 96:
			width = 31
		textured_rect(image, (x, 96, x + width, 127), color, 200 + x)
	textured_rect(image, (0, 128, 127, 159), WHITE, 300)
	textured_rect(image, (0, 160, 63, 191), RED, 400)
	textured_rect(image, (64, 160, 127, 191), SKIRT, 464)
	textured_rect(image, (0, 192, 127, 223), NAVY, 500)
	textured_rect(image, (0, 224, 63, 255), SOCK, 600)
	textured_rect(image, (64, 224, 127, 255), SHOE, 664)

	# Highlights echo the soft stepped shading of the concept without smoothing pixels.
	draw = ImageDraw.Draw(image)
	draw.rectangle((68, 99, 72, 104), fill=(255, 255, 255, 255))
	draw.rectangle((76, 99, 79, 104), fill=shade(WHITE, -22))
	draw.rectangle((84, 99, 90, 101), fill=shade(RED, 22))
	draw.rectangle((6, 165, 20, 167), fill=shade(RED, 18))
	draw.rectangle((72, 166, 124, 168), fill=shade(SKIRT, 11))
	draw.rectangle((8, 198, 120, 201), fill=shade(NAVY, 16))
	draw.rectangle((8, 216, 120, 219), fill=shade(NAVY, -12))
	return image


def spawn_egg() -> Image.Image:
	image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	draw = ImageDraw.Draw(image)
	# A large jagged curl reads as a fluffy tail even at the native 16x16 resolution.
	draw.polygon([(9, 7), (12, 6), (14, 7), (13, 9), (15, 10), (14, 12), (15, 13),
			(13, 15), (10, 14), (9, 12), (11, 11), (11, 9)], fill=HAIR_SHADOW)
	draw.polygon([(10, 8), (12, 7), (13, 8), (12, 10), (14, 11), (13, 13),
			(11, 13), (10, 12), (12, 11), (11, 9)], fill=HAIR_LIGHT)
	# The egg itself carries no face, bow or clothing marks: only the two ears remain.
	draw.polygon([(4, 1), (6, 3), (8, 3), (10, 1), (11, 5), (12, 8), (11, 12),
			(9, 15), (4, 15), (2, 12), (1, 8), (2, 5)], fill=HAIR)
	draw.polygon([(4, 2), (5, 4), (3, 5)], fill=INNER_EAR)
	draw.polygon([(10, 2), (9, 4), (11, 5)], fill=INNER_EAR)
	return image


def main() -> None:
	ENTITY.parent.mkdir(parents=True, exist_ok=True)
	EGG.parent.mkdir(parents=True, exist_ok=True)
	catgirl_texture().save(ENTITY)
	spawn_egg().save(EGG)


if __name__ == "__main__":
	main()
