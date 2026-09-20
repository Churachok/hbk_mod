#!/usr/bin/env python3
"""Build NPC UV textures with the same pixel skin workflow as the existing mobs.

The sheets in /home/kirill/refer_mods are visual references, not usable UV maps.
This keeps the palette and character details in a reproducible source format.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw
from generate_kirill_texture import textured_rect, paint_limb, shade

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/hbk"
SKIN = (242, 227, 178, 255)
WHITE = (248, 248, 248, 255)
BLACK = (15, 15, 15, 255)
SPECS = {
    "anton": ((176, 116, 77, 255), (216, 42, 46, 255), (91, 91, 91, 255)),
    "denis": ((235, 235, 235, 255), (108, 108, 108, 255), (184, 184, 184, 255)),
    "gosha": ((20, 20, 20, 255), (18, 18, 18, 255), (28, 28, 28, 255)),
    "grisha": ((250, 198, 25, 255), (218, 38, 45, 255), (236, 236, 236, 255)),
    "lesha": ((164, 108, 74, 255), (238, 147, 184, 255), (108, 108, 108, 255)),
    "sasha": ((249, 191, 22, 255), (164, 164, 168, 255), (89, 89, 89, 255)),
    "vlad": ((174, 120, 83, 255), (183, 175, 224, 255), (106, 106, 106, 255)),
}


def skin(name, hair, shirt, trousers):
    image = Image.new("RGBA", (64, 64))
    draw = ImageDraw.Draw(image)
    face = (177, 117, 80, 255) if name == "gosha" else SKIN
    for seed, (box, color) in enumerate([
        ((8, 0, 15, 7), hair), ((16, 0, 23, 7), face),
        ((0, 8, 7, 15), hair), ((8, 8, 15, 15), face),
        ((16, 8, 23, 15), hair), ((24, 8, 31, 15), hair),
    ]):
        textured_rect(image, box, color, seed)
    draw.rectangle((8, 8, 15, 9), fill=hair)
    draw.point((8, 10), fill=hair)
    draw.point((15, 10), fill=hair)
    for x in (1, 17):
        draw.rectangle((x, 13, x + 4, 15), fill=face)
    if name in ("lesha", "grisha"):
        draw.rectangle((8, 10, 9, 14), fill=hair)
        draw.rectangle((15, 10, 15, 14), fill=hair)
    if name == "sasha":
        draw.rectangle((8, 10, 11, 15), fill=hair)
    for eye in (9, 13):
        draw.rectangle((eye, 11, eye + 1, 12), fill=WHITE)
        draw.point((eye + 1, 11 if name in ("gosha", "grisha", "sasha") else 12), fill=BLACK)
    if name == "denis":
        draw.rectangle((13, 11, 14, 12), fill=BLACK)
        draw.point((14, 12), fill=(224, 24, 24, 255))
    draw.line((11, 14, 12, 14), fill=(62, 49, 34, 255))
    if name == "vlad":
        draw.point((13, 14), fill=BLACK)
        draw.point((12, 15), fill=BLACK)
    if name == "grisha":
        for x in (10, 12, 14):
            draw.point((x, 15), fill=shade(hair, -30))
    # Raised hair shell, with transparent face and no opaque hat over the eyes.
    textured_rect(image, (40, 0, 47, 7), hair, 60)
    for box in [(32, 8, 39, 11), (48, 8, 55, 11), (56, 8, 63, 14)]:
        textured_rect(image, box, hair, 61)
    draw.rectangle((40, 8, 47, 9), fill=hair)
    if name in ("sasha", "grisha", "lesha"):
        draw.rectangle((40, 10, 41, 14), fill=hair)
        draw.rectangle((47, 10, 47, 14), fill=hair)
    for seed, box in enumerate([(20, 16, 27, 19), (28, 16, 35, 19),
                               (16, 20, 19, 31), (20, 20, 27, 31),
                               (28, 20, 31, 31), (32, 20, 39, 31)], 10):
        textured_rect(image, box, shirt, seed)
    draw.rectangle((23, 20, 24, 20), fill=face)
    if name == "anton":
        draw.rectangle((23, 21, 24, 30), fill=WHITE)
        draw.rectangle((22, 30, 25, 31), fill=WHITE)
    if name == "grisha":
        # Pixel hammer and sickle, positioned on the front of the red shirt.
        gold = (255, 218, 30, 255)
        draw.line((22, 24, 26, 28), fill=gold, width=1)
        draw.line((24, 23, 22, 25), fill=gold, width=2)
        draw.line([(25, 23), (26, 24), (26, 26), (25, 27), (23, 27), (22, 26)], fill=gold)
    shoes = (130, 132, 135, 255) if name == "grisha" else WHITE
    if name == "gosha":
        shoes = BLACK
    paint_limb(image, 40, 16, shirt, face, 2, 20)
    paint_limb(image, 32, 48, shirt, face, 2, 30)
    paint_limb(image, 0, 16, trousers, shoes, 3, 40)
    paint_limb(image, 16, 48, trousers, shoes, 3, 50)
    return image


def lex_texture():
    orange = (229, 119, 17, 255)
    image = Image.new("RGBA", (64, 32), orange)
    textured_rect(image, (0, 0, 63, 31), orange, 90)
    draw = ImageDraw.Draw(image)
    # AdultFelineModel head: 5x4x5 at UV (0,0); the front is (5,5)..(9,8).
    for x in (5, 8):
        draw.rectangle((x, 6, x + 1, 7), fill=(44, 158, 32, 255))
        draw.line((x + 1, 6, x + 1, 7), fill=BLACK)
    # Nose box at UV (0,24), pink centre of the muzzle.
    draw.point((3, 26), fill=(245, 131, 169, 255))
    # All four legs share the front/hind leg UVs; black paws on each side.
    draw.rectangle((8, 19, 15, 20), fill=BLACK)
    draw.rectangle((40, 10, 47, 11), fill=BLACK)
    # Distal tail segment UV (4,15), final three pixels and bottom face.
    draw.rectangle((4, 21, 7, 23), fill=BLACK)
    draw.point((6, 15), fill=BLACK)
    return image


def egg(primary, secondary):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(6, 1), (9, 1), (12, 5), (13, 10), (11, 14), (4, 14), (2, 10), (3, 5)], fill=primary)
    for box in [(5, 3, 6, 5), (9, 6, 11, 8), (4, 9, 6, 11), (8, 12, 9, 13)]:
        draw.rectangle(box, fill=secondary)
    return image


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent="\t") + "\n")


def main():
    (ASSETS / "textures/entity").mkdir(parents=True, exist_ok=True)
    for name, spec in SPECS.items():
        skin(name, *spec).save(ASSETS / f"textures/entity/{name}.png")
    lex_texture().save(ASSETS / "textures/entity/lex.png")
    drops = {
        "anton": [("buckwheat", 1.0)], "denis": [("jaw_member", 0.2)],
        "gosha": [("beastlike_member", 0.2)],
        "grisha": [("hammer_fighter_member", 0.2), ("sickle_and_hammer", 0.2)],
        "lesha": [("colossal_member", 0.2)], "sasha": [("armored_member", 0.2)],
        "vlad": [("carrier_member", 0.2)], "lex": [],
    }
    for name, entries in drops.items():
        pools = []
        for item, chance in entries:
            entry = {"type": "minecraft:item", "name": f"hbk:{item}"}
            if chance < 1.0:
                entry["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
            pools.append({"rolls": 1.0, "entries": [entry]})
        write_json(ROOT / f"data/hbk/loot_table/entities/{name}.json", {
            "type": "minecraft:entity", "pools": pools, "random_sequence": f"hbk:entities/{name}"})
        primary, secondary = SPECS[name][:2] if name in SPECS else ((229, 119, 17, 255), BLACK)
        egg(primary, secondary).save(ASSETS / f"textures/item/{name}_spawn_egg.png")
        write_json(ASSETS / f"models/item/{name}_spawn_egg.json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": f"hbk:item/{name}_spawn_egg"}})
        write_json(ASSETS / f"items/{name}_spawn_egg.json", {
            "model": {"type": "minecraft:model", "model": f"hbk:item/{name}_spawn_egg"}})
    ru_names = dict(zip(drops, ["Антон", "Денис", "Гоша", "Гриша", "Лёша", "Саша", "Влад", "Лекс"]))
    for locale in ("en_us", "ru_ru"):
        path = ASSETS / f"lang/{locale}.json"
        data = json.loads(path.read_text())
        for name in drops:
            title = ru_names[name] if locale == "ru_ru" else name.capitalize()
            data[f"entity.hbk.{name}"] = title
            data[f"item.hbk.{name}_spawn_egg"] = f"Яйцо призыва: {title}" if locale == "ru_ru" else f"{title} Spawn Egg"
        write_json(path, data)


if __name__ == "__main__":
    main()
