#!/usr/bin/env python3
"""Family mining supplies; source-only exporter, standard Python, no private paths."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/data/hbk/loot_table/chests'
FAMILIES = ['primordial', 'celestial', 'columnar', 'cavernous', 'woven', 'sedimentary', 'labyrinthine', 'fragmented', 'inverse', 'stratified']


def entry(name, weight=1, minimum=1, maximum=1):
    result = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    if maximum != 1:
        result['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': minimum, 'max': maximum}}]
    return result


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    for family in FAMILIES:
        pools = [
            {'rolls': 1, 'entries': [entry('hbk:' + family + '_mineral', minimum=2, maximum=5)]},
            {'rolls': {'type': 'minecraft:uniform', 'min': 4, 'max': 7}, 'entries': [
                entry('minecraft:torch', 10, 4, 12), entry('minecraft:coal', 8, 2, 6),
                entry('minecraft:raw_iron', 7, 2, 5), entry('minecraft:rail', 8, 4, 12),
                entry('minecraft:bread', 7, 2, 4), entry('minecraft:paper', 5, 2, 5),
                entry('minecraft:charcoal', 5, 2, 4), entry('minecraft:glowstone_dust', 4, 1, 3),
                entry('minecraft:redstone', 6, 2, 6)]},
            {'rolls': 1, 'entries': [entry('minecraft:diamond', 2, 1, 2),
                entry('minecraft:name_tag', 2), entry('hbk:' + family + '_component', 4),
                entry('minecraft:gold_ingot', 5, 1, 3), {'type': 'minecraft:empty', 'weight': 12}]}
        ]
        (ROOT / f'parallel_mine_{family}.json').write_text(json.dumps({'type': 'minecraft:chest', 'pools': pools}, indent=2) + '\n')


if __name__ == '__main__':
    main()
