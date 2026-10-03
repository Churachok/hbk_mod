#!/usr/bin/env python3
"""Generate the colour atlas used by KonataModel and her pixel-art spawn egg.

Requires Pillow; all outputs are relative to the repository.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk"
PALETTE = ["#f4c8b0", "#367dac", "#559aca", "#f5f2ed", "#d63f61",
           "#f17e98", "#268c52", "#292c35", "#aa9086", "#665650", "#b83252"]


def main():
    atlas = Image.new("RGBA", (512, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(atlas)
    for index, colour in enumerate(PALETTE):
        x, y = index % 8 * 64, index // 8 * 64
        draw.rectangle((x, y, x + 63, y + 63), fill=colour)
    atlas.save(ROOT / "textures/entity/konata.png")
    # Test 2: one sleeve cuboid with painted cuffs, without stacked stripe geometry.
    # Vanilla box UV layout for width=4, height=11, depth=5, origin=(192, 64).
    test2 = atlas.copy()
    sleeve = ImageDraw.Draw(test2)
    u, v = 192, 64
    sleeve.rectangle((u, v, u + 63, v + 63), fill=PALETTE[3])
    sleeve.rectangle((u + 9, v, u + 12, v + 4), fill=PALETTE[4])  # bottom face
    sleeve.rectangle((u, v + 11, u + 17, v + 15), fill=PALETTE[4])
    for row in (v + 12, v + 14):
        sleeve.rectangle((u, row, u + 17, row), fill=PALETTE[3])
    test2.save(ROOT / "textures/entity/konata_test2.png")
    egg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(egg)
    draw.polygon([(6, 1), (9, 1), (12, 4), (14, 9), (14, 12), (11, 15),
                  (4, 15), (1, 12), (1, 9), (3, 4)], fill="#244e74")
    draw.polygon([(6, 2), (9, 2), (11, 5), (13, 10), (12, 13), (10, 14),
                  (4, 13), (2, 11), (4, 5)], fill=PALETTE[1])
    draw.rectangle((5, 3, 7, 5), fill=PALETTE[2])
    draw.rectangle((3, 8, 5, 10), fill=PALETTE[4])
    draw.rectangle((9, 6, 11, 8), fill=PALETTE[4])
    draw.rectangle((7, 11, 10, 13), fill=PALETTE[4])
    egg.save(ROOT / "textures/item/konata_spawn_egg.png")

    # Two separate white drops for the material received from Konata.
    drops = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(drops)
    draw.polygon([(5, 1), (3, 5), (3, 8), (5, 10), (7, 9), (8, 7), (7, 4)], fill="#ffffff")
    draw.line([(4, 8), (5, 9), (7, 8)], fill="#cbd8df", width=1)
    draw.point((5, 3), fill="#ffffff")
    draw.polygon([(11, 5), (9, 8), (9, 11), (11, 14), (13, 13), (14, 11), (13, 8)], fill="#ffffff")
    draw.line([(10, 11), (11, 13), (13, 12)], fill="#cbd8df", width=1)
    draw.point((11, 7), fill="#ffffff")
    drops.save(ROOT / "textures/item/shperma.png")

    # Konata's theme on the same chunky, perspective-shaped record as vanilla discs.
    disc = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(disc)
    draw.rectangle((5, 3, 9, 12), fill="#111111")
    draw.rectangle((2, 4, 12, 11), fill="#111111")
    draw.rectangle((1, 5, 13, 10), fill="#212121")
    draw.rectangle((0, 6, 14, 9), fill="#212121")
    draw.rectangle((5, 4, 9, 11), fill="#404040")
    draw.rectangle((2, 5, 12, 10), fill="#404040")
    draw.rectangle((1, 6, 13, 9), fill="#404040")
    # Sparse highlights and the darker lower rim reproduce the hand-pixelled grooves.
    draw.rectangle((2, 6, 4, 8), fill="#515151")
    draw.rectangle((10, 6, 12, 8), fill="#515151")
    draw.point((6, 5), fill="#515151")
    draw.point((8, 5), fill="#515151")
    draw.point((3, 9), fill="#333333")
    draw.point((11, 9), fill="#333333")
    draw.rectangle((1, 9, 13, 9), fill="#262626")
    draw.rectangle((2, 10, 12, 10), fill="#262626")
    draw.rectangle((5, 11, 9, 11), fill="#111111")
    # A tiny blue-and-pink paper label keeps Konata's palette without losing the disc shape.
    draw.rectangle((6, 6, 8, 8), fill="#367dac")
    draw.point((5, 7), fill="#559aca")
    draw.point((9, 7), fill="#559aca")
    draw.point((6, 7), fill="#f17e98")
    draw.point((8, 7), fill="#d63f61")
    draw.point((7, 7), fill="#262626")
    disc.save(ROOT / "textures/item/music_disc_konata_theme.png")


if __name__ == "__main__":
    main()
