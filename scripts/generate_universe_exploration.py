#!/usr/bin/env python3
"""Exploration loot, bilingual clues and unstable natural rocks. Standard library only."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
FAMILIES = ['primordial', 'celestial', 'columnar', 'cavernous', 'woven', 'sedimentary', 'labyrinthine', 'fragmented', 'inverse', 'stratified']
SITES = {
    'research_complex': ('Исследовательский комплекс', 'Research complex', 'minecraft:brewing_stand', 'Полевой дистиллятор', 'Field distiller'),
    'watchtower': ('Башня наблюдателей', 'Watchtower', 'minecraft:spyglass', 'Линза смотрителя', "Warden's lens"),
    'underground_lab': ('Подземная лаборатория', 'Underground laboratory', 'minecraft:experience_bottle', 'Нестабильный опыт', 'Unstable experiment'),
    'ruined_city': ('Разрушенный город', 'Ruined city', 'minecraft:golden_carrot', 'Паёк последнего жителя', "Last citizen's ration"),
    'ring_machine': ('Кольцевой механизм', 'Ring machine', 'minecraft:comparator', 'Синхронизатор пустоты', 'Void synchronizer'),
    'secret': ('Секретное хранилище', 'Secret vault', 'minecraft:diamond', 'Неучтённый резерв', 'Unregistered reserve'),
}
LANDMARKS = {
    'fissure': ('Километровый разлом', 'Kilometre fissure'),
    'monoliths': ('Парящие монолиты', 'Floating monoliths'),
    'crystal_forest': ('Кристаллический лес', 'Crystal forest'),
    'skeleton': ('Скелет неизвестного', 'Unknown skeleton'),
    'petrified_wave': ('Окаменевшая волна', 'Petrified wave'),
    'world_eye': ('Каменный глаз мира', 'Stone eye of the world'),
}
LOGS = {
    'research_complex': ('Основоположник межпространственной теории — доктор Лекс. Говорят, имя Менгель — его псевдоним.\n\nЛекс родом из Израиля. По слухам из родной вселенной 04228, с виду он обычный рыжий говорящий кот.',
                         'Dr. Lex founded interdimensional theory. Rumour says Mengel is his alias.\n\nLex comes from Israel. Witnesses in his home universe, 04228, describe an ordinary ginger cat that can speak.'),
    'watchtower': ('Последний раз Лекса видели с красивой фиолетоволосой девушкой.\n\nПозже её нашли в 04228, родной вселенной доктора. На связь с Лексом выйти не удалось. Его дальнейшая судьба неизвестна.',
                   'Lex was last seen with a beautiful purple-haired girl.\n\nShe was later found in 04228, his home universe. Attempts to contact Lex failed. His fate remains unknown.'),
    'underground_lab': ('Межпространственные экспедиции стали нормой. Группы учёных из разных вселенных путешествуют, строят лаборатории, исследуют миры и уходят дальше.\n\nТак лаборатории становятся заброшенными.',
                        'Interdimensional expeditions are now routine. Teams of scientists from different universes travel, build laboratories, study worlds and move on.\n\nThat is how laboratories become abandoned.'),
    'ruined_city': ('Учёные зашли в тупик. Их версия: первые прыжки Лекса породили лишь одну аномалию. Она уничтожала вселенные и создавала новые.\n\nГде она, неизвестно. Может, Лекс её устранил. Поиски идут, но кажутся бесполезными.',
                    'Research has stalled. Scientists believe the first jumps created just one anomaly. It destroyed universes, creating new ones.\n\nIts location is unknown. Lex may have stopped it. The search goes on, but seems futile.'),
    'ring_machine': ('По исследованиям Лекса построили машину для прыжков во времени — по ветвям основной вселенной.\n\nЕго переходы создавали новые вселенные. Аномалии в них дали толчок открытиям местных учёных.',
                     "Lex's research led to a machine that jumped through time, along branches of the main universe.\n\nHis travels created new universes. The anomalies there spurred discoveries by their own scientists."),
    'secret': ('Одна закономерность всё же найдена: примитивные машины принимают адрес из пяти цифр.\n\nИх порядок:\n1 — экосистема;\n2 — рельеф;\n3 — геология;\n4 — распределение материи;\n5 — гидрология.',
               'One pattern has been found: primitive machines accept a five-digit address.\n\nFrom left to right:\n1 — ecosystem;\n2 — terrain;\n3 — geology;\n4 — matter distribution;\n5 — hydrology.'),
}
BOOK_TITLES = {
    'research_complex': ('I. Доктор с двумя именами', 'I. The Doctor with Two Names'),
    'ring_machine': ('II. Ветви времени', 'II. Branches of Time'),
    'underground_lab': ('III. Экспедиционный век', 'III. The Age of Expeditions'),
    'watchtower': ('IV. Последний след', 'IV. The Last Sighting'),
    'ruined_city': ('V. Первая аномалия', 'V. The First Anomaly'),
    'secret': ('VI. Пять цифр', 'VI. Five Digits'),
}

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def entry(name, low=1, high=1, weight=1, functions=None):
    fs = [] if functions is None else functions[:]
    if low != 1 or high != 1:
        fs.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': low, 'max': high}})
    return {'type': 'minecraft:item', 'name': name, 'weight': weight, **({'functions': fs} if fs else {})}

def main():
    labels = {lang: json.loads((RES / f'assets/hbk/lang/{lang}.json').read_text()) for lang in ['ru_ru', 'en_us']}
    def text(key, ru, en):
        labels['ru_ru'][key] = ru
        labels['en_us'][key] = en
    for kind, (ru, en, special, reward_ru, reward_en) in SITES.items():
        text('structure.hbk.' + kind, ru, en)
        text('loot.hbk.' + kind, reward_ru, reward_en)
        text('loot.hbk.clue.' + kind,
             'Архив HBK: ищите треснувшую кладку, шахту обслуживания и скрытое хранилище. Крыши защищают от бурь.',
             'HBK archive: look for cracked masonry, the service shaft and a hidden vault. Roofs protect from storms.')
        text('book.hbk.expedition.' + kind, *LOGS[kind])
        text('book.hbk.expedition.title.' + kind, *BOOK_TITLES[kind])
        text('book.hbk.expedition.shelter',
             'Перед бурей есть 10 секунд. Гравитационная буря длится минуту. Крыши защищают от неё и осадков. Герметичный шлем спасает от кислоты и пепла; любой шлем смягчает град.',
             'Storms give 10 seconds of warning. Gravity storms last a minute. Roofs protect from storms and precipitation. A sealed helmet stops acid and ash; any helmet softens hail.')
        for f, family in enumerate(FAMILIES):
            named = [{'function': 'minecraft:set_name', 'name': {'translate': 'loot.hbk.' + kind}},
                     {'function': 'minecraft:set_lore', 'mode': 'append', 'lore': [{'translate': 'loot.hbk.clue.' + kind}]}]
            reward = entry(special, 1, 3 if kind in ['underground_lab', 'ruined_city', 'secret'] else 1, functions=named)
            tables = {'type': 'minecraft:chest', 'pools': [
                {'rolls': 1, 'entries': [entry('hbk:' + family + '_mineral', 4, 10)]},
                {'rolls': 1, 'entries': [reward]},
                {'rolls': 1, 'entries': [entry('minecraft:written_book', functions=[
                    # Native book covers store a string, not a Component. Keep a readable
                    # Russian cover and a translatable display name that takes precedence.
                    {'function': 'minecraft:set_book_cover', 'title': BOOK_TITLES[kind][0], 'author': 'HBK', 'generation': 0},
                    {'function': 'minecraft:set_name', 'target': 'custom_name',
                     'name': {'translate': 'book.hbk.expedition.title.' + kind, 'italic': False}},
                    {'function': 'minecraft:set_written_book_pages', 'mode': 'replace_all', 'pages': [
                        {'translate': 'book.hbk.expedition.' + kind}, {'translate': 'book.hbk.expedition.shelter'}]}])]},
                {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6}, 'entries': [
                    entry('minecraft:bread', 2, 5, 8), entry('minecraft:torch', 6, 16, 8),
                    entry('minecraft:iron_ingot', 2, 5, 5), entry('minecraft:redstone', 3, 8, 5),
                    entry('hbk:' + family + '_component', 1, 2, 2), entry('minecraft:paper', 2, 5, 3),
                    entry('minecraft:gold_ingot', 1, 3, 3), entry('minecraft:ender_pearl', 1, 2, 1)]},
            ]}
            if kind == 'secret':
                tables['pools'].append({'rolls': 1, 'entries': [entry('minecraft:enchanted_book', functions=[{
                    'function': 'minecraft:enchant_with_levels', 'levels': 24}])]})
            write(RES / f'data/hbk/loot_table/chests/universe_{kind}_{family}.json', tables)
    for kind, (ru, en) in LANDMARKS.items():
        text('landmark.hbk.' + kind, ru, en)
    for key, ru, en in [
        ('acid', 'Кислотные осадки', 'Acid precipitation'), ('ash', 'Пеплопад', 'Ashfall'),
        ('crystal', 'Кристаллический град', 'Crystal hail'), ('electric', 'Электрическая буря', 'Electrical storm')]:
        text('disaster.hbk.' + key, ru, en)
    for key, ru, en in [
        ('gravity_warning', 'Гравитационная буря через %s с. Найдите крышу!', 'Gravity storm in %s s. Find a roof!'),
        ('gravity_active', 'Гравитационная буря: ещё %s с. Укрытие защищает.', 'Gravity storm: %s s left. Roofs protect you.'),
        ('weather_warning', '%s через %s с. Найдите укрытие!', '%s in %s s. Find shelter!'),
        ('weather_active', '%s: ещё %s с. Укрытие защищает.', '%s: %s s left. Shelter protects you.'),
        ('forecast_gravity', 'Гравитационная буря: %s с до следующей фазы.', 'Gravity storm: %s s until the next phase.'),
        ('forecast_weather', '%s: %s с до следующей фазы.', '%s: %s s until the next phase.')]:
        text('message.hbk.disaster.' + key, ru, en)
    text('message.hbk.universe.site', '%s: %s, %s, %s', '%s: %s, %s, %s')
    text('block.hbk.cargo_crate', 'Грузовые ящики', 'Cargo Crates')
    text('commands.hbk.anomaly.overworld', 'Аномалии находятся в обычном мире и параллельных вселенных', 'Anomalies exist in the Overworld and parallel universes')
    for lang, content in labels.items():
        # Existing translations use tabs. Restore that formatting without changing their values.
        path = RES / f'assets/hbk/lang/{lang}.json'
        path.write_text(json.dumps(content, ensure_ascii=False, indent='\t') + '\n')
    path = RES / 'data/hbk/tags/block/anomaly_unstable.json'
    tag = json.loads(path.read_text())
    for family in FAMILIES:
        for role in ['stone', 'surface', 'deep', 'loam']:
            block = 'hbk:' + family + '_' + role
            if block not in tag['values']:
                tag['values'].append(block)
    write(path, tag)

if __name__ == '__main__':
    main()
