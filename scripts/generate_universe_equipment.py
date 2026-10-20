#!/usr/bin/env python3
"""Vanilla model references and mineral tints; no copied PNGs or external dependencies.

Native iron silhouettes/UV textures, diamond statistics registered in Java.
Run after generate_universe_assets.py. Existing unrelated tags/translations are preserved.
"""
from pathlib import Path
import importlib.util
import json

REPO = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('rocks', REPO / 'scripts/generate_universe_assets.py')
rocks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rocks)
A, D, ROOT = rocks.A, rocks.D, rocks.ROOT
ROLES = ['sword', 'pickaxe', 'axe', 'shovel', 'hoe', 'helmet', 'chestplate', 'leggings', 'boots']
PATTERNS = [['M', 'M', 'S'], ['MMM', ' S ', ' S '], ['MM', 'MS', ' S'], ['M', 'S', 'S'], ['MM', ' S', ' S'],
            ['MMM', 'M M'], ['M M', 'MMM', 'MMM'], ['MMM', 'M M', 'M M'], ['M M', 'M M']]
NOUNS_RU = ['меч', 'кирка', 'топор', 'лопата', 'мотыга', 'шлем', 'нагрудник', 'поножи', 'ботинки']


def tag(kind, name, values, namespace='minecraft'):
    path = ROOT / f'data/{namespace}/tags/{kind}/{name}.json'
    data = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
    data['values'] = list(dict.fromkeys(data['values'] + values))
    rocks.write_json(path, data)


def main():
    langs = {lang: json.loads((A / f'lang/{lang}.json').read_text()) for lang in ['en_us', 'ru_ru']}
    ore_ids, pickaxes = [], ['minecraft:diamond_pickaxe', 'minecraft:netherite_pickaxe', 'hbk:redstone_pickaxe']
    groups = ['swords', 'pickaxes', 'axes', 'shovels', 'hoes', 'head_armor', 'chest_armor', 'leg_armor', 'foot_armor']
    for f, family in enumerate(rocks.FAMILIES):
        mineral = 'hbk:' + family + '_mineral'
        colour = (255 << 24) | (rocks.ACCENTS[f][0] << 16) | (rocks.ACCENTS[f][1] << 8) | rocks.ACCENTS[f][2]
        colour -= 1 << 32  # Native ARGB int codec.
        tag('item', 'repairs_' + family + '_equipment', [mineral], 'hbk')
        langs['en_us']['tag.item.hbk.repairs_' + family + '_equipment'] = family.title() + ' Equipment Repair Materials'
        langs['ru_ru']['tag.item.hbk.repairs_' + family + '_equipment'] = 'Минерал для ремонта: ' + rocks.RU[f].lower()
        rocks.write_json(A / f'equipment/{family}.json', {'layers': {
            category: [{'texture': 'minecraft:iron', 'dyeable': {'color_when_undyed': colour}}]
            for category in ['humanoid', 'humanoid_baby', 'humanoid_leggings']}})
        for r, role in enumerate(ROLES):
            name = family + '_' + role
            rocks.write_json(A / f'models/item/{name}.json', {'parent': 'minecraft:item/iron_' + role})
            rocks.write_json(A / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': 'hbk:item/' + name,
                'tints': [{'type': 'minecraft:constant', 'value': colour}]}})
            key = {'M': mineral}
            if r < 5: key['S'] = 'minecraft:stick'
            rocks.write_json(D / f'recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
                'pattern': PATTERNS[r], 'key': key, 'result': {'id': 'hbk:' + name}})
            rocks.write_json(D / f'advancement/recipes/{name}.json', {'parent': 'minecraft:recipes/root', 'criteria': {
                'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': mineral}]}}},
                'requirements': [['has_material']], 'rewards': {'recipes': ['hbk:' + name]}})
            tag('item', groups[r], ['hbk:' + name])
            if r == 1:
                pickaxes.append('hbk:' + name)
                tag('item', 'cluster_max_harvestables', ['hbk:' + name])
            adjective = rocks.RU[f]
            if r in [1, 3, 4]: adjective = adjective[:-2] + 'ая'
            if r in [7, 8]: adjective = adjective[:-2] + 'ые'
            langs['ru_ru']['item.hbk.' + name] = adjective + ' ' + NOUNS_RU[r]
            langs['en_us']['item.hbk.' + name] = family.title() + ' ' + role.title()
        ore_ids.append('hbk:' + family + '_ore')
    for ore in ['coal', 'iron', 'copper', 'gold', 'redstone', 'lapis', 'emerald', 'diamond']:
        ore_ids += ['minecraft:' + ore + '_ore', 'minecraft:deepslate_' + ore + '_ore']
    ore_ids += ['minecraft:nether_gold_ore', 'minecraft:nether_quartz_ore', 'minecraft:ancient_debris', 'hbk:uranium_ore']
    tag('block', 'parallel_ores', ore_ids, 'hbk')
    tag('item', 'parallel_ore_pickaxes', pickaxes, 'hbk')
    tag('block', 'mineable/pickaxe', ['hbk:fractured_block'])
    langs['ru_ru']['tag.item.hbk.parallel_ore_pickaxes'] = 'Кирки для руд параллельных миров'
    langs['en_us']['tag.item.hbk.parallel_ore_pickaxes'] = 'Parallel Ore Pickaxes'
    langs['ru_ru']['book.hbk.recipes.info.universe_equipment'] = 'Снаряжение\n\nУ каждой руды — 9 предметов.\n\nКрафты ванильные: минералы и палки.\n\nАлмазный уровень. Ремонт — своим минералом.'
    langs['en_us']['book.hbk.recipes.info.universe_equipment'] = 'Mineral equipment\n\nNine items per ore.\n\nVanilla recipes: minerals and sticks.\n\nDiamond statistics. Repair with its mineral.'
    langs['ru_ru']['book.hbk.recipes.info.universe_mining'] = 'Добыча\n\nРуды: алмазная кирка или выше.\n\nОсколок пространства: редстоуновая кирка, материальный разломанный блок.\n\nКрафта осколков нет.'
    langs['en_us']['book.hbk.recipes.info.universe_mining'] = 'Mining\n\nParallel ores: diamond pickaxe or better.\n\nSpatial shard: redstone pickaxe on a material fractured block.\n\nNo shard crafting.'
    langs['ru_ru']['book.hbk.recipes.info.universe_travel'] = 'Параллельные миры\n\nРядом с местом прибытия стоит обратная печь.\n\nОткройте её и нажмите «Вернуться».\n\nИнвентарь сохраняется.'
    langs['en_us']['book.hbk.recipes.info.universe_travel'] = 'Parallel worlds\n\nA return furnace stands near your arrival.\n\nOpen it and press Return.\n\nYour inventory travels with you.'
    for lang, data in langs.items(): rocks.write_language(A / f'lang/{lang}.json', data)
    print('Generated 90 vanilla-style equipment items/recipes, 10 native equipment assets and mining/repair tags.')


if __name__ == '__main__': main()
