#!/usr/bin/env python3
"""Generate four original stylized squirrel calls. Standard library + FFmpeg.

No recordings, downloads, external references or personal paths are required.
"""

import math
from pathlib import Path
import random
import struct
import subprocess
import tempfile
import wave

RATE = 44100
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk/sounds/entity/squirrel"

# start, duration, initial pitch, final pitch, amplitude
CALLS = {
    "chirp": [(0.03, 0.13, 2700, 3900, 0.38), (0.23, 0.18, 3400, 2100, 0.34)],
    "chatter": [(0.03 + i * 0.085, 0.06, 1900 + i * 90, 1400 + i * 60, 0.32)
                for i in range(7)],
    "squeak": [(0.03, 0.34, 2100, 4200, 0.38), (0.44, 0.12, 3300, 2200, 0.24)],
    "trill": [(0.03 + i * 0.065, 0.055, 2600 + i * 55, 3100 - i * 40, 0.30)
              for i in range(9)],
}


def synthesize(notes, seed):
    rng = random.Random(seed)
    duration = max(start + length for start, length, *_ in notes) + 0.08
    frames = bytearray()
    for index in range(int(duration * RATE)):
        time = index / RATE
        sample = 0.0
        for start, length, low, high, amplitude in notes:
            elapsed = time - start
            if not 0 <= elapsed < length:
                continue
            progress = elapsed / length
            envelope = math.sin(math.pi * progress) ** 0.7
            # Swept, slightly raspy animal chirp; short modulated chatter is not a pure beep.
            phase = 2 * math.pi * (low * elapsed + (high - low) * elapsed * elapsed / (2 * length))
            phase += 1.2 * math.sin(2 * math.pi * 37 * elapsed)
            voiced = math.sin(phase) + 0.24 * math.sin(2 * phase) + 0.09 * math.sin(3 * phase)
            sample += amplitude * envelope * (voiced * 0.75 + rng.uniform(-0.12, 0.12))
        frames.extend(struct.pack("<h", round(max(-1, min(1, sample)) * 32767)))
    return frames


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="hbk-squirrel-sounds-") as temporary:
        for seed, (name, notes) in enumerate(CALLS.items()):
            source = Path(temporary) / (name + ".wav")
            with wave.open(str(source), "wb") as stream:
                stream.setnchannels(1)
                stream.setsampwidth(2)
                stream.setframerate(RATE)
                stream.writeframes(synthesize(notes, seed))
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(source),
                            "-c:a", "libvorbis", "-q:a", "5", str(OUTPUT / (name + ".ogg"))], check=True)
    print("Generated four mono squirrel calls: chirp, chatter, squeak, trill")


if __name__ == "__main__":
    main()
