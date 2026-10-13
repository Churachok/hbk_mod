#!/usr/bin/env python3
"""Draw HBK kitchen pixel art and build head/vision assets from repository sources.

Requires Pillow. Run from any directory; no external references are used.
"""

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk"


def save(image, group, name):
    folder = ASSETS / "textures" / group
    folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f"{name}.png")


def onigiri():
    image = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(image)
    d.polygon([(7, 1), (9, 1), (15, 12), (14, 14), (2, 14), (0, 12)], fill="#595f57")
    d.polygon([(7, 2), (8, 2), (14, 12), (13, 13), (2, 13), (1, 12)], fill="#d1d3bd")
    d.polygon([(7, 2), (8, 2), (12, 10), (10, 12), (2, 12), (2, 10)], fill="#f5f2df")
    d.line((4, 8, 7, 3), fill="#ffffff")
    for p in [(5, 8), (8, 5), (10, 8), (3, 11), (11, 11), (7, 7)]:
        d.point(p, fill="#dbdec9")
    d.rectangle((6, 9, 10, 14), fill="#182a24")
    d.rectangle((7, 9, 9, 13), fill="#31483a")
    d.line((7, 10, 7, 12), fill="#536046")
    d.point((9, 12), fill="#101f1c")
    return image


def kettle(hot=False, noodles=False):
    image = Image.new("RGBA", (32, 32))
    d = ImageDraw.Draw(image)
    dark, rim, steel, shine = "#28353f", "#566873", "#9bafb9", "#deebdf"
    if noodles:
        # Only the enamel palette changes; all geometry and tea/steam stay identical.
        rim, steel, shine = "#863a40", "#ca5459", "#f5c5bf"
    # Enamel handle, spout, wide body, lid and little red knob.
    d.arc((6, 4, 25, 25), 190, 350, fill=dark, width=3)
    d.arc((8, 6, 23, 24), 195, 345, fill=rim, width=1)
    d.polygon([(8, 18), (3, 16), (1, 11), (0, 11), (0, 18), (5, 23), (10, 24)], fill=dark)
    d.polygon([(8, 19), (4, 18), (2, 14), (2, 18), (6, 22), (10, 23)], fill=steel)
    d.ellipse((7, 13, 28, 29), fill=dark)
    d.ellipse((8, 14, 27, 27), fill=steel)
    d.rectangle((10, 16, 25, 25), fill=steel)
    d.arc((8, 14, 27, 27), 0, 100, fill=rim, width=3)
    d.rectangle((11, 17, 13, 23), fill=shine)
    d.point((14, 16), fill=shine)
    d.ellipse((9, 12, 26, 17), fill=dark)
    d.ellipse((10, 12, 25, 15), fill=shine)
    d.rectangle((16, 10, 19, 12), fill="#753838")
    d.line((16, 10, 18, 10), fill="#c76a4d")
    d.line((11, 28, 24, 28), fill=rim)
    if hot:
        d.line([(12, 9), (11, 7), (13, 5), (12, 2)], fill="#f4e8cb", width=1)
        d.line([(22, 9), (21, 6), (23, 4), (22, 1)], fill="#e3d1b3", width=1)
        d.rectangle((17, 19, 24, 23), fill="#79543a")
        d.rectangle((18, 20, 23, 22), fill="#c89a56")
    else:
        d.rectangle((17, 19, 24, 23), fill="#40585a")
        d.rectangle((18, 20, 23, 22), fill="#8bc3b4")
    return image


def head():
    # Alexander's existing unarmoured Sasha skin, standard humanoid head UVs.
    skin = Image.open(ASSETS / "textures/entity/sasha.png").convert("RGBA")
    atlas = skin.crop((0, 0, 32, 16))
    save(atlas, "entity", "lebedev_head")
    faces = {"up": (8, 0), "down": (16, 0), "east": (0, 8),
             "north": (8, 8), "west": (16, 8), "south": (24, 8)}
    for name, (x, y) in faces.items():
        save(atlas.crop((x, y, x + 8, y + 8)), "item", f"lebedev_head_{name}")


def vision():
    # Append brightness to existing post chains to preserve their priority and colours.
    folder = ASSETS / "post_effect"
    passes = [
        {"vertex_shader": "minecraft:core/screenquad", "fragment_shader": "hbk:post/onigiri",
         "inputs": [{"sampler_name": "Scene", "target": "minecraft:main"}], "output": "onigiri_swap"},
        {"vertex_shader": "minecraft:core/screenquad", "fragment_shader": "minecraft:post/blit",
         "inputs": [{"sampler_name": "In", "target": "onigiri_swap"}],
         "uniforms": {"BlitConfig": [{"name": "ColorModulate", "type": "vec4", "value": [1, 1, 1, 1]}]},
         "output": "minecraft:main"},
    ]
    for base in (None, "currant_inversion", "goshas_rage", "kirill_glasses"):
        chain = json.loads((folder / f"{base}.json").read_text()) if base else {"targets": {}, "passes": []}
        chain["targets"]["onigiri_swap"] = {}
        chain["passes"].extend(passes)
        name = f"{base}_onigiri" if base else "onigiri"
        (folder / f"{name}.json").write_text(json.dumps(chain, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--noodle-kettles-only", action="store_true",
                        help="Only rebuild the two red variants, leaving existing assets untouched")
    args = parser.parse_args()
    save(kettle(noodles=True), "item", "doshirak_kettle")
    save(kettle(hot=True, noodles=True), "item", "hard_doshirak_kettle")
    if args.noodle_kettles_only:
        print("Generated red kettles with the original 32x32 silhouettes")
        return
    rice = onigiri()
    save(rice, "item", "onigiri")
    icon = Image.new("RGBA", (18, 18))
    icon.paste(rice, (1, 1))
    save(icon, "mob_effect", "onigiri")
    save(kettle(), "item", "kirill_kettle")
    save(kettle(True), "item", "hard_kirill_kettle")
    head()
    vision()
    print("Generated onigiri, kettles, Lebedev head and compatible brightness chains")


if __name__ == "__main__":
    main()
