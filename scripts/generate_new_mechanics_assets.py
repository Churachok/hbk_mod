#!/usr/bin/env python3
"""Generate original pixel art and a synthesized goose honk for new mechanics."""

from __future__ import annotations

import math
import random
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/hbk"
ITEMS = ASSETS / "textures/item"
BLOCKS = ASSETS / "textures/block"
EFFECTS = ASSETS / "textures/mob_effect"
ENTITIES = ASSETS / "textures/entity"
SOUNDS = ASSETS / "sounds"
T = (0, 0, 0, 0)


def canvas(size: int = 16) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGBA", (size, size), T)
    return image, ImageDraw.Draw(image)


def save(image: Image.Image, folder: Path, name: str) -> None:
    folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f"{name}.png")


def western_chestplate() -> Image.Image:
    image, d = canvas()
    dark, mid, edge, purple, glow = "#09090d", "#181822", "#34333f", "#55139a", "#a52cff"
    d.polygon([(2, 3), (5, 1), (7, 3), (8, 3), (10, 1), (13, 3), (12, 13), (9, 15), (8, 12), (7, 12), (6, 15), (3, 13)], fill=dark)
    d.line([(2, 3), (3, 13), (6, 15)], fill=edge, width=1)
    d.line([(13, 3), (12, 13), (9, 15)], fill=mid, width=1)
    d.polygon([(4, 4), (7, 6), (6, 11), (4, 9)], fill=mid)
    d.polygon([(12, 4), (9, 6), (10, 11), (12, 9)], fill="#111118")
    d.line([(3, 4), (6, 6), (8, 10), (10, 6), (13, 4)], fill=purple)
    d.point((8, 10), fill=glow)
    return image


def balalaika_pickaxe() -> Image.Image:
    image, d = canvas()
    wood, light, dark = "#a85c21", "#e39a3b", "#4a2816"
    iron, shine, shadow = "#aeb6c4", "#eef3fa", "#555b69"
    d.polygon([(1, 14), (2, 9), (6, 13)], fill=dark)
    d.polygon([(2, 13), (3, 9), (6, 12)], fill=wood)
    d.line([(2, 12), (5, 11)], fill=light)
    d.line([(4, 10), (11, 3)], fill=dark, width=2)
    d.line([(5, 10), (12, 3)], fill=light)
    d.line([(2, 13), (12, 3)], fill="#f5dfb2")
    d.polygon([(8, 2), (12, 1), (15, 4), (14, 5), (12, 3), (10, 5)], fill=iron)
    d.line([(8, 2), (12, 1), (15, 4)], fill=shine)
    d.line([(10, 5), (12, 3), (14, 5)], fill=shadow)
    return image


def currant_tincture() -> Image.Image:
    image, d = canvas(32)
    d.rectangle((12, 1, 19, 4), fill="#6c3c1f")
    d.rectangle((10, 4, 21, 8), fill="#cad7e2")
    d.rectangle((7, 8, 24, 28), fill="#aebcc9")
    d.rectangle((8, 10, 23, 27), fill="#52052d")
    d.rectangle((10, 9, 21, 12), fill="#a20d55")
    d.rectangle((7, 11, 8, 24), fill="#e8f5ff")
    d.rectangle((10, 15, 21, 23), fill="#ead6a3")
    font = ImageFont.truetype("/usr/share/fonts/TTF/DejaVuSansCondensed.ttf", 4)
    d.text((10, 17), "Смородина", fill="#2b160d", font=font, spacing=0)
    d.ellipse((13, 20, 16, 23), fill="#60103d")
    d.ellipse((16, 19, 19, 22), fill="#7f1550")
    d.point((12, 11), fill="#f6fbff")
    return image


def ration() -> Image.Image:
    image, d = canvas()
    d.polygon([(2, 4), (7, 2), (14, 5), (12, 14), (3, 13)], fill="#626341")
    d.polygon([(2, 4), (7, 5), (14, 5), (7, 7)], fill="#85865c")
    d.line([(7, 3), (7, 14)], fill="#c4a77b")
    d.line([(2, 9), (13, 9)], fill="#b6976b")
    d.rectangle((6, 8, 8, 10), fill="#9d2828")
    d.point((4, 6), fill="#9a9b6e")
    return image


def sushka() -> Image.Image:
    image, d = canvas()
    d.ellipse((2, 2, 13, 13), fill="#7b3b12")
    d.ellipse((3, 3, 12, 12), fill="#d17a20")
    d.ellipse((6, 6, 9, 9), fill=T)
    d.arc((3, 3, 12, 12), 190, 330, fill="#ffc558", width=2)
    d.point((5, 4), fill="#ffe09b")
    d.point((11, 7), fill="#8d4314")
    d.point((5, 11), fill="#8d4314")
    return image


def golden_crown() -> Image.Image:
    image, d = canvas()
    d.polygon([(2, 5), (5, 8), (7, 3), (9, 8), (13, 4), (12, 12), (3, 12)], fill="#e3a617")
    d.line([(2, 5), (5, 9), (7, 3), (9, 9), (13, 4)], fill="#fff178")
    d.rectangle((3, 10, 12, 13), fill="#f0bd26")
    d.line([(3, 13), (12, 13)], fill="#8e5b09")
    d.point((7, 11), fill="#37b9dd")
    d.line([(1, 7), (0, 5)], fill="#f5f5eb")
    d.line([(13, 7), (15, 5)], fill="#f5f5eb")
    return image


def shovel_sword() -> Image.Image:
    image, d = canvas()
    d.line([(2, 14), (10, 6)], fill="#5b321b", width=3)
    d.line([(3, 13), (10, 6)], fill="#a56636")
    d.rectangle((1, 13, 4, 15), fill="#747c88")
    d.polygon([(8, 8), (10, 2), (13, 0), (15, 2), (13, 7), (10, 10)], fill="#aeb6c2")
    d.line([(10, 2), (13, 0), (15, 2)], fill="#f3f7fb")
    d.line([(10, 9), (13, 7), (15, 2)], fill="#555d69")
    d.line([(7, 8), (11, 10)], fill="#d2a43a", width=2)
    d.point((9, 9), fill="#f0d179")
    return image


def strange_chest_texture() -> Image.Image:
    image, d = canvas()
    d.rectangle((0, 0, 15, 15), fill="#261713")
    for y in (2, 7, 12):
        d.line((0, y, 15, y), fill="#4b2a20")
    for x in (1, 14):
        d.rectangle((x, 0, x + 1, 15), fill="#37303f")
    d.rectangle((6, 5, 10, 10), fill="#25202d")
    d.ellipse((6, 5, 10, 9), fill="#7520c7")
    d.ellipse((7, 6, 9, 8), fill="#cf72ff")
    d.point((8, 7), fill="#0b0710")
    d.rectangle((7, 9, 9, 12), fill="#50465c")
    d.point((8, 10), fill="#17121c")
    d.point((4, 4), fill="#684032")
    d.point((12, 10), fill="#100b0a")
    return image


def hand_effect() -> Image.Image:
    image, d = canvas(18)
    purple, glow = "#5a1594", "#c96cff"
    d.polygon([(5, 15), (3, 10), (4, 5), (6, 10), (7, 2), (9, 10), (11, 3), (11, 11), (14, 6), (14, 13), (11, 16)], fill=purple)
    d.line([(5, 14), (4, 10), (5, 7)], fill=glow)
    d.point((8, 4), fill="#f3c7ff")
    d.point((12, 5), fill="#f3c7ff")
    return image


def soulfulness_effect() -> Image.Image:
    image, d = canvas(18)
    d.ellipse((3, 3, 14, 14), fill="#d99a32")
    d.ellipse((5, 5, 12, 12), fill="#f5d87c")
    d.arc((6, 6, 11, 11), 20, 160, fill="#5b3517", width=1)
    d.point((6, 8), fill="#5b3517")
    d.point((11, 8), fill="#5b3517")
    d.text((1, 0), "♪", fill="#a653c6")
    d.text((12, 9), "♪", fill="#7b279c")
    return image


def synthesize_goose_honk() -> None:
    SOUNDS.mkdir(parents=True, exist_ok=True)
    rate = 44_100
    duration = 0.82
    frames: list[bytes] = []
    random.seed(1945)
    phase = 0.0
    for index in range(int(rate * duration)):
        t = index / rate
        attack = min(1.0, t / 0.035)
        release = min(1.0, (duration - t) / 0.14)
        envelope = attack * release
        frequency = 470.0 - 165.0 * (t / duration) + 18.0 * math.sin(t * 24.0)
        phase += 2.0 * math.pi * frequency / rate
        rasp = random.uniform(-1.0, 1.0) * 0.07
        sample = envelope * (0.68 * math.sin(phase) + 0.23 * math.sin(phase * 2.02) + rasp)
        frames.append(struct.pack("<h", max(-32767, min(32767, int(sample * 26000)))))
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / "goose_honk.wav"
        with wave.open(str(wav_path), "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(rate)
            wav_file.writeframes(b"".join(frames))
        subprocess.run(
            ["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav_path), "-c:a", "libvorbis", "-q:a", "5", str(SOUNDS / "goose_honk.ogg")],
            check=True,
        )


def main() -> None:
    for name, factory in {
        "western_chestplate": western_chestplate,
        "balalaika_pickaxe": balalaika_pickaxe,
        "currant_tincture": currant_tincture,
        "ration": ration,
        "sushka": sushka,
        "golden_crown": golden_crown,
        "shovel_sword": shovel_sword,
    }.items():
        save(factory(), ITEMS, name)
    save(strange_chest_texture(), BLOCKS, "strange_chest")
    save(hand_effect(), EFFECTS, "hand_immortality")
    save(soulfulness_effect(), EFFECTS, "soulfulness")
    black_skin = Image.new("RGBA", (64, 64), (0, 0, 0, 255))
    save(black_skin, ENTITIES, "black_silhouette")
    synthesize_goose_honk()
    print("Generated new mechanic textures and goose_honk.ogg")


if __name__ == "__main__":
    main()
