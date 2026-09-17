#!/usr/bin/env python3
"""Generate the small material swatches used by the block-built weapon model."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ITEMS = ROOT / "src/main/resources/assets/hbk/textures/item"


def swatch(name: str, palette: tuple[tuple[int, int, int, int], ...]) -> None:
    image = Image.new("RGBA", (16, 16))
    pixels = image.load()
    for y in range(16):
        for x in range(16):
            # Quiet pixel variation keeps large cube faces recognisably Minecraft-like.
            index = ((x // 4) + (y // 4) * 3 + (1 if (x + y) % 7 == 0 else 0)) % len(palette)
            pixels[x, y] = palette[index]
    ITEMS.mkdir(parents=True, exist_ok=True)
    image.save(ITEMS / f"{name}.png")


def main() -> None:
    swatch("attacking_member_flesh", (
        (213, 185, 171, 255), (224, 199, 186, 255), (198, 164, 151, 255), (235, 211, 198, 255),
    ))
    swatch("attacking_member_flesh_dark", (
        (172, 132, 123, 255), (190, 149, 138, 255), (155, 113, 108, 255), (205, 165, 151, 255),
    ))
    swatch("attacking_member_tip", (
        (105, 53, 72, 255), (124, 62, 82, 255), (88, 42, 62, 255), (139, 73, 92, 255),
    ))
    print("Generated Attacking Member block-model textures")


if __name__ == "__main__":
    main()
