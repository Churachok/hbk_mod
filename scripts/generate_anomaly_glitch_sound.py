#!/usr/bin/env python3
"""Synthesize HBK's original glitch prelude; requires FFmpeg, no external recordings."""
import math
from pathlib import Path
import random
import struct
import subprocess
import tempfile
import wave


def main():
    root = Path(__file__).resolve().parents[1]
    target = root / 'src/main/resources/assets/hbk/sounds/anomaly_glitch.ogg'
    target.parent.mkdir(parents=True, exist_ok=True)
    rate, duration = 22050, 0.65
    rng = random.Random(64104)
    frames, phase, noise = bytearray(), 0.0, 0.0
    for i in range(round(rate * duration)):
        t = i / rate
        if i % 11 == 0:
            noise = rng.uniform(-1, 1)
        frequency = 320 + (int(t * 24) % 5) * 270 + 1500 * (t / duration) ** 2
        phase += math.tau * frequency / rate
        gate = 0.12 if int(t / 0.028) % 7 in (2, 5) else 1.0
        envelope = min(1, t / 0.006, (duration - t) / 0.045)
        sample = (0.36 * noise + 0.22 * math.sin(phase) + 0.07 * math.copysign(1, math.sin(phase / 3)))
        frames.extend(struct.pack('<h', round(32767 * sample * gate * envelope)))
    with tempfile.TemporaryDirectory(prefix='hbk-anomaly-sound-') as scratch:
        source = Path(scratch) / 'glitch.wav'
        with wave.open(str(source), 'wb') as output:
            output.setparams((1, 2, rate, 0, 'NONE', 'not compressed'))
            output.writeframes(frames)
        subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', str(source),
                        '-map_metadata', '-1', '-c:a', 'libvorbis', '-q:a', '4', str(target)], check=True)
    print(f'Generated {target.relative_to(root)} ({duration:.2f}s, mono)')


if __name__ == '__main__':
    main()
