#!/usr/bin/env python3
"""Generate the repetitive JSON models, item definitions, blockstates and loot tables."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/hbk"
DATA = ROOT / "data/hbk"

SOLID = [
    "infected_dirt", "radioactive_stone", "ash_soil", "cracked_radioactive_stone",
    "uranium_ore", "sediment_mud", "toxic_water", "scorched_stone", "radioactive_sand",
]
PLANTS = ["dead_grass", "infected_bush", "glowing_mushroom"]
ALL = SOLID + PLANTS + ["moldy_moss", "dry_log"]


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    for name in ALL:
        if name == "dry_log":
            blockstate = {"variants": {
                "axis=x": {"model": "hbk:block/dry_log_horizontal", "x": 90, "y": 90},
                "axis=y": {"model": "hbk:block/dry_log"},
                "axis=z": {"model": "hbk:block/dry_log_horizontal", "x": 90},
            }}
        else:
            blockstate = {"variants": {"": {"model": f"hbk:block/{name}"}}}
        write(ASSETS / "blockstates" / f"{name}.json", blockstate)

        if name in PLANTS:
            block_model = {
                "parent": "minecraft:block/cross",
                "textures": {"cross": f"hbk:block/{name}"},
            }
            item_model = {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"hbk:block/{name}"},
            }
        elif name == "moldy_moss":
            block_model = {
                "parent": "minecraft:block/carpet",
                "textures": {"wool": "hbk:block/moldy_moss"},
            }
            item_model = {"parent": "hbk:block/moldy_moss"}
        elif name == "dry_log":
            block_model = {
                "parent": "minecraft:block/cube_column",
                "textures": {
                    "side": "hbk:block/dry_log",
                    "end": "hbk:block/dry_log_top",
                },
            }
            item_model = {"parent": "hbk:block/dry_log"}
            write(ASSETS / "models/block/dry_log_horizontal.json", {
                "parent": "minecraft:block/cube_column_horizontal",
                "textures": {
                    "side": "hbk:block/dry_log",
                    "end": "hbk:block/dry_log_top",
                },
            })
        else:
            block_model = {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"hbk:block/{name}"},
            }
            if name == "toxic_water":
                block_model["textures"]["all"] = {
                    "sprite": "hbk:block/toxic_water",
                    "force_translucent": True,
                }
            item_model = {"parent": f"hbk:block/{name}"}

        write(ASSETS / "models/block" / f"{name}.json", block_model)
        write(ASSETS / "models/item" / f"{name}.json", item_model)
        write(ASSETS / "items" / f"{name}.json", {
            "model": {"type": "minecraft:model", "model": f"hbk:item/{name}"}
        })
        write(DATA / "loot_table/blocks" / f"{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"hbk:{name}"}],
                "rolls": 1.0,
            }],
            "random_sequence": f"hbk:blocks/{name}",
        })

    print(f"Generated {len(ALL)} wasteland resource sets")


if __name__ == "__main__":
    main()
