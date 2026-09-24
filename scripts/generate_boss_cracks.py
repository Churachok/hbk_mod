#!/usr/bin/env python3
"""Generate deterministic armor-damage stages from the checked-in liberal skin."""

from pathlib import Path
import shutil
import subprocess


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/hbk/textures/entity"
SOURCE = TEXTURES / "mad_liberal.png"

# Cumulative zig-zags across the visible head, torso, arm and leg UV islands.
CRACKS = [
    [(23, 20), (25, 23), (23, 26), (26, 30)],
    [(10, 8), (12, 11), (10, 14)],
    [(45, 21), (46, 24), (44, 27), (46, 31)],
    [(5, 21), (7, 24), (5, 27), (6, 31)],
    [(21, 53), (23, 56), (21, 59), (22, 63)],
    [(37, 53), (39, 56), (37, 59), (38, 63)],
]


def main() -> None:
    executable = shutil.which("magick") or shutil.which("convert")
    if executable is None:
        raise SystemExit("ImageMagick (magick or convert) is required")
    for stage in range(1, 5):
        command = [executable, str(SOURCE), "-stroke", "#30231c", "-strokewidth", "1", "-fill", "none"]
        for points in CRACKS[: stage + 1]:
            coordinates = " ".join(f"{x},{y}" for x, y in points)
            command.extend(["-draw", f"polyline {coordinates}"])
        command.append(str(TEXTURES / f"mad_liberal_cracked_{stage}.png"))
        subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
