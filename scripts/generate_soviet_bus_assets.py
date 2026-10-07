#!/usr/bin/env python3
"""Reproducible model-local pixel materials, exact route plates and a synthesized dual-tone horn.

Requires Pillow and FFmpeg. Does not read references or files outside this repository.
"""
import math
from pathlib import Path
import random
import struct
import subprocess
import tempfile
import wave

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/hbk"

GLYPHS = {
    "6": ["01110", "10000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "с": ["111", "100", "100", "100", "111"],
    "2": ["111", "001", "111", "100", "111"],
    "8": ["111", "101", "111", "101", "111"],
    "а": ["010", "101", "111", "101", "101"],
}


def text(draw, value, x, y, scale=1):
    for char in value:
        glyph = GLYPHS[char]
        for row, bits in enumerate(glyph):
            for col, bit in enumerate(bits):
                if bit == "1":
                    draw.rectangle((x + col * scale, y + row * scale,
                                    x + (col + 1) * scale - 1, y + (row + 1) * scale - 1), fill="#16191c")
        x += (len(glyph[0]) + 1) * scale


def texture():
    image = Image.new("RGBA", (1024, 512), "#ffffff")
    rng = random.Random(67)
    patches = [
        (0, 0, 512, 128, (225, 224, 216, 255)),
        (512, 0, 512, 128, (28, 31, 32, 255)),
        (0, 128, 128, 128, (150, 155, 156, 255)),
        (128, 128, 128, 128, (66, 70, 73, 255)),
        (256, 128, 128, 128, (35, 53, 80, 255)),
        (384, 128, 128, 128, (210, 161, 49, 255)),
        (512, 128, 256, 128, (29, 43, 55, 110)),
        (768, 128, 128, 128, (255, 231, 154, 255)),
        (896, 128, 128, 128, (200, 42, 31, 255)),
    ]
    for x, y, width, height, color in patches:
        for px in range(x, x + width):
            for py in range(y, y + height):
                noise = rng.choice([-8, -3, 0, 0, 2, 5, 9])
                image.putpixel((px, py), tuple(max(0, min(255, c + noise)) for c in color[:3]) + (color[3],))
    draw = ImageDraw.Draw(image)
    # Front and back cube faces: u + depth, v + depth; south starts u + 2*depth + width.
    for x in (129, 144):
        text(draw, "67", x + 1, 258)
    for x in (129, 162):
        text(draw, "с228ас", x + 4, 322)
    # Both broad faces of the side placard, 1 x 14 x 34 model pixels.
    for x in (256, 291):
        text(draw, "67", x + 6, 293)
    folder = ASSETS / "textures/entity"
    folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / "soviet_bus.png")


def horn():
    rate = 44100
    duration = 1.35
    frames = bytearray()
    for i in range(int(rate * duration)):
        t = i / rate
        envelope = 0.0
        for begin, end in [(0.02, 0.45), (0.57, 1.18)]:
            if begin <= t <= end:
                envelope = min(1.0, (t - begin) / 0.018, (end - t) / 0.055)
        # Slightly detuned two-note electromechanical horn with buzzy harmonics.
        value = sum(math.sin(2 * math.pi * frequency * harmonic * t) / harmonic
                    for frequency in (370, 466) for harmonic in (1, 2, 3, 5))
        frames.extend(struct.pack("<h", int(max(-1, min(1, value * envelope * 0.22)) * 32767)))
    folder = ASSETS / "sounds"
    folder.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="hbk-bus-horn-") as temporary:
        source = Path(temporary) / "horn.wav"
        with wave.open(str(source), "wb") as stream:
            stream.setnchannels(1)
            stream.setsampwidth(2)
            stream.setframerate(rate)
            stream.writeframes(frames)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(source),
                        "-c:a", "libvorbis", "-q:a", "5", str(folder / "bus_horn.ogg")], check=True)


if __name__ == "__main__":
    texture()
    horn()
    print("Generated bus materials, route 67, с228ас plates and dual-tone horn")
