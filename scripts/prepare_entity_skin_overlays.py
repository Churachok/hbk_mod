#!/usr/bin/env python3
"""Keep the supplied skin pixels and restore transparency in head overlays."""

from pathlib import Path
from statistics import median
from subprocess import run
import sys


NAMES = "anton denis gosha grisha kirill lesha lex liza nurse sasha vlad".split()


def rgba(source: Path, width: int, height: int) -> bytearray:
    pixels = bytearray(run(
        ["magick", str(source), "-depth", "8", "rgba:-"],
        capture_output=True, check=True,
    ).stdout)
    if len(pixels) != width * height * 4:
        raise ValueError(f"Unexpected skin size: {source}")
    return pixels


def prepare(name: str, source: Path, target: Path) -> None:
    width, height = (64, 32) if name == "lex" else (64, 64)
    pixels = rgba(source, width, height)
    if name != "lex":
        def color(x: int, y: int) -> tuple[int, int, int]:
            offset = (y * width + x) * 4
            return tuple(pixels[offset:offset + 3])

        # The top of the base head is a large, reliable sample of each
        # character's hair color. It comes from the supplied texture itself.
        hair = tuple(round(median(
            color(x, y)[channel] for y in range(8) for x in range(8, 16)
        )) for channel in range(3))

        for y in range(16):
            for x in range(32, 64):
                offset = (y * width + x) * 4
                if name == "nurse":
                    # The cap is white like the JPEG background. Its red cross
                    # occupies the upper four rows of the outer front face.
                    visible = (y < 8 and 40 <= x < 56) or (8 <= y < 12)
                else:
                    pixel = pixels[offset:offset + 3]
                    hair_distance = sum((pixel[c] - hair[c]) ** 2 for c in range(3))
                    white_distance = sum((pixel[c] - 255) ** 2 for c in range(3))
                    visible = hair_distance < 0.65 * white_distance
                pixels[offset + 3] = 255 if visible else 0

    run([
        "magick", "-size", f"{width}x{height}", "-depth", "8", "rgba:-",
        "-type", "TrueColorAlpha", "-strip", str(target),
    ], input=pixels, check=True)


def main() -> None:
    project_root = Path(__file__).resolve().parents[1]
    source_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else project_root / "art_concepts/skin_cleanup/originals"
    output_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else project_root / "src/main/resources/assets/hbk/textures/entity"
    output_dir.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        prepare(name, source_dir / f"{name}.png", output_dir / f"{name}.png")


if __name__ == "__main__":
    main()
