#!/usr/bin/env python3
"""Original seamless 16px rocks and resources. Standard library only; no external inputs.

Run from any directory. Coordinates wrap at 16; all motif fields are periodic.
The four variants alter grain, cracks, veins and contrast, not just the filename.
"""
from pathlib import Path
import json
import math
import random
import re
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
A = ROOT / 'assets/hbk'
D = ROOT / 'data/hbk'
FAMILIES = ['primordial', 'celestial', 'columnar', 'cavernous', 'woven',
            'sedimentary', 'labyrinthine', 'fragmented', 'inverse', 'stratified']
RU = ['Первичный', 'Небесный', 'Столбчатый', 'Пещерный', 'Волокнистый',
      'Осадочный', 'Лабиринтный', 'Осколочный', 'Инверсный', 'Пластовый']
PALETTES = [(112, 111, 88), (157, 176, 173), (74, 86, 88), (143, 132, 94),
            (113, 81, 75), (88, 112, 109), (101, 115, 139), (145, 108, 103),
            (116, 119, 146), (140, 113, 83)]
ACCENTS = [(190, 154, 88), (122, 214, 235), (141, 127, 95), (229, 210, 154),
           (179, 188, 113), (209, 153, 85), (121, 163, 126), (176, 145, 202),
           (169, 151, 214), (184, 194, 202)]
ROLES = ['stone', 'surface', 'deep', 'formation', 'ore']
ROCK_NAMES_RU = [
    ['Первичный камень', 'Рыхлая порода', 'Глубинный сланец', 'Минеральные прожилки', 'Первичная руда'],
    ['Небесный камень', 'Пористая порода', 'Плотный небесный камень', 'Светящиеся жилы', 'Руда лёгкого кристалла'],
    ['Колонный базальт', 'Расколотый базальт', 'Ребристый камень', 'Тяжёлый минерал', 'Базальтовая руда'],
    ['Пещерный камень', 'Кальцитовая порода', 'Плотный известняк', 'Натёчные образования', 'Известняковая руда'],
    ['Волокнистый камень', 'Полосатая порода', 'Плотная волокнистая порода', 'Сплетённый минерал', 'Волокнистая руда'],
    ['Осадочный камень', 'Уплотнённый песок', 'Глубинная осадочная порода', 'Эродированная порода', 'Осадочная руда'],
    ['Лабиринтный сланец', 'Трещиноватая порода', 'Плотный камень', 'Лабиринтный минерал', 'Сланцевая руда'],
    ['Осколочный камень', 'Кристаллическая крошка', 'Плотная осколочная порода', 'Кристаллический обломок', 'Нестабильный минерал'],
    ['Инверсный камень', 'Висячая порода', 'Плотный инверсный камень', 'Плотный инверсный минерал', 'Инверсная руда'],
    ['Пластовый камень', 'Межслойная порода', 'Глубинный пластовый камень', 'Слоистый кристалл', 'Межслойный минерал'],
]


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def write_language(path, value):
    # Preserve unrelated hand-written translations and their formatting.
    old = {m.group(2): m.group(0) for m in re.finditer(r'(?m)^([ \t]*)"([^"\n]+)":.*$', path.read_text())} if path.exists() else {}
    lines = ['{']
    for index, (key, text) in enumerate(value.items()):
        line = old.get(key)
        if line is None or json.loads('{' + line.rstrip().rstrip(',') + '}')[key] != text:
            line = '\t' + json.dumps(key, ensure_ascii=False) + ': ' + json.dumps(text, ensure_ascii=False)
        lines.append(line.rstrip().rstrip(',') + (',' if index < len(value)-1 else ''))
    path.write_text('\n'.join(lines + ['}']) + '\n')


def png(path, pixels):
    path.parent.mkdir(parents=True, exist_ok=True)
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    raw = b''.join(b'\x00' + bytes(c for pixel in row for c in pixel) for row in pixels)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def rock(f, r, variant, top=False):
    # Short clustered strokes, discrete shade steps and restrained family motifs.
    # Avoid the old continuous sine stripes, which read as corrugated plastic on cliffs.
    rng = random.Random(17029 + f * 1000 + variant * 11 + top)
    grain = [[rng.randrange(-2, 3) for _ in range(16)] for _ in range(16)]
    clusters = [[rng.randrange(-3, 4) for _ in range(8)] for _ in range(8)]
    cracks = [(rng.randrange(16), rng.randrange(16), rng.randrange(2, 5)) for _ in range(6)]
    ore_rng = random.Random(8149 + f * 301 + variant * 19 + top)
    ore = [(ore_rng.randrange(16), ore_rng.randrange(16)) for _ in range(7)]
    out = []
    for y in range(16):
        row = []
        for x in range(16):
            coarse = clusters[(y // 2) % 8][(x // 2) % 8]
            motif = 0
            for cx, cy, length in cracks:
                dx, dy = (x-cx) % 16, (y-cy) % 16
                if f == 2 and not top:
                    hit = dy < length and dx == (dy // 3)
                elif f in (5, 9) and not top:
                    hit = dx < length and dy == (dx // 4)
                else:
                    hit = dx < length and dy == dx // 2
                if hit: motif -= 9
                elif dx < length and dy == (dx // 2 + 1): motif += 4
            if f == 1 and coarse == -3: motif -= 5  # tiny chalk pores
            if f == 7 and coarse == 3: motif += 5   # small crystal chips
            shade = coarse * 3 + grain[y][x] * 2 + motif + [0, 7, -12, 0, 0][r]
            # Quantised, visibly pixel-art palette. Role variants share the same host grain.
            shade = round(shade / 3) * 3
            colour = [max(0, min(255, c + shade)) for c in PALETTES[f]]
            if r in (3, 4):
                mineral = any((x-cx) % 16 < 2 and (y-cy) % 16 < (2 if r == 4 else 1) for cx, cy in ore)
                if mineral:
                    colour = [max(0, min(255, c + grain[y][x] * 4 - (8 if (x+y) % 3 == 0 else 0))) for c in ACCENTS[f]]
            row.append(tuple(colour) + (255,))
        out.append(row)
    return out


def item(f, role):
    out = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(2, 14):
        for x in range(2, 14):
            dx, dy = x-7, y-7
            inside = abs(dx) + abs(dy) < 7 if role != 1 else abs(dx + dy*.25) < (y-1)*.35 and y < 14
            if role == 2:
                inside = 13 < dx*dx + dy*dy < 42 or abs(dx) <= 1 and abs(dy) <= 1
            if inside:
                edge = abs(dx)+abs(dy) > 5
                delta = -30 if edge else 22 if x < 7 else -8
                out[y][x] = tuple(max(0, min(255, c+delta)) for c in ACCENTS[f]) + (255,)
    return out


def drops(item_id, low=1, high=1, chance=None):
    entry = {'type': 'minecraft:item', 'name': 'hbk:' + item_id}
    if low != 1 or high != 1:
        entry['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': low, 'max': high}}]
    pool = {'rolls': 1, 'entries': [entry], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}
    if chance is not None:
        pool['conditions'].append({'condition': 'minecraft:random_chance', 'chance': chance})
    return pool


def recipe(name, ingredients, result, count):
    write_json(D / f'recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
              'ingredients': ['hbk:' + i for i in ingredients], 'result': {'id': 'hbk:' + result, 'count': count}})
    criteria = {f'has_{i}': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': 'hbk:' + i}]}}
                for i in dict.fromkeys(ingredients + [result])}
    write_json(D / f'advancement/recipes/{name}.json', {'parent': 'minecraft:recipes/root', 'criteria': criteria,
              'requirements': [list(criteria)], 'rewards': {'recipes': ['hbk:' + name]}})


def main():
    langs = {lang: json.loads((A / f'lang/{lang}.json').read_text()) for lang in ('en_us', 'ru_ru')}
    pickaxe, shovel, iron = [], [], []
    for f, family in enumerate(FAMILIES):
        for r, role in enumerate(ROLES):
            name = family + '_' + role
            variants = {}
            for v in range(4):
                texture = f'universe/{name}_{v}'
                for top in (False, True):
                    png(A / f'textures/block/{texture}{"_top" if top else ""}.png', rock(f, r, v, top))
                write_json(A / f'models/block/{name}_{v}.json', {'parent': 'minecraft:block/cube_column', 'textures': {
                    'side': f'hbk:block/{texture}', 'end': f'hbk:block/{texture}_top'}})
                variants[f'variant={v}'] = {'model': f'hbk:block/{name}_{v}'}
            write_json(A / f'blockstates/{name}.json', {'variants': variants})
            write_json(A / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'hbk:block/{name}_0'}})
            loot = [drops(family + '_mineral', 1, 2), drops(family + '_component', chance=.04)] if r == 4 else [drops(family + '_shard', 2, 4)] if r == 3 else [drops(name)]
            write_json(D / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': loot})
            (shovel if r == 1 and f in (0, 1, 3, 5) else pickaxe).append('hbk:' + name)
            if r in (2, 4): iron.append('hbk:' + name)
            langs['en_us']['block.hbk.' + name] = family.title() + ' ' + role.title()
            langs['ru_ru']['block.hbk.' + name] = ROCK_NAMES_RU[f][r]
        for r, role in enumerate(('mineral', 'shard', 'component')):
            name = family + '_' + role
            png(A / f'textures/item/{name}.png', item(f, r))
            write_json(A / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'hbk:item/' + name}})
            write_json(A / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': 'hbk:item/' + name}})
            langs['en_us']['item.hbk.' + name] = family.title() + ' ' + role.title()
            langs['ru_ru']['item.hbk.' + name] = RU[f] + ' ' + ['минерал', 'осколок', 'редкий компонент'][r]
        recipe(family + '_mineral', [family + '_shard']*4, family + '_mineral', 1)
        recipe(family + '_formation', [family + '_stone']*4 + [family + '_mineral']*4, family + '_formation', 4)
    for name, values in [('mineable/pickaxe', pickaxe), ('mineable/shovel', shovel), ('needs_iron_tool', iron), ('needs_diamond_tool', ['hbk:' + f + '_ore' for f in FAMILIES])]:
        path = ROOT / f'data/minecraft/tags/block/{name}.json'
        tag = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
        tag['values'] = list(dict.fromkeys(tag['values'] + values))
        write_json(path, tag)
    for lang, water, biome in [('en_us', 'Universe Water', 'Parallel Universe'), ('ru_ru', 'Вода вселенной', 'Параллельная вселенная')]:
        langs[lang]['block.hbk.universe_water'] = water
        langs[lang]['biome.hbk.universe'] = biome
    for lang, value in langs.items(): write_language(A / f'lang/{lang}.json', value)
    write_json(D / 'dimension_type/universe.json', {'ambient_light': 0, 'attributes': {
        'minecraft:visual/sky_color': '#78a7ff', 'minecraft:visual/fog_color': '#c0d8ff'},
        'coordinate_scale': 1, 'has_ceiling': False, 'has_ender_dragon_fight': False, 'has_skylight': True,
        'height': 384, 'logical_height': 384, 'min_y': -64, 'infiniburn': '#minecraft:infiniburn_overworld',
        'monster_spawn_block_light_limit': 0, 'monster_spawn_light_level': 7, 'default_clock': 'minecraft:overworld',
        'timelines': '#minecraft:in_overworld'})
    write_json(D / 'worldgen/biome/universe.json', {'has_precipitation': True, 'temperature': .6, 'downfall': .5,
        'effects': {'water_color': '#6198b0'}, 'carvers': [], 'features': [], 'spawn_costs': {},
        'spawners': {k: [] for k in ('ambient', 'axolotls', 'creature', 'misc', 'monster', 'underground_water_creature', 'water_ambient', 'water_creature')}})
    print('Generated 400 rock PNGs, 30 item PNGs, 50 blockstates, models, loot and 20 recipes.')


if __name__ == '__main__': main()
