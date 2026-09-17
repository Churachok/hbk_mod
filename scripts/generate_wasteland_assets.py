#!/usr/bin/env python3
"""Generate the radioactive wasteland's original 16x16 pixel textures."""

from pathlib import Path
import random

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/hbk/textures/block"
SIZE = 16


def noisy(name, base, shades, seed, accents=()):
    rng = random.Random(seed)
    image = Image.new("RGBA", (SIZE, SIZE), base)
    pixels = image.load()
    base_rgb = base[:3]
    for y in range(SIZE):
        for x in range(SIZE):
            color = rng.choice(shades) if rng.random() < 0.48 else base_rgb
            pixels[x, y] = (*color, 255)
    for color, count in accents:
        for _ in range(count):
            x, y = rng.randrange(SIZE), rng.randrange(SIZE)
            pixels[x, y] = (*color, 255)
            if rng.random() < 0.35 and x + 1 < SIZE:
                pixels[x + 1, y] = (*color, 255)
    save(image, name)
    return image


def save(image, name):
    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / f"{name}.png")


def line_texture(name, base, shades, lines, seed):
    image = noisy(name, base, shades, seed)
    draw = ImageDraw.Draw(image)
    for points, color, width in lines:
        draw.line(points, fill=(*color, 255), width=width)
    save(image, name)


def transparent_plant(name, stems, highlights=(), glow=False):
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for points, color, width in stems:
        draw.line(points, fill=(*color, 255), width=width)
    for box, color in highlights:
        draw.rectangle(box, fill=(*color, 255))
    if glow:
        for x, y in [(5, 5), (10, 4), (8, 8)]:
            draw.point((x, y), fill=(226, 255, 92, 255))
    save(image, name)


def main():
    noisy("infected_dirt", (66, 54, 36, 255), [(53, 43, 31), (83, 68, 43), (97, 78, 48)], 101,
          [((114, 146, 38), 18), ((169, 218, 44), 7)])
    noisy("radioactive_stone", (66, 68, 64, 255), [(51, 53, 51), (78, 81, 77), (90, 91, 85)], 102,
          [((113, 137, 45), 6)])
    noisy("ash_soil", (54, 48, 45, 255), [(40, 38, 37), (70, 63, 58), (82, 73, 66)], 103,
          [((105, 94, 73), 5)])
    line_texture("cracked_radioactive_stone", (70, 71, 68, 255), [(54, 55, 54), (86, 87, 82)], [
        ([(1, 0), (5, 5), (4, 10), (8, 15)], (24, 27, 24), 2),
        ([(15, 3), (10, 7), (11, 12)], (31, 35, 30), 1),
        ([(5, 5), (9, 4)], (113, 131, 46), 1),
    ], 104)
    ore = noisy("uranium_ore", (61, 65, 63, 255), [(46, 49, 49), (78, 82, 78), (91, 91, 84)], 105)
    draw = ImageDraw.Draw(ore)
    for x, y in [(3, 4), (11, 3), (7, 9), (13, 12), (2, 13)]:
        draw.polygon([(x, y - 2), (x + 2, y), (x, y + 3), (x - 1, y)], fill=(151, 222, 35, 255))
        draw.point((x, y - 1), fill=(225, 255, 91, 255))
    save(ore, "uranium_ore")
    line_texture("sediment_mud", (78, 68, 38, 255), [(64, 54, 34), (95, 83, 46), (111, 94, 51)], [
        ([(0, 5), (5, 4), (10, 6), (15, 5)], (121, 116, 53), 1),
        ([(0, 12), (6, 11), (13, 13), (15, 12)], (51, 48, 31), 1),
    ], 106)
    water = Image.new("RGBA", (SIZE, SIZE), (41, 91, 18, 210))
    draw = ImageDraw.Draw(water)
    draw.line([(0, 3), (4, 2), (8, 4), (13, 3), (15, 4)], fill=(116, 174, 28, 225))
    draw.line([(0, 10), (4, 11), (9, 9), (15, 10)], fill=(85, 145, 19, 230))
    draw.point((7, 6), fill=(184, 230, 55, 240))
    draw.point((13, 14), fill=(142, 196, 37, 240))
    save(water, "toxic_water")
    line_texture("scorched_stone", (43, 43, 42, 255), [(31, 33, 33), (55, 56, 54), (67, 64, 58)], [
        ([(0, 14), (5, 9), (8, 10), (12, 4), (15, 5)], (112, 61, 30), 1),
        ([(8, 10), (10, 15)], (74, 45, 28), 1),
    ], 107)
    noisy("radioactive_sand", (155, 142, 89, 255), [(137, 124, 76), (177, 164, 105), (194, 180, 117)], 108,
          [((154, 198, 43), 8), ((201, 231, 67), 3)])
    moss = noisy("moldy_moss", (53, 75, 29, 255), [(39, 53, 29), (70, 95, 37), (90, 111, 43)], 109)
    md = ImageDraw.Draw(moss)
    md.line([(0, 2), (4, 5), (9, 3), (15, 8)], fill=(115, 133, 48, 255))
    md.line([(2, 15), (6, 10), (11, 12), (15, 9)], fill=(29, 45, 24, 255))
    save(moss, "moldy_moss")

    transparent_plant("dead_grass", [
        ([(8, 15), (7, 7), (5, 2)], (112, 90, 47), 2),
        ([(8, 15), (10, 8), (13, 4)], (82, 69, 42), 2),
        ([(7, 13), (3, 8)], (139, 110, 51), 1),
    ])
    transparent_plant("infected_bush", [
        ([(8, 15), (8, 6), (4, 2)], (70, 61, 38), 2),
        ([(8, 10), (13, 5)], (77, 68, 40), 2),
        ([(8, 8), (3, 6)], (61, 54, 36), 1),
    ], [((3, 3, 5, 5), (105, 130, 38)), ((11, 5, 13, 7), (132, 161, 39)), ((6, 8, 8, 10), (92, 113, 34))])
    mushroom = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(mushroom)
    draw.rectangle((7, 7, 9, 15), fill=(172, 165, 104, 255))
    draw.rectangle((4, 5, 12, 8), fill=(125, 188, 34, 255))
    draw.rectangle((6, 3, 10, 6), fill=(173, 236, 55, 255))
    draw.rectangle((7, 3, 9, 4), fill=(231, 255, 116, 255))
    save(mushroom, "glowing_mushroom")

    line_texture("dry_log", (75, 57, 39, 255), [(55, 44, 34), (91, 68, 44), (110, 82, 49)], [
        ([(3, 0), (2, 15)], (42, 36, 30), 2),
        ([(10, 0), (12, 15)], (45, 37, 29), 1),
    ], 110)
    top = noisy("dry_log_top", (101, 75, 47, 255), [(82, 61, 42), (117, 88, 53)], 111)
    td = ImageDraw.Draw(top)
    td.rectangle((2, 2, 13, 13), outline=(62, 49, 37, 255))
    td.rectangle((5, 5, 10, 10), outline=(73, 54, 36, 255))
    td.line([(8, 8), (13, 4)], fill=(40, 34, 29, 255))
    save(top, "dry_log_top")
    print(f"Generated radioactive wasteland textures in {OUT}")


if __name__ == "__main__":
    main()
