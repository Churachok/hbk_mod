#!/usr/bin/env python3
"""Generate original pixel textures for the Stalinka content update."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/hbk/textures"
TRANSPARENT = (0, 0, 0, 0)


def canvas(size=(32, 32), color=TRANSPARENT):
    image = Image.new("RGBA", size, color)
    return image, ImageDraw.Draw(image)


def sickle_and_hammer():
    source = ROOT / "art_concepts/new_update/ussr_hammer_and_sickle_source.png"
    emblem = Image.open(source).convert("RGBA")
    bounds = emblem.getchannel("A").getbbox()
    emblem = emblem.crop(bounds)
    emblem.thumbnail((28, 28), Image.Resampling.LANCZOS)

    image = Image.new("RGBA", (32, 32), TRANSPARENT)
    image.alpha_composite(emblem, ((32 - emblem.width) // 2, (32 - emblem.height) // 2))
    pixels = image.load()
    for y in range(32):
        for x in range(32):
            pixels[x, y] = (255, 215, 0, 255) if pixels[x, y][3] >= 96 else TRANSPARENT
    return image


def condensed_milk():
    image, draw = canvas()
    outline, metal, light = "#373942", "#9ca4ae", "#edf1f2"
    red, cream, shadow = "#c52b32", "#ffe0a0", "#c28d4b"
    draw.ellipse((6, 8, 25, 14), fill=outline)
    draw.rectangle((6, 11, 25, 27), fill=outline)
    draw.ellipse((6, 23, 25, 29), fill=outline)
    draw.rectangle((8, 12, 23, 25), fill=metal)
    draw.ellipse((8, 22, 23, 27), fill=metal)
    draw.rectangle((8, 16, 23, 20), fill=red)
    draw.rectangle((8, 18, 23, 19), fill="#f7e4bd")
    draw.line([(9, 13), (9, 24)], fill=light, width=2)
    draw.ellipse((7, 8, 24, 14), fill=metal)
    draw.ellipse((9, 10, 22, 14), fill=cream)
    draw.polygon([(15, 9), (20, 2), (28, 5), (24, 13), (21, 11), (24, 6), (20, 5), (18, 11)], fill=outline)
    draw.polygon([(17, 9), (21, 3), (27, 5), (23, 11), (21, 10), (24, 6), (21, 5), (19, 10)], fill=metal)
    draw.line([(21, 3), (27, 5)], fill=light)
    draw.polygon([(14, 12), (19, 12), (20, 18), (18, 21), (17, 15), (14, 14)], fill=cream)
    draw.point((18, 20), fill=shadow)
    return image


def sweet_life():
    image, draw = canvas((18, 18))
    draw.ellipse((2, 2, 15, 15), fill="#d69b36")
    draw.ellipse((4, 4, 13, 13), fill="#ffe09a")
    draw.arc((5, 5, 12, 12), 15, 165, fill="#70401e", width=1)
    draw.point((6, 8), fill="#70401e")
    draw.point((11, 8), fill="#70401e")
    draw.line([(1, 13), (5, 11)], fill="#8ed56f", width=2)
    draw.line([(13, 5), (17, 3)], fill="#8ed56f", width=2)
    return image


def drowsiness():
    image, draw = canvas((18, 18))
    draw.polygon([(3, 2), (12, 2), (7, 8), (14, 8), (8, 15), (2, 15), (8, 9), (2, 9), (7, 3), (3, 3)], fill="#7a6e9b")
    draw.line([(4, 3), (10, 3), (5, 9)], fill="#c9bdea")
    draw.point((13, 3), fill="#efe9ff")
    return image


def diabetes():
    image, draw = canvas((18, 18))
    draw.ellipse((3, 2, 14, 15), fill="#7c2630")
    draw.ellipse((5, 4, 12, 13), fill="#c95355")
    draw.polygon([(9, 1), (14, 7), (12, 10), (8, 7), (6, 4)], fill="#f0c58d")
    draw.line([(4, 14), (14, 3)], fill="#f4e1bd", width=2)
    return image


def flying_carpet_entity():
    image, draw = canvas((128, 32), "#781d2a")
    draw.rectangle((0, 0, 127, 31), outline="#e7b543", width=2)
    for x in range(4, 128, 8):
        draw.line([(x, 3), (x + 4, 15), (x, 28)], fill="#c98d36", width=2)
    for x in range(0, 128, 16):
        draw.polygon([(x + 4, 16), (x + 8, 10), (x + 12, 16), (x + 8, 22)], fill="#e4c66b")
        draw.polygon([(x + 6, 16), (x + 8, 13), (x + 10, 16), (x + 8, 19)], fill="#315b69")
    draw.line((0, 6, 127, 6), fill="#f2d47a")
    draw.line((0, 25, 127, 25), fill="#f2d47a")
    return image


def save(image, group, name):
    destination = ASSETS / group
    destination.mkdir(parents=True, exist_ok=True)
    image.save(destination / f"{name}.png")


def main():
    save(sickle_and_hammer(), "item", "sickle_and_hammer")
    save(condensed_milk(), "item", "condensed_milk")
    save(sweet_life(), "mob_effect", "sweet_life")
    save(drowsiness(), "mob_effect", "drowsiness")
    save(diabetes(), "mob_effect", "diabetes")
    save(flying_carpet_entity(), "entity", "flying_carpet")
    print("Generated Stalinka update textures")


if __name__ == "__main__":
    main()
