#!/usr/bin/env python3
"""45 binary alloys: recipes, native models, original 16px metal art (Python + Pillow)."""
from pathlib import Path
import importlib.util
import json
import generate_universe_alloy_textures as art

REPO = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('equipment', REPO / 'scripts/generate_universe_equipment.py')
equipment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(equipment)
rocks = equipment.rocks
A, D = rocks.A, rocks.D

# pair | alloy identifier | Russian alloy | English alloy | reward(s): id, role, native model, Russian, English
ROWS = '''01|light_bronze|Лёгкая бронза|Light Bronze|gliding_chestplate,chestplate,netherite_chestplate,Планирующий нагрудник,Gliding Chestplate
02|drilling_steel|Буровая сталь|Drilling Steel|drill_pickaxe,pickaxe,netherite_pickaxe,Буровая кирка,Drill Pickaxe
03|light_steel|Светосталь|Lightsteel|mining_helmet,helmet,netherite_helmet,Шахтёрский шлем,Mining Helmet
04|root_steel|Корневая сталь|Root Steel|forest_axe,axe,netherite_axe,Лесной топор,Forest Axe
05|earth_bronze|Земляная бронза|Earth Bronze|field_hoe,hoe,netherite_hoe,Полевая мотыга,Field Hoe;field_shovel,shovel,netherite_shovel,Полевая лопата,Field Shovel
06|trail_steel|Следовая сталь|Trail Steel|entry_compass,misc,compass,Компас входа,Entry Compass
07|assembly_alloy|Сборочный сплав|Assembly Alloy|recycling_anvil,device,anvil,Разборочная наковальня,Recycling Anvil
08|support_inverite|Опорный инверит|Support Inverite|building_glove,misc,brush,Строительная перчатка,Building Glove
09|repair_steel|Ремонтная сталь|Repair Steel|field_repair_kit,misc,shears,Походный ремкомплект,Field Repair Kit
12|ballast_alloy|Балластный сплав|Ballast Alloy|anchor_boots,boots,netherite_boots,Якорные ботинки,Anchor Boots
13|dawn_alloy|Рассветный сплав|Dawn Alloy|expedition_barometer,misc,clock,Экспедиционный барометр,Expedition Barometer
14|sail_wire|Парусная проволока|Sail Wire|grappling_hook,misc,fishing_rod,Крюк-кошка,Grappling Hook
15|foam_alloy|Пенный сплав|Foam Alloy|air_tank,misc,nautilus_shell,Дыхательный баллон,Air Tank
16|wind_alloy|Ветровой сплав|Wind Alloy|wind_vane,device,daylight_detector,Флюгер,Wind Vane
17|impulse_alloy|Импульсный сплав|Impulse Alloy|impulse_crossbow,crossbow,crossbow,Импульсный арбалет,Impulse Crossbow
18|sail_alloy|Парусный сплав|Sail Alloy|sail_boots,boots,netherite_boots,Парусные ботинки,Sail Boots
19|lifting_alloy|Подъёмный сплав|Lifting Alloy|lift_platform,device,heavy_weighted_pressure_plate,Лифт-платформа,Lift Platform
23|sonorous_steel|Гулкая сталь|Sonorous Steel|seismic_hammer,pickaxe,netherite_pickaxe,Сейсмический молот,Seismic Hammer
24|spring_steel|Упругая сталь|Spring Steel|spring_shield,shield,shield,Упругий щит,Spring Shield
25|seabed_steel|Донная сталь|Seabed Steel|diving_boots,boots,netherite_boots,Водолазные ботинки,Diving Boots
26|fastening_alloy|Крепёжный сплав|Fastening Alloy|row_builder,misc,brush,Укладчик,Row Builder
27|ramming_steel|Таранная сталь|Ramming Steel|blast_shield,shield,shield,Противовзрывной щит,Blast Shield
28|counterweight_alloy|Обратновесный сплав|Counterweight Alloy|gravity_chestplate,chestplate,netherite_chestplate,Гравитационный нагрудник,Gravity Chestplate
29|riveted_steel|Клёпаная сталь|Riveted Steel|cargo_minecart,minecart,chest_minecart,Грузовая вагонетка,Cargo Minecart
34|lantern_alloy|Фонарный сплав|Lantern Alloy|growth_lamp,device,sea_lantern,Светильник-теплица,Growth Lamp
35|filtering_alloy|Фильтрующий сплав|Filtering Alloy|air_purifier,device,blast_furnace,Очиститель воздуха,Air Purifier
36|echo_steel|Эхосталь|Echosteel|anomaly_sensor,device,observer,Датчик аномалий,Anomaly Sensor
37|resonant_alloy|Резонансный сплав|Resonant Alloy|tuning_fork,misc,amethyst_shard,Камертон,Tuning Fork
38|clear_inverite|Просветный инверит|Clear Inverite|phase_glass,device,tinted_glass,Фазовое стекло,Phase Glass
39|heat_alloy|Жаровой сплав|Heat Alloy|smelting_pickaxe,pickaxe,netherite_pickaxe,Жаровая кирка,Smelting Pickaxe
45|hermetic_alloy|Герметичный сплав|Hermetic Alloy|hermetic_helmet,helmet,netherite_helmet,Герметичный шлем,Hermetic Helmet
46|loop_steel|Петельная сталь|Loop Steel|rope_ladder_spool,misc,lead,Катушка верёвочной лестницы,Rope Ladder Spool
47|sharp_thread|Остронит|Sharpthread|harvest_sickle,hoe,netherite_hoe,Серп,Harvest Sickle
48|reverse_wire|Обратная проволока|Reverse Wire|carrying_grips,misc,shears,Переносные захваты,Carrying Grips
49|membrane_alloy|Мембранный сплав|Membrane Alloy|sample_bag,misc,bundle,Сумка образцов,Sample Bag
56|washing_alloy|Промывочный сплав|Washing Alloy|washing_trough,device,cauldron,Промывочный лоток,Washing Trough
57|magnetic_alloy|Магнитный сплав|Magnetic Alloy|miner_belt,misc,iron_nugget,Шахтёрский пояс,Miner Belt
58|capillary_alloy|Капиллярный сплав|Capillary Alloy|drain_pump,device,dispenser,Осушающий насос,Drain Pump
59|thermal_alloy|Теплоёмкий сплав|Thermal Alloy|heat_cartridge,misc,blaze_rod,Тепловая кассета,Heat Cartridge
67|prospecting_alloy|Поисковый сплав|Prospecting Alloy|ore_compass,misc,recovery_compass,Рудный компас,Ore Compass
68|route_inverite|Маршрутный инверит|Route Inverite|transition_plate,device,light_weighted_pressure_plate,Переходная плита,Transition Plate
69|rhythm_steel|Ритмосталь|Rhythmsteel|rhythm_controller,device,repeater,Ритмовый контроллер,Rhythm Controller
78|slit_alloy|Щелевой сплав|Slit Alloy|rift_sword,sword,netherite_sword,Щелевой меч,Rift Sword
79|shard_steel|Осколочная сталь|Shard Steel|shaped_charge,misc,fire_charge,Точный заряд,Shaped Charge
89|return_alloy|Возвратный сплав|Return Alloy|return_capsule,misc,ender_pearl,Капсула возврата,Return Capsule'''


def definitions():
    result = []
    for line in ROWS.splitlines():
        pair, name, ru, en, reward_text = line.split('|')
        rewards = []
        for text in reward_text.split(';'):
            rid, role, model, rru, ren = text.split(',')
            base = 'netherite_' + role if role in equipment.ROLES else 'netherite_ingot'
            rewards.append(dict(id=rid, role=role, model=model, ru=rru, en=ren, base=base))
        result.append(dict(first=int(pair[0]), second=int(pair[1]), id=name, ru=ru, en=en, rewards=rewards))
    assert len(result) == 45 and len({(a['first'], a['second']) for a in result}) == 45
    return result


def main():
    alloys = definitions()
    art.export(alloys)
    rocks.write_json(rocks.ROOT / 'hbk/alloys.json', alloys)
    langs = {l: json.loads((A / f'lang/{l}.json').read_text()) for l in ['ru_ru', 'en_us']}
    recipes = []
    def recipe(name, data, material):
        rocks.write_json(D / f'recipe/{name}.json', data)
        recipes.append(name)
        rocks.write_json(D / f'advancement/recipes/{name}.json', {'parent': 'minecraft:recipes/root', 'criteria': {
            'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': material}]}}},
            'requirements': [['has_material']], 'rewards': {'recipes': ['hbk:' + name]}})
    def item(name, model, colour=None):
        # Copy native item definitions only where their animation dispatch is needed.
        native_dynamic = model in ['shield', 'crossbow', 'compass', 'recovery_compass', 'clock', 'bundle']
        if native_dynamic:
            definition = {'model': {'type': 'minecraft:model', 'model': 'hbk:item/' + name}}
            fallback = {'shield': 'shield', 'crossbow': 'crossbow', 'compass': 'compass_16',
                        'recovery_compass': 'recovery_compass_16', 'clock': 'clock_00', 'bundle': 'bundle'}[model]
            rocks.write_json(A / f'models/item/{name}.json', {'parent': 'minecraft:item/' + fallback})
        else:
            rocks.write_json(A / f'models/item/{name}.json', {'parent': 'minecraft:item/' + model})
            definition = {'model': {'type': 'minecraft:model', 'model': 'hbk:item/' + name}}
        if colour is not None:
            definition['model']['tints'] = [{'type': 'minecraft:constant', 'value': colour}]
        rocks.write_json(A / f'items/{name}.json', definition)
    groups = dict(zip(equipment.ROLES, ['swords','pickaxes','axes','shovels','hoes','head_armor','chest_armor','leg_armor','foot_armor']))
    craft_rows = []
    for index, a in enumerate(alloys):
        name = a['id']
        rgb = art.colour(index)
        colour = ((255 << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]) - (1 << 32)
        for f in [a['first'], a['second']]:
            family = rocks.FAMILIES[f]
            equipment.tag('item', 'alloy_materials/' + family, ['hbk:' + family + '_ore', 'hbk:' + family + '_mineral'], 'hbk')
        sample, ingot, block = name + '_sample', name + '_ingot', name + '_block'
        for material in [sample, ingot]:
            rocks.write_json(A / f'models/item/{material}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'hbk:item/alloys/' + material}})
            rocks.write_json(A / f'items/{material}.json', {'model': {'type': 'minecraft:model', 'model': 'hbk:item/' + material}})
        rocks.write_json(A / f'models/block/{block}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'hbk:block/alloys/' + block}})
        rocks.write_json(A / f'blockstates/{block}.json', {'variants': {'': {'model': 'hbk:block/' + block}}})
        rocks.write_json(A / f'items/{block}.json', {'model': {'type': 'minecraft:model', 'model': 'hbk:block/' + block}})
        rocks.write_json(D / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'hbk:' + block}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        equipment.tag('block', 'mineable/pickaxe', ['hbk:' + block])
        equipment.tag('block', 'needs_stone_tool', ['hbk:' + block])
        recipe(sample, {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [
            '#hbk:alloy_materials/' + rocks.FAMILIES[f] for f in [a['first'], a['second']]], 'result': {'id': 'hbk:' + sample}}, '#hbk:alloy_materials/' + rocks.FAMILIES[a['first']])
        recipe(name + '_blasting', {'type': 'minecraft:blasting', 'category': 'misc', 'ingredient': 'hbk:' + sample,
            'result': {'id': 'hbk:' + ingot}, 'experience': 1.0, 'cookingtime': 200}, 'hbk:' + sample)
        recipe(block, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['III'] * 3,
            'key': {'I': 'hbk:' + ingot}, 'result': {'id': 'hbk:' + block}}, 'hbk:' + ingot)
        recipe(name + '_from_block', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ['hbk:' + block], 'result': {'id': 'hbk:' + ingot, 'count': 9}}, 'hbk:' + block)
        equipment.tag('item', 'repairs_' + name, ['hbk:' + ingot], 'hbk')
        langs['ru_ru']['tag.item.hbk.repairs_' + name] = 'Ремонт: ' + a['ru']
        langs['en_us']['tag.item.hbk.repairs_' + name] = a['en'] + ' Repair Materials'
        for lang, label in [('ru_ru', a['ru']), ('en_us', a['en'])]:
            langs[lang]['item.hbk.' + sample] = ('Образец: ' if lang == 'ru_ru' else 'Sample: ') + label
            langs[lang]['item.hbk.' + ingot] = label
            langs[lang]['block.hbk.' + block] = ('Блок: ' if lang == 'ru_ru' else 'Block: ') + label
        for reward in a['rewards']:
            rid, role, model = reward['id'], reward['role'], reward['model']
            if role == 'device':
                native = 'minecraft:block/' + ('repeater_1tick' if model == 'repeater' else model)
                rocks.write_json(A / f'blockstates/{rid}.json', {'variants': {
                    '': {'model': native}}} if rid not in ['phase_glass'] else {'variants': {'powered=false': {'model': native}, 'powered=true': {'model': 'minecraft:block/glass'}}})
                rocks.write_json(A / f'items/{rid}.json', {'model': {'type': 'minecraft:model', 'model': native}})
                rocks.write_json(D / f'loot_table/blocks/{rid}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'hbk:' + rid}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
                equipment.tag('block', 'mineable/pickaxe', ['hbk:' + rid])
                equipment.tag('block', 'needs_stone_tool', ['hbk:' + rid])
            else:
                if role in equipment.ROLES:
                    rocks.write_json(A / f'models/item/{rid}.json', {'parent': 'minecraft:item/' + ('handheld' if role in ['pickaxe', 'sword', 'axe', 'shovel', 'hoe'] else 'generated'),
                        'textures': {'layer0': 'hbk:item/alloys/' + rid}})
                    rocks.write_json(A / f'items/{rid}.json', {'model': {'type': 'minecraft:model', 'model': 'hbk:item/' + rid}})
                else:
                    item(rid, model)
                if role in groups: equipment.tag('item', groups[role], ['hbk:' + rid])
                if role == 'pickaxe':
                    equipment.tag('item', 'parallel_ore_pickaxes', ['hbk:' + rid], 'hbk')
                    equipment.tag('item', 'cluster_max_harvestables', ['hbk:' + rid])
                if role in ['helmet', 'chestplate', 'leggings', 'boots']:
                    rocks.write_json(A / f'equipment/{name}.json', {'layers': {cat: [{'texture': 'minecraft:iron', 'dyeable': {'color_when_undyed': colour}}]
                        for cat in ['humanoid', 'humanoid_baby', 'humanoid_leggings']}})
            recipe(rid + '_smithing', {'type': 'minecraft:smithing_transform', 'template': 'minecraft:netherite_upgrade_smithing_template',
                'base': 'minecraft:' + reward['base'], 'addition': 'hbk:' + ingot, 'result': {'id': 'hbk:' + rid}}, 'hbk:' + ingot)
            for lang in langs:
                langs[lang][('block' if role == 'device' else 'item') + '.hbk.' + rid] = reward['ru' if lang == 'ru_ru' else 'en']
            craft_rows.append(f"| {a['ru']} | `{sample}` | {reward['base']} | {reward['ru']} |")
    equipment.tag('item', 'alloy_samples', ['hbk:' + a['id'] + '_sample' for a in alloys], 'hbk')
    equipment.tag('item', 'alloy_ingots', ['hbk:' + a['id'] + '_ingot' for a in alloys], 'hbk')
    equipment.tag('item', 'sample_bag_contents', ['#hbk:alloy_samples', '#hbk:alloy_ingots'] + ['hbk:' + f + '_' + r for f in rocks.FAMILIES for r in ['mineral', 'shard', 'component']], 'hbk')
    rocks.write_json(A / 'blockstates/alloy_rope_ladder.json', {'variants': {
        'facing=' + direction: {'model': 'minecraft:block/ladder', **({'y': rotation} if rotation else {})}
        for direction, rotation in [('north', 0), ('south', 180), ('west', 270), ('east', 90)]}})
    equipment.tag('block', 'climbable', ['hbk:alloy_rope_ladder'])
    equipment.tag('block', 'mineable/axe', ['hbk:alloy_rope_ladder'])
    equipment.tag('item', 'enchantable/durability', ['hbk:' + r['id'] for a in alloys for r in a['rewards'] if r['role'] not in ['device', 'minecart'] and r['id'] not in ['return_capsule', 'shaped_charge']])
    tips = {
        'gliding_chestplate': ('Удерживайте прыжок при падении. Прочность: 1/с.', 'Hold jump while falling. Durability: 1/sec.'),
        'drill_pickaxe': ('Камень 3×3. Shift: один блок. Прочность за каждый блок.', 'Mines rock 3×3. Sneak: one block. Wear per block.'),
        'mining_helmet': ('Ночное зрение. Светопыль или светящийся чернильный мешок: 5 мин.', 'Night vision. Glowstone dust or glow ink sac: 5 min.'),
        'forest_axe': ('Срубает естественный местный ствол. Shift: одно бревно.', 'Fells natural parallel trees. Sneak: one log.'),
        'field_hoe': ('Вспахивает землю 3×3. Shift: один блок.', 'Tills soil 3×3. Sneak: one block.'),
        'field_shovel': ('Прокладывает дорожки 3×3. Shift: один блок.', 'Makes paths 3×3. Sneak: one block.'),
        'entry_compass': ('Указывает на обратную печь текущего измерения.', 'Points to your return furnace in this dimension.'),
        'building_glove': ('ПКМ: опора в воздухе из полного блока в инвентаре.', 'Use: place a full inventory block in the air within reach.'),
        'field_repair_kit': ('ПКМ: ремонт предмета в другой руке его обычным материалом.', 'Use: repair the other-hand item with its normal material.'),
        'anchor_boots': ('Отбрасывание снижено на 75%. Защита от восходящего потока.', '75% less knockback. Resists updraft lift.'),
        'expedition_barometer': ('ПКМ: текущая погода и время до её изменения.', 'Use: current weather and time until it changes.'),
        'grappling_hook': ('ПКМ: подтянуться к твёрдой поверхности в пределах 16 блоков.', 'Use: pull toward a solid surface within 16 blocks.'),
        'air_tank': ('Держите в руке под водой. Запас: 60 с; заряд на поверхности.', 'Hold underwater. 60 sec reserve; recharges above water.'),
        'sail_boots': ('Отпустите и снова нажмите прыжок в воздухе. Один раз до посадки.', 'Release and press jump in midair. Once per landing.'),
        'seismic_hammer': ('ПКМ по камню: поиск подземной пустоты в радиусе 12 блоков.', 'Use on rock: find an underground void within 12 blocks.'),
        'spring_shield': ('Успешный блок ближнего удара отталкивает нападающего.', 'Blocking a melee hit pushes the attacker away.'),
        'diving_boots': ('Ускоряют движение под водой. Shift: прижаться ко дну.', 'Easier underwater movement. Sneak: hold to the bottom.'),
        'row_builder': ('ПКМ по грани: ряд до 8 блоков из инвентаря. Shift: один.', 'Use on a face: extend up to 8 inventory blocks. Sneak: one.'),
        'blast_shield': ('При блокировании снижает урон взрыва на 65%, расходуя прочность.', 'Blocking reduces explosion damage by 65%, costing durability.'),
        'gravity_chestplate': ('Ослабляет отклонения местной тяжести на 80%.', 'Reduces local gravity deviations by 80%.'),
        'tuning_fork': ('ПКМ: отталкивает видимых мобов в радиусе 5 блоков. Пауза: 5 с.', 'Use: repel visible mobs within 5 blocks. Cooldown: 5 sec.'),
        'smelting_pickaxe': ('Переплавляет подходящую рудную добычу. Сохраняет Удачу и Шёлковое касание.', 'Smelts suitable ore drops. Respects Fortune and Silk Touch.'),
        'hermetic_helmet': ('Защищает от всех газов параллельных миров.', 'Protects from every parallel-world gas.'),
        'rope_ladder_spool': ('ПКМ по стене: лестница до 24 блоков вниз. Shift+ПКМ: свернуть.', 'Use on a wall: ladder up to 24 blocks down. Sneak-use: retract.'),
        'harvest_sickle': ('ПКМ: зрелый урожай 3×3 с пересадкой. Нужны семена в инвентаре.', 'Use: harvest and replant ripe crops 3×3. Inventory seeds required.'),
        'carrying_grips': ('ПКМ: поднять одиночный сундук или бочку; ПКМ по грани: поставить.', 'Use: carry a single chest/barrel; use on a face to place it.'),
        'sample_bag': ('ПКМ: 18 слотов для местных минералов, образцов и катализаторов.', 'Use: 18 slots for parallel minerals, samples and catalysts.'),
        'miner_belt': ('В инвентаре притягивает доступную вам добычу в радиусе 5 блоков. ПКМ: переключить.', 'Attracts available loot within 5 blocks. Use: toggle.'),
        'heat_cartridge': ('ПКМ по горящей печи: забрать тепло; по холодной: передать.', 'Use on a burning furnace to capture heat; a cold one to transfer.'),
        'ore_compass': ('ПКМ по руде: выбрать образец. ПКМ: найти такую руду в радиусе 16.', 'Use on ore to select it. Use: search loaded blocks within 16.'),
        'rift_sword': ('ПКМ: рывок до 4 блоков, с остановкой у стены. Пауза: 3 с.', 'Use: dash up to 4 blocks, stopping at walls. Cooldown: 3 sec.'),
        'shaped_charge': ('ПКМ: убрать обычную породу 3×3×3; руды и контейнеры остаются.', 'Use: clear ordinary rock 3×3×3; preserves ores and containers.'),
        'return_capsule': ('ПКМ: одноразовый возврат к обратной печи текущего измерения.', 'Use: consumed to return to this dimension’s entry furnace.'),
    }
    messages = {
        'repair_material': ('Нужны повреждённый предмет в другой руке и его материал для ремонта.', 'Hold damaged equipment in the other hand and carry its repair material.'),
        'enabled': ('Устройство включено.', 'Enabled.'), 'disabled': ('Устройство выключено.', 'Disabled.'),
        'no_entrance': ('Обратная печь в этом измерении ещё не известна.', 'No known return furnace in this dimension.'),
        'blocked': ('Рядом с целью нет безопасного места.', 'No safe space near the destination.'),
        'ore_sample': ('Нажмите ПКМ по нужному рудному блоку.', 'Use on an ore block to select a sample.'),
        'not_found': ('Подходящей цели рядом нет.', 'No matching target nearby.'),
        'ordinary_weather': ('Барометр рассчитан на погоду параллельных миров.', 'This barometer forecasts parallel-world weather.'),
        'rain': ('осадки', 'rain'), 'calm': ('спокойно', 'calm'),
        'forecast': ('Сейчас: %s. Смена через %s с.', 'Now: %s. Changes in %s sec.'),
        'coordinates': ('Цель: %s, %s, %s', 'Target: %s, %s, %s'),
        'air': ('Запас воздуха: %s с', 'Air reserve: %s sec'), 'heat': ('Запас тепла: %s тиков', 'Stored heat: %s ticks'),
        'wind': ('Ветер: %s/9, направление %s°', 'Wind: %s/9, direction %s°'),
        'link_first': ('Первая плита выбрана. Shift+ПКМ по второй.', 'First plate selected. Sneak-use the second plate.'),
        'linked': ('Плиты связаны.', 'Plates linked.'), 'link_far': ('Плиты должны быть в пределах 128 блоков.', 'Plates must be within 128 blocks.'),
        'recording': ('Запись: 8 шагов, по секунде на шаг.', 'Recording 8 steps, one second each.'),
        'recycling_anvil_help': ('Снаряжение местных минералов — в первый слот. Возврат до 35%.', 'Parallel mineral equipment in the first slot. Returns up to 35%.'),
        'growth_lamp_help': ('Положите костную муку. Радиус роста: 4 блока.', 'Insert bone meal. Growth radius: 4 blocks.'),
        'air_purifier_help': ('Бумага + древесный уголь: 60 с защиты от газа в радиусе 6.', 'Paper + charcoal: 60 sec gas protection within 6 blocks.'),
        'washing_trough_help': ('Образец — в первый слот, ведро воды — рядом. Плавка вдвое быстрее.', 'Sample in the first slot; add a water bucket. Blasting is twice as fast.'),
        'drain_pump_help': ('Редстоун + соседний контейнер с отдельными пустыми вёдрами.', 'Power with redstone; adjacent container needs single empty buckets.'),
    }
    tips.update({
        'impulse_crossbow': ('Обычные стрелы становятся отталкивающими болтами.', 'Ordinary arrows become impulse bolts.'),
        'cargo_minecart': ('54 слота. Обычные рельсы и физика вагонетки.', '54 slots. Native rails and minecart physics.'),
        'recycling_anvil': messages['recycling_anvil_help'], 'growth_lamp': messages['growth_lamp_help'],
        'air_purifier': messages['air_purifier_help'], 'washing_trough': messages['washing_trough_help'],
        'drain_pump': messages['drain_pump_help'],
        'wind_vane': ('ПКМ: направление и сила ветра. Редстоун-сигнал 0–15.', 'Use: wind direction and strength. Redstone output: 0–15.'),
        'anomaly_sensor': ('Редстоун-сигнал 15 в опасном ядре локальной аномалии.', 'Outputs 15 in the dangerous core of a local anomaly.'),
        'phase_glass': ('Редстоун-сигнал делает стекло проходимым.', 'Redstone power makes the glass passable.'),
        'lift_platform': ('Одна вертикаль, до 64 блоков. Прыжок: вверх; Shift: вниз.', 'Same vertical shaft, up to 64 blocks. Jump: up; sneak: down.'),
        'transition_plate': ('Shift+ПКМ пустой рукой по двум плитам: связать в пределах 128 блоков.', 'Sneak-use two plates with an empty hand to link within 128 blocks.'),
        'rhythm_controller': ('Shift+ПКМ: запись 8 секунд. ПКМ: повторять/выключить.', 'Sneak-use: record 8 seconds. Use: play/stop.'),
        'ingredients': ('Состав: %s + %s', 'Ingredients: %s + %s'),
        'blasting': ('Плавильная печь, топливо, 10 с → слиток.', 'Blast furnace, fuel, 10 sec → ingot.'),
        'washed': ('Промытый образец: плавильная печь, 5 с → слиток.', 'Washed sample: blast furnace, 5 sec → ingot.'),
        'smithing': ('Кузнечный стол + шаблон незеритового улучшения:', 'Smithing table + netherite upgrade template:'),
        'upgrade': ('%s → %s', '%s → %s'),
    })
    for lang, index in [('ru_ru', 0), ('en_us', 1)]:
        for key, values in tips.items(): langs[lang]['tooltip.hbk.alloy.' + key] = values[index]
        for key, values in messages.items(): langs[lang]['message.hbk.alloy.' + key] = values[index]
    for f, family in enumerate(rocks.FAMILIES):
        langs['ru_ru']['tag.item.hbk.alloy_materials.' + family] = 'Материалы для сплавов: ' + rocks.RU[f].lower()
        langs['en_us']['tag.item.hbk.alloy_materials.' + family] = family.title() + ' Alloy Materials'
    for key, ru, en in [('alloy_samples', 'Образцы сплавов', 'Alloy Samples'), ('alloy_ingots', 'Слитки сплавов', 'Alloy Ingots'), ('sample_bag_contents', 'Содержимое сумки образцов', 'Sample Bag Contents')]:
        langs['ru_ru']['tag.item.hbk.' + key] = ru
        langs['en_us']['tag.item.hbk.' + key] = en
    equipment.tag('item', 'enchantable/crossbow', ['hbk:impulse_crossbow'])
    langs['ru_ru']['book.hbk.recipes.info.universe_alloys'] = 'Сплавы\n\n2 разные руды или минерала → образец.\n\nПлавильная печь → слиток.\n\nКузнечный стол: шаблон улучшения + незерит + сплав.'
    langs['en_us']['book.hbk.recipes.info.universe_alloys'] = 'Alloys\n\n2 different ores or minerals → sample.\n\nBlast furnace → ingot.\n\nSmithing: upgrade template + netherite + alloy.'
    for lang, data in langs.items(): rocks.write_language(A / f'lang/{lang}.json', data)
    (REPO / 'docs/UNIVERSE_ALLOY_RECIPES.md').write_text('# Изготовление сплавов\n\nВсе 45 пар симметричны. По одной руде **или** минералу каждого семейства дают один образец. Только плавильная печь: 200 тиков и топливо → один слиток. Каждый предмет требует один шаблон незеритового улучшения, указанную незеритовую основу и один слиток. Имя и зачарования основы сохраняются. Блок хранения = 9 слитков, разбирается обратно без потерь.\n\n| Сплав | Образец (namespace hbk) | Незеритовая основа | Результат |\n| --- | --- | --- | --- |\n' + '\n'.join(craft_rows) + '\n')
    print(f'Generated {len(alloys)} alloys, {len(craft_rows)} upgrades, {len(recipes)} recipes; original 16px metal art and native item geometry.')


if __name__ == '__main__': main()
