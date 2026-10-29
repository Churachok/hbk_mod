#!/usr/bin/env python3
"""Import the original card unchanged and derive an eight-second laugh from the checked-in CC0 clip."""

from pathlib import Path
import subprocess

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
CARD_SOURCE = ROOT / "art/skipping_owl/original-card.jpg"
CARD_OUTPUT = ROOT / "src/main/resources/assets/hbk/textures/gui/skipping_owl_card.png"
LAUGH_SOURCE = ROOT / "src/main/resources/assets/hbk/sounds/canned_laughter.ogg"
LAUGH_OUTPUT = ROOT / "src/main/resources/assets/hbk/sounds/entity/skipping_owl/laugh.ogg"
LAUGH_SECONDS = 8


def main() -> None:
    CARD_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(CARD_SOURCE) as original:
        # Format conversion only: keep the complete source, its aspect ratio and all pixels.
        original.convert("RGB").save(CARD_OUTPUT)

    LAUGH_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    filters = (
        "asetrate=30870,aresample=44100,"
        "aecho=0.8:0.45:350|700:0.35|0.2,"
        "alimiter=limit=0.85,apad=whole_dur=8,atrim=duration=8,"
        "afade=t=in:st=0:d=0.15,afade=t=out:st=7.6:d=0.4"
    )
    subprocess.run(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(LAUGH_SOURCE),
         "-vn", "-af", filters, "-ac", "1", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5",
         str(LAUGH_OUTPUT)],
        check=True,
    )
    duration = float(subprocess.check_output(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of",
         "default=noprint_wrappers=1:nokey=1", str(LAUGH_OUTPUT)], text=True).strip())
    if abs(duration - LAUGH_SECONDS) > 0.001:
        raise ValueError(f"The authoritative owl finale requires an exact eight-second clip, got {duration}")


if __name__ == "__main__":
    main()
