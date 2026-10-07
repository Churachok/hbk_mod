#!/usr/bin/env python3
"""Generate vanilla recipe-book unlocks from HBK's recipe JSON files."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RECIPES = ROOT / "src/main/resources/data/hbk/recipe"
ADVANCEMENTS = ROOT / "src/main/resources/data/hbk/advancement/recipes"


def discovery_item(recipe):
    if recipe["type"] == "minecraft:smelting":
        return recipe["ingredient"]
    if recipe["type"] == "minecraft:smithing_transform":
        return recipe["base"]
    if recipe["type"] == "minecraft:crafting_shaped":
        return next(iter(recipe["key"].values()))
    return recipe["ingredients"][0]


def main():
    ADVANCEMENTS.mkdir(parents=True, exist_ok=True)
    for source in sorted(RECIPES.glob("*.json")):
        recipe = json.loads(source.read_text(encoding="utf-8"))
        recipe_id = f"hbk:{source.stem}"
        advancement = {
            "parent": "minecraft:recipes/root",
            "criteria": {
                "has_ingredient": {
                    "trigger": "minecraft:inventory_changed",
                    "conditions": {"items": [{"items": discovery_item(recipe)}]},
                },
                "has_the_recipe": {
                    "trigger": "minecraft:recipe_unlocked",
                    "conditions": {"recipe": recipe_id},
                },
            },
            "requirements": [["has_ingredient", "has_the_recipe"]],
            "rewards": {"recipes": [recipe_id]},
        }
        target = ADVANCEMENTS / source.name
        target.write_text(json.dumps(advancement, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
