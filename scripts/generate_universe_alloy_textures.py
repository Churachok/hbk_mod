#!/usr/bin/env python3
"""Export original alloy art to native 16px sprites. Python 3 + Pillow; no network.

Source atlas and its prompt live in art/universe_alloys. Material palettes use six
explicit tones instead of multiplying already dark vanilla netherite textures.
"""
from pathlib import Path
import colorsys
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
ART = REPO / 'art/universe_alloys'
TEXTURES = REPO / 'src/main/resources/assets/hbk/textures'

# Catalogue order: each pair has a stable, intentionally chosen material colour.
COLOURS = '''c58f56 809baa b9c6a1 829977 b49565 8ba8a1 a6aaa6 82b5aa b1aaa1
8b8fba e2ba8e b8c9a0 9ec5c9 b5c3bf b0a6c9 b6c4dc a4b9cb
91a6bc 87a5a2 788fa4 9faeb5 83939b 9792b7 9aa8b2
bdc487 99b9a1 9caec2 b3a3c5 8abcb4 c68c66
88ab9c 999dae a0b384 80aaa9 b3b896
a5c9bb 939fab a0bcb3 c4a279
96b4a9 8eabc0 a5a3bd
a299bd 9da3b5
92b4c4'''.split()
assert len(COLOURS) == 45


def colour(index):
    return tuple(bytes.fromhex(COLOURS[index]))


def palette(index):
    r, g, b = (v / 255 for v in colour(index))
    h, s, _ = colorsys.rgb_to_hsv(r, g, b)
    # Coloured shadows, readable midtones and a nearly white warm highlight.
    return [tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s * sat, value))
            for sat, value in [(0.65, .20), (.95, .33), (1.0, .48), (.9, .65), (.65, .82), (.28, .96)]]


def tile(atlas, column, row, index, opaque=False, handle=False, rows=4):
    w, h = atlas.size
    bounds = tuple(round(n) for n in (column*w/4, row*h/rows, (column+1)*w/4, (row+1)*h/rows))
    image = atlas.crop(bounds).resize((16, 16), Image.Resampling.NEAREST)
    tones = palette(index)
    pixels = []
    for r, g, b, a in image.getdata():
        if not opaque and a < 192:
            pixels.append((0, 0, 0, 0))
            continue
        if handle and r > b * 1.2 and r > g * 1.1:
            pixels.append((r, g, b, 255))
            continue
        value = (r + g + b) / 3
        shade = min(5, max(0, round((value - 30) / 43)))
        pixels.append((*tones[shade], 255))
    image.putdata(pixels)
    return image


def save(relative, image):
    path = TEXTURES / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def export(alloys):
    atlas = Image.open(ART / 'metal-atlas.png').convert('RGBA')
    gear = Image.open(ART / 'equipment-atlas.png').convert('RGBA')
    preview = Image.new('RGBA', (9*36, 5*54), (40, 40, 44, 255))
    for i, alloy in enumerate(alloys):
        name = alloy['id']
        # The four silhouettes and panel patterns distinguish both shape and material.
        column = (alloy['first'] + 2*alloy['second']) % 4
        ingot = tile(atlas, column, 0, i)
        sample = tile(atlas, (column + 1) % 4, 1, i)
        block = tile(atlas, column, 2, i, opaque=True)
        save(f'item/alloys/{name}_ingot.png', ingot)
        save(f'item/alloys/{name}_sample.png', sample)
        save(f'block/alloys/{name}_block.png', block)
        for reward in alloy['rewards']:
            role = reward['role']
            if role in ['pickaxe', 'sword', 'axe', 'shovel', 'hoe', 'helmet', 'chestplate', 'boots']:
                cell = ['pickaxe', 'sword', 'axe', 'shovel', 'hoe', 'helmet', 'chestplate', 'boots'].index(role)
                sprite = tile(gear, cell % 4, cell // 4, i, handle=True, rows=2)
                save(f'item/alloys/{reward["id"]}.png', sprite)
        px, py = i % 9 * 36, i // 9 * 54
        preview.alpha_composite(ingot, (px+2, py+1)); preview.alpha_composite(sample, (px+18, py+18))
        preview.alpha_composite(block, (px+2, py+35))
    preview.resize((1296, 1080), Image.Resampling.NEAREST).save(ART / 'palette-preview.png')


if __name__ == '__main__':
    import generate_universe_alloys
    export(generate_universe_alloys.definitions())
