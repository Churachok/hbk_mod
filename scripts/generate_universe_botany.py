#!/usr/bin/env python3
"""Export the checked-in original art atlases to native 16px textures and Minecraft data.

Requires Python 3 + Pillow. No network, Minecraft installation or personal paths.
Atlas source: art/universe_botany; generation prompts and provenance accompany the PNGs.
"""
from pathlib import Path
import json
import importlib.util
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
ROOT = REPO / 'src/main/resources'
A, D = ROOT / 'assets/hbk', ROOT / 'data/hbk'
ART = REPO / 'art/universe_botany'
FAMILIES = ['primordial', 'celestial', 'columnar', 'cavernous', 'woven', 'sedimentary', 'labyrinthine', 'fragmented', 'inverse', 'stratified']
SPECIES = ['foldwood', 'sailwood', 'chordwood', 'lanternwood', 'knotwood', 'siltwood', 'loopwood', 'shardwood', 'pendantwood', 'terracewood']
TREES = ['Складчатник', 'Парусник', 'Струнник', 'Фонарник', 'Узловик', 'Иловик', 'Петляк', 'Крошечник', 'Подвесник', 'Ступенник']
GENITIVE = ['складчатника', 'парусника', 'струнника', 'фонарника', 'узловика', 'иловика', 'петляка', 'крошечника', 'подвесника', 'ступенника']
PLANTS = ['Складчатый папоротник', 'Пухоцвет', 'Струнная осока', 'Фонарный колокольчик', 'Узловой папоротник', 'Иловая камышница', 'Петельчатый папоротник', 'Осколочный росток', 'Подвесной колокольчик', 'Ярусный веерник']
FRUITS = ['Складчатый стручок', 'Небесная коробочка', 'Ребристая шишка', 'Светосочная ягода', 'Смоляной узел', 'Иловый клубень', 'Компасный бутон', 'Осколочный плод', 'Подвесная ягода', 'Слоистая семечка']
RATIONS = ['Плотный паёк', 'Воздушный паёк', 'Упругий паёк', 'Светосочный паёк', 'Жилистый паёк', 'Болотный паёк', 'Тропный паёк', 'Сшивающий паёк', 'Подъёмный паёк', 'Террасный паёк']
SOILS = ['Складчатая дернина', 'Пуховая дернина', 'Струнный перегной', 'Фонарный мох', 'Узловатая подстилка', 'Иловая почва', 'Петельчатый перегной', 'Осколочная дернина', 'Подвесной мох', 'Террасная дернина']
EFFECTS_RU = ['Сопротивление 0:40', 'Замедленное падение 1:00', 'Прыгучесть II 1:00', 'Ночное зрение 3:00', 'Спешка 1:00', 'Подводное дыхание 2:00', 'Скорость 1:00', 'Регенерация 0:10', 'Левитация 0:05 + медленное падение 1:00', 'Огнестойкость 1:30']
EFFECTS_EN = ['Resistance 0:40', 'Slow Falling 1:00', 'Jump Boost II 1:00', 'Night Vision 3:00', 'Haste 1:00', 'Water Breathing 2:00', 'Speed 1:00', 'Regeneration 0:10', 'Levitation 0:05 + Slow Falling 1:00', 'Fire Resistance 1:30']
ROLES = ['log', 'stripped_log', 'planks', 'stairs', 'slab', 'fence', 'leaves', 'soil', 'plant', 'sapling', 'lamp']
spec = importlib.util.spec_from_file_location('rocks', REPO / 'scripts/generate_universe_assets.py')
rocks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rocks)
write = rocks.write_json


def tile(atlas, index, opaque=False, top=False):
    w, h = atlas.size
    x, y = index % 4, index // 4
    bounds = (round(x*w/4)+2, round(y*h/4)+2, round((x+1)*w/4)-2, round((y+1)*h/4)-2)
    image = atlas.crop(bounds)
    if top:
        image = image.crop((0, 0, image.width, round(image.height * .28)))
    image = image.resize((16, 16), Image.Resampling.NEAREST)
    # Game cutouts use binary alpha; discard antialiased edge fringes without changing the art.
    pixels = [(r, g, b, 255 if opaque or a >= 192 else 0) for r, g, b, a in
              (image.getpixel((x, y)) for y in range(16) for x in range(16))]
    image.putdata(pixels)
    return image


def texture(species, name, image):
    path = A / f'textures/block/botany/{species}_{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=False)


def subsoil_tile(index):
    atlas = Image.open(ART / 'subsoils-v2.png').convert('RGBA')
    w, h = atlas.size
    x, y = index % 5, index // 5
    return atlas.crop((round(x*w/5), round(y*h/4), round((x+1)*w/5), round((y+1)*h/4))).resize((16, 16), Image.Resampling.NEAREST)


def model(name, parent, textures):
    write(A / f'models/block/{name}.json', {'parent': 'minecraft:block/' + parent, 'textures': textures})


def blockstate(name, variants):
    write(A / f'blockstates/{name}.json', {'variants': variants})


def item_model(name, block=None, sprite=None):
    if sprite:
        write(A / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': sprite}})
    write(A / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'hbk:block/{block or name}' if not sprite else f'hbk:item/{name}'}})


def tag(kind, name, values, namespace='minecraft'):
    path = ROOT / f'data/{namespace}/tags/{kind}/{name}.json'
    data = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
    data['values'] = list(dict.fromkeys(data['values'] + values))
    write(path, data)


def loot(name, pools=None):
    write(D / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': pools or [rocks.drops(name)]})


def recipe(name, result, count=1, ingredients=None, pattern=None, key=None):
    data = {'type': 'minecraft:crafting_shaped' if pattern else 'minecraft:crafting_shapeless', 'category': 'misc', 'result': {'id': 'hbk:' + result, 'count': count}}
    if pattern:
        data.update(pattern=pattern, key=key)
        inputs = list(key.values())
    else:
        data['ingredients'] = ingredients
        inputs = ingredients
    write(D / f'recipe/{name}.json', data)
    criteria = {'has_' + str(i): {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': value}]}} for i, value in enumerate(dict.fromkeys(inputs + ['hbk:' + result]))}
    write(D / f'advancement/recipes/{name}.json', {'parent': 'minecraft:recipes/root', 'criteria': criteria, 'requirements': [list(criteria)], 'rewards': {'recipes': ['hbk:' + name]}})


def cross_model(name, texture_id, height=16):
    elements = []
    for first in (True, False):
        faces = {side: {'uv': [0, 0, 16, 16], 'texture': '#cross'} for side in (('north', 'south') if first else ('west', 'east'))}
        elements.append({'from': [0.8, 0, 8] if first else [8, 0, .8], 'to': [15.2, height, 8] if first else [8, height, 15.2],
                         'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True}, 'shade': False, 'faces': faces})
    write(A / f'models/block/{name}.json', {'ambientocclusion': False, 'textures': {'particle': texture_id, 'cross': texture_id}, 'elements': elements})


def generate(f, species, langs):
    atlas = Image.open(ART / (FAMILIES[f] + '.png')).convert('RGBA')
    for name, index, opaque in [('log', 0, True), ('log_top', 1, True), ('planks', 2, True), ('stripped_log', 3, True),
                                ('leaves', 4, False), ('plant', 6, False), ('sapling', 7, False), ('fruit', 8, False), ('ration', 9, False),
                                ('lamp', 10, True), ('leaves_alt', 12, False), ('soil_side', 13, True), ('sprout', 14, False)]:
        texture(species, name, tile(atlas, index, opaque))
    texture(species, 'soil', tile(Image.open(ART / 'soil_tops.png').convert('RGBA'), f, True))
    texture(species, 'soil_side', subsoil_tile(f + 10))
    loam = FAMILIES[f] + '_loam'
    loam_texture = 'hbk:block/botany/' + species + '_loam'
    texture(species, 'loam', subsoil_tile(f))
    model(loam, 'cube_all', {'all': loam_texture})
    blockstate(loam, {'': {'model': 'hbk:block/' + loam}})
    item_model(loam)
    loot(loam)
    tag('block', 'mineable/shovel', ['hbk:' + loam])
    tag('block', 'supports_vegetation', ['hbk:' + loam])
    tag('item', 'sapling_soil', ['minecraft:dirt', 'hbk:' + loam], namespace='hbk')
    langs['en_us']['block.hbk.' + loam] = FAMILIES[f].title() + ' Loam'
    langs['ru_ru']['block.hbk.' + loam] = ['Складчатый суглинок', 'Небесный суглинок', 'Базальтовый перегной', 'Известняковый суглинок', 'Узловой суглинок', 'Илистый суглинок', 'Лабиринтный перегной', 'Осколочный суглинок', 'Подвесной перегной', 'Террасный суглинок'][f]
    t = lambda role: f'hbk:block/botany/{species}_{role}'
    for role in ['log', 'stripped_log']:
        name = species + '_' + role
        model(name, 'cube_column', {'side': t(role), 'end': t('log_top')})
        blockstate(name, {'axis=y': {'model': 'hbk:block/' + name}, 'axis=x': {'model': 'hbk:block/' + name, 'x': 90, 'y': 90}, 'axis=z': {'model': 'hbk:block/' + name, 'x': 90}})
    for role in ['planks', 'lamp', 'leaves', 'leaves_alt']:
        model(species + '_' + role, 'cube_all', {'all': t(role)})
    blockstate(species + '_planks', {'': {'model': 'hbk:block/' + species + '_planks'}})
    blockstate(species + '_lamp', {'': {'model': 'hbk:block/' + species + '_lamp'}})
    blockstate(species + '_leaves', {'': [{'model': 'hbk:block/' + species + '_leaves', 'weight': 3}, {'model': 'hbk:block/' + species + '_leaves_alt'}]})
    for role in ['leaves', 'leaves_alt']:
        write(A / f'textures/block/botany/{species}_{role}.png.mcmeta', {'texture': {'mipmap_strategy': 'dark_cutout'}})
    model(species + '_soil', 'cube_bottom_top', {'top': t('soil'), 'side': t('soil_side'), 'bottom': t('soil') if f == 8 else loam_texture})
    blockstate(species + '_soil', {'': {'model': 'hbk:block/' + species + '_soil'}})
    slab = species + '_slab'
    for suffix, parent in [('', 'slab'), ('_top', 'slab_top')]: model(slab + suffix, parent, dict.fromkeys(['bottom', 'top', 'side'], t('planks')))
    blockstate(slab, {f'type={typ}': {'model': 'hbk:block/' + species + target} for typ, target in [('bottom', '_slab'), ('top', '_slab_top'), ('double', '_planks')]})
    stairs = species + '_stairs'
    for suffix, parent in [('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')]: model(stairs + suffix, parent, dict.fromkeys(['bottom', 'top', 'side'], t('planks')))
    variants = {}
    for facing, angle in [('east', 0), ('south', 90), ('west', 180), ('north', 270)]:
        for half in ['bottom', 'top']:
            for shape in ['straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right']:
                suffix = '' if shape == 'straight' else '_inner' if shape.startswith('inner') else '_outer'
                y = (angle + (270 if shape.endswith('left') else 0) + (90 if half == 'top' and shape != 'straight' else 0)) % 360
                variants[f'facing={facing},half={half},shape={shape}'] = {'model': 'hbk:block/' + stairs + suffix, 'uvlock': True, **({'x': 180} if half == 'top' else {}), **({'y': y} if y else {})}
    blockstate(stairs, variants)
    fence = species + '_fence'
    for suffix, parent in [('_post', 'fence_post'), ('_side', 'fence_side'), ('_inventory', 'fence_inventory')]: model(fence + suffix, parent, {'texture': t('planks')})
    write(A / f'blockstates/{fence}.json', {'multipart': [{'apply': {'model': 'hbk:block/' + fence + '_post'}}] + [
        {'when': {direction: 'true'}, 'apply': {'model': 'hbk:block/' + fence + '_side', 'y': rotation, 'uvlock': True}}
        for direction, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]]})
    for role in ['plant', 'sapling']:
        variants = {}
        for stage in range(4 if role == 'plant' else 2):
            name = species + '_' + role + '_' + str(stage)
            cross_model(name, t('sprout' if role == 'plant' and stage == 0 else role), [6, 10, 13, 16][stage] if role == 'plant' else 16)
            for hanging in [False, True]:
                variants[f'{"age" if role == "plant" else "stage"}={stage},hanging={str(hanging).lower()}'] = {'model': 'hbk:block/' + name, **({'x': 180} if hanging else {})}
        blockstate(species + '_' + role, variants)
    for role in ROLES:
        name = species + '_' + role
        item_model(name, block=fence + '_inventory' if role == 'fence' else None, sprite=t(role) if role in ['plant', 'sapling'] else None)
        loot(name)
    for role in ['fruit', 'ration']: item_model(species + '_' + role, sprite=t(role))
    loot(slab, [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'hbk:' + slab, 'functions': [{'function': 'minecraft:set_count', 'count': 2, 'conditions': [{'condition': 'minecraft:block_state_property', 'block': 'hbk:' + slab, 'properties': {'type': 'double'}}]}]}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}])
    shear = {'condition': 'minecraft:any_of', 'terms': [{'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}}, {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]}
    loot(species + '_leaves', [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'name': 'hbk:' + species + '_leaves', 'conditions': [shear]},
        {'type': 'minecraft:item', 'name': 'hbk:' + species + '_sapling', 'conditions': [{'condition': 'minecraft:survives_explosion'}, {'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune', 'chances': [.07, .09, .11, .14, .18]}]}]}]},
        {**rocks.drops(species + '_fruit', chance=.12), 'conditions': [{'condition': 'minecraft:inverted', 'term': shear}, {'condition': 'minecraft:random_chance', 'chance': .12}, {'condition': 'minecraft:survives_explosion'}]}])
    loot(species + '_plant', [rocks.drops(species + '_plant'), {**rocks.drops(species + '_fruit', 2, 3), 'conditions': [{'condition': 'minecraft:block_state_property', 'block': 'hbk:' + species + '_plant', 'properties': {'age': '3'}}, {'condition': 'minecraft:survives_explosion'}]}])
    logs = ['hbk:' + species + '_' + r for r in ['log', 'stripped_log']]
    for kind in ['item', 'block']:
        tag(kind, species + '_logs', logs, 'hbk')
        tag(kind, 'logs', logs); tag(kind, 'logs_that_burn', logs)
        for role, tagname in [('planks', 'planks'), ('leaves', 'leaves'), ('sapling', 'saplings'), ('stairs', 'wooden_stairs'), ('slab', 'wooden_slabs'), ('fence', 'wooden_fences')]: tag(kind, tagname, ['hbk:' + species + '_' + role])
    tag('block', 'supports_vegetation', ['hbk:' + species + '_soil'])
    tag('block', 'supports_bamboo', ['hbk:' + species + '_soil'])
    tag('block', 'mineable/axe', ['hbk:' + species + '_' + r for r in ['log', 'stripped_log', 'planks', 'stairs', 'slab', 'fence']])
    tag('block', 'mineable/shovel', ['hbk:' + species + '_soil'])
    tag('block', 'mineable/hoe', ['hbk:' + species + '_leaves'])
    tag('block', 'replaceable', ['hbk:' + species + '_plant', 'hbk:' + species + '_sapling'])
    h = lambda role: 'hbk:' + species + '_' + role
    recipe(species + '_planks', species + '_planks', 4, ingredients=['#hbk:' + species + '_logs'])
    recipe(slab, slab, 6, pattern=['PPP'], key={'P': h('planks')})
    recipe(stairs, stairs, 4, pattern=['P  ', 'PP ', 'PPP'], key={'P': h('planks')})
    recipe(fence, fence, 3, pattern=['PSP', 'PSP'], key={'P': h('planks'), 'S': 'minecraft:stick'})
    mineral = 'hbk:' + FAMILIES[f] + '_mineral'
    recipe(species + '_lamp', species + '_lamp', 4, pattern=['PFP', 'FMF', 'PFP'], key={'P': h('planks'), 'F': h('fruit'), 'M': mineral})
    recipe(species + '_ration', species + '_ration', ingredients=[h('fruit'), h('fruit'), 'minecraft:wheat', mineral])
    recipe(species + '_sapling', species + '_sapling', 2, ingredients=[h('fruit'), mineral, '#hbk:sapling_soil'])
    for role in ROLES:
        en = species.title() + ' ' + role.replace('_', ' ').title()
        ru = {'log': 'Бревно ', 'stripped_log': 'Обтёсанное бревно ', 'planks': 'Доски ', 'stairs': 'Ступеньки ', 'slab': 'Плита ', 'fence': 'Забор ', 'leaves': 'Листва ', 'sapling': 'Саженец ', 'lamp': 'Светильник '}.get(role, '') + GENITIVE[f]
        if role == 'soil': ru = SOILS[f]
        if role == 'plant': ru = PLANTS[f]
        langs['en_us']['block.hbk.' + species + '_' + role] = en
        langs['ru_ru']['block.hbk.' + species + '_' + role] = ru
    for role, ru in [('fruit', FRUITS[f]), ('ration', RATIONS[f])]:
        langs['ru_ru']['item.hbk.' + species + '_' + role] = ru
        langs['en_us']['item.hbk.' + species + '_' + role] = species.title() + (' Harvest' if role == 'fruit' else ' Expedition Ration')
    for lang in langs:
        title, effect = (TREES[f], EFFECTS_RU[f]) if lang == 'ru_ru' else (species.title(), EFFECTS_EN[f])
        langs[lang]['book.hbk.recipes.info.' + species + '_flora'] = (f'{f}: {title}\n\n{PLANTS[f]}\n{FRUITS[f]}\n\nПКМ — сбор урожая.\nРастение отрастает.\n\nСаженцы возвращают\nэтот вид дерева.\nПод потолком — вниз.' if lang == 'ru_ru' else f'{f}: {title}\n\nRight-click mature plants for harvests. They regrow.\n\nSaplings regrow this tree. Under a ceiling, they grow downward.\n\nLog → 4 planks.')
        short_effect = 'Левитация 0:05,\nпадение 1:00' if f == 8 else effect
        langs[lang]['book.hbk.recipes.info.' + species + '_crafts'] = (f'{f}: крафты\n\nПаёк: 2 плода,\nпшеница и минерал.\n{short_effect}.\n\nСветильник:\nминерал в центре,\nдоски по углам,\nплоды по сторонам.\n\nПрочее: зелёная книга.' if lang == 'ru_ru' else f'{f}: crafting\n\nRation: 2 harvests,\nwheat, local mineral.\n{effect}.\n\nLamp: mineral centre,\nplanks in corners,\nharvests on sides.\n\nMore: recipe book.')
        langs[lang]['ecology.hbk.' + species] = effect


def main():
    langs = {lang: json.loads((A / f'lang/{lang}.json').read_text()) for lang in ['en_us', 'ru_ru']}
    for f, species in enumerate(SPECIES): generate(f, species, langs)
    habitat_ids = ['meadow', 'sky_garden', 'spire_taiga', 'lantern_grotto', 'braided_jungle', 'basin_marsh', 'maze_forest', 'shard_birches', 'hanging_garden', 'terrace_savanna']
    habitat_ru = ['Складчатые луга', 'Парусные сады', 'Струнные рощи', 'Фонарные гроты', 'Узловые джунгли', 'Иловые котловины', 'Петельчатый лабиринт', 'Осколочные сады', 'Подвесные сады', 'Ступенчатые рощи']
    for f, key in enumerate(habitat_ids):
        langs['ru_ru']['habitat.hbk.' + key] = habitat_ru[f]
        langs['en_us']['habitat.hbk.' + key] = SPECIES[f].title() + ' Wilds'
    langs['ru_ru']['message.hbk.universe.ecology'] = '%s. Древесина: %s. Характерная руда: %s. Своё бревно даёт 4 доски; доступны плиты, ступеньки, заборы и светильники. Саженцы восстанавливают местное дерево. Все рецепты — в зелёной книге.'
    langs['en_us']['message.hbk.universe.ecology'] = '%s. Timber: %s. Signature ore: %s. Logs make 4 family planks for slabs, stairs, fences and lamps. Saplings regrow this original tree. All recipes are in the recipe book.'
    langs['ru_ru']['message.hbk.universe.harvest'] = 'Растение: %s. Урожай: %s. ПКМ собирает плоды, затем они отрастают. Паёк из 2 плодов, пшеницы и местного минерала: %s.'
    langs['en_us']['message.hbk.universe.harvest'] = 'Plant: %s. Harvest: %s. Right-click to harvest; it regrows. A ration of 2 harvests, wheat and the local mineral grants %s.'
    langs['ru_ru']['book.hbk.recipes.info.universe_ecology'] = 'Местная флора\n\nУ каждой первой цифры свои деревья, почва и урожай.\n\nБревно → 4 своих доски. Саженец восстанавливает местное дерево.\n\nПКМ по зрелым растениям — урожай.\n\nДалее: 10 семейств.'
    langs['en_us']['book.hbk.recipes.info.universe_ecology'] = 'Local flora\n\nEach first digit has its own trees, soil and harvests.\n\nLog → 4 family planks. Saplings regrow that original tree.\n\nRight-click ripe plants to harvest.\n\nNext: all 10 families.'
    langs['ru_ru']['tag.item.hbk.sapling_soil'] = 'Грунт для местных саженцев'
    langs['en_us']['tag.item.hbk.sapling_soil'] = 'Soil for original saplings'
    for lang, data in langs.items(): rocks.write_language(A / f'lang/{lang}.json', data)
    print('Exported 10 original botanical sets: 120 blocks (including local loam), 20 foods, 150 textures and 70 recipes.')


if __name__ == '__main__': main()
