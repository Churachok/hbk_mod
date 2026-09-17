#!/usr/bin/env python3
"""Generate pixel textures for the stew and nurse update."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/hbk/textures"
T = (0, 0, 0, 0)


def canvas(size: int) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGBA", (size, size), T)
    return image, ImageDraw.Draw(image)


def save(image: Image.Image, group: str, name: str) -> None:
    folder = TEXTURES / group
    folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f"{name}.png")


def stew() -> Image.Image:
    image, d = canvas(16)
    outline, dark, olive, rust, steel, shine = "#292b28", "#4b4936", "#696746", "#9a4c2c", "#a7a89c", "#e4dfc8"
    d.ellipse((2, 2, 13, 6), fill=outline)
    d.rectangle((2, 4, 13, 13), fill=outline)
    d.ellipse((2, 10, 13, 15), fill=outline)
    d.rectangle((3, 5, 12, 12), fill=olive)
    d.ellipse((3, 2, 12, 5), fill=steel)
    d.ellipse((4, 3, 11, 4), fill=dark)
    d.ellipse((3, 11, 12, 14), fill=dark)
    d.rectangle((4, 6, 5, 11), fill="#8c8a61")
    d.rectangle((11, 6, 12, 10), fill="#4b4c35")
    d.point((5, 4), fill=shine)
    for point in ((4, 8), (9, 6), (10, 11), (6, 12)):
        d.point(point, fill=rust)
    return image


def bandage() -> Image.Image:
    image, d = canvas(16)
    outline, shade, white, light = "#59595c", "#bcb8b2", "#e9e5dc", "#fffdf3"
    d.ellipse((2, 3, 10, 11), fill=outline)
    d.rectangle((6, 3, 12, 11), fill=outline)
    d.ellipse((3, 4, 9, 10), fill=white)
    d.ellipse((5, 6, 8, 9), fill=shade)
    d.ellipse((6, 7, 7, 8), fill=outline)
    d.rectangle((7, 4, 11, 10), fill=white)
    d.rectangle((8, 11, 14, 13), fill=outline)
    d.rectangle((7, 10, 13, 12), fill=white)
    d.line((8, 5, 8, 9), fill=light)
    d.line((8, 11, 12, 11), fill=shade)
    return image


def effect_icon(kind: str) -> Image.Image:
    image, d = canvas(18)
    if kind == "expired":
        d.ellipse((3, 2, 14, 15), fill="#6f7048", outline="#303126")
        d.rectangle((5, 6, 12, 12), fill="#99956a")
        d.line((4, 4, 13, 13), fill="#b94a3d", width=2)
        d.point((8, 3), fill="#d7d19b")
    elif kind == "hearty_lunch":
        d.ellipse((1, 7, 16, 15), fill="#ded3b5", outline="#5b4c3e")
        d.ellipse((3, 8, 14, 13), fill="#9d633a")
        for point in ((5, 9), (8, 10), (11, 9), (6, 12), (12, 11)):
            d.point(point, fill="#e0bb72")
        d.rectangle((11, 3, 15, 8), fill="#626448", outline="#303126")
        d.rectangle((3, 1, 5, 6), fill="#f5cb58")
        d.rectangle((1, 3, 7, 5), fill="#f5cb58")
    else:
        d.polygon([(3, 3), (8, 1), (14, 5), (13, 14), (5, 16), (2, 11)], fill="#555b68", outline="#272b33")
        d.rectangle((6, 4, 10, 12), fill="#737b8d")
        d.polygon([(12, 11), (16, 11), (14, 16)], fill="#2f3440")
        d.line((1, 5, 5, 5), fill="#9aa4b9")
        d.line((0, 8, 4, 8), fill="#9aa4b9")
    return image


def nurse_spawn_egg() -> Image.Image:
    image, d = canvas(16)
    d.ellipse((3, 1, 12, 14), fill="#d9dce1", outline="#4c5158")
    d.ellipse((5, 2, 10, 7), fill="#fafafa")
    d.rectangle((7, 4, 8, 9), fill="#b73232")
    d.rectangle((5, 6, 10, 7), fill="#b73232")
    for point in ((5, 10), (10, 11), (6, 13), (4, 7)):
        d.point(point, fill="#8d939d")
    return image


def nurse_skin() -> Image.Image:
    image = Image.new("RGBA", (64, 64), T)
    d = ImageDraw.Draw(image)
    skin, skin_shadow = "#e7a17d", "#c77a60"
    hair, hair_light = "#4b2e24", "#6b4030"
    white, cloth, shade = "#f2f3ef", "#d9dde0", "#9099a3"
    dark, red = "#30343b", "#b83232"

    # Head faces (standard 64x64 humanoid UV layout).
    for box in ((8, 0, 15, 7), (16, 0, 23, 7), (0, 8, 7, 15), (8, 8, 15, 15), (16, 8, 23, 15), (24, 8, 31, 15)):
        d.rectangle(box, fill=hair)
    d.rectangle((8, 8, 15, 15), fill=skin)
    d.rectangle((8, 8, 15, 10), fill=hair)
    d.rectangle((8, 11, 9, 14), fill=hair_light)
    d.rectangle((14, 11, 15, 14), fill=hair)
    d.point((10, 12), fill=dark)
    d.point((13, 12), fill=dark)
    d.line((11, 14, 12, 14), fill=skin_shadow)
    # White nurse cap on the hat overlay, with a small red medical mark.
    d.rectangle((40, 8, 47, 11), fill=white)
    d.rectangle((43, 9, 44, 11), fill=red)
    d.rectangle((42, 10, 45, 10), fill=red)

    # Torso and coat.
    for box in ((20, 16, 27, 19), (28, 16, 35, 19), (16, 20, 19, 31), (20, 20, 27, 31), (28, 20, 31, 31), (32, 20, 39, 31)):
        d.rectangle(box, fill=cloth)
    d.rectangle((20, 20, 27, 31), fill=white)
    d.line((23, 20, 23, 31), fill=shade)
    d.point((25, 23), fill=dark)
    d.point((25, 27), fill=dark)
    d.rectangle((21, 22, 22, 25), fill=red)
    d.rectangle((20, 23, 23, 24), fill=red)

    # Right arm.
    for box in ((44, 16, 47, 19), (48, 16, 51, 19), (40, 20, 43, 31), (44, 20, 47, 31), (48, 20, 51, 31), (52, 20, 55, 31)):
        d.rectangle(box, fill=cloth)
    d.rectangle((44, 20, 47, 28), fill=white)
    d.rectangle((44, 29, 47, 31), fill=skin)

    # Left arm.
    for box in ((36, 48, 39, 51), (40, 48, 43, 51), (32, 52, 35, 63), (36, 52, 39, 63), (40, 52, 43, 63), (44, 52, 47, 63)):
        d.rectangle(box, fill=cloth)
    d.rectangle((36, 52, 39, 60), fill=white)
    d.rectangle((36, 61, 39, 63), fill=skin)

    # Legs: dark trousers and shoes.
    for box in ((4, 16, 7, 19), (8, 16, 11, 19), (0, 20, 3, 31), (4, 20, 7, 31), (8, 20, 11, 31), (12, 20, 15, 31),
                (20, 48, 23, 51), (24, 48, 27, 51), (16, 52, 19, 63), (20, 52, 23, 63), (24, 52, 27, 63), (28, 52, 31, 63)):
        d.rectangle(box, fill=dark)
    d.rectangle((4, 20, 7, 26), fill="#66554e")
    d.rectangle((20, 52, 23, 58), fill="#66554e")
    return image


def main() -> None:
    save(stew(), "item", "stew")
    save(bandage(), "item", "bandage")
    save(nurse_spawn_egg(), "item", "nurse_spawn_egg")
    save(effect_icon("expired"), "mob_effect", "expired")
    save(effect_icon("hearty_lunch"), "mob_effect", "hearty_lunch")
    save(effect_icon("heaviness"), "mob_effect", "heaviness")
    save(nurse_skin(), "entity", "nurse")
    print("Generated stew and nurse update textures")


if __name__ == "__main__":
    main()
