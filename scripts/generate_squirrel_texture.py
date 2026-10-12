"""Generate the 64x64 UV atlas for the cuboid squirrel model (standard library only)."""

from pathlib import Path
import random
import struct
import zlib


def main() -> None:
    rng = random.Random(7)
    fur = [(195, 76, 23, 255), (214, 89, 27, 255), (228, 105, 34, 255), (207, 83, 24, 255)]
    cream = (244, 211, 158, 255)
    dark = (78, 39, 24, 255)
    pixels = [[fur[rng.randrange(len(fur))] for _ in range(64)] for _ in range(64)]

    def fill(x: int, y: int, width: int, height: int, color: tuple[int, ...]) -> None:
        for row in range(y, y + height):
            for column in range(x, x + width):
                pixels[row][column] = color

    # Body: front chest, underside, and the bottom two pixels of both side faces.
    fill(9, 9, 6, 5, cream)
    fill(15, 0, 6, 9, cream)
    fill(0, 12, 9, 2, cream)
    fill(15, 12, 9, 2, cream)
    # Head: flat pixel eyes on the front and side faces, cream cheeks.
    fill(37, 8, 5, 2, cream)
    for x in (37, 41):
        fill(x, 6, 1, 2, (21, 18, 16, 255))
        pixels[6][x] = (250, 245, 227, 255)
    for x in (36, 42):
        fill(x, 6, 1, 2, (21, 18, 16, 255))
    # Short legs with dark paws, simple ears with cream inner faces.
    fill(0, 28, 8, 2, dark)
    fill(17, 26, 2, 2, cream)
    fill(16, 24, 6, 1, dark)
    # Muzzle front is x=26..29, y=26..27: keep the nose centered, not on a side face.
    fill(24, 24, 12, 4, cream)
    fill(27, 26, 2, 1, dark)
    # The rear UV face of every 6x5x4 tail segment is x=14..19, y=44..48.
    # Keep the cream underside, but give it fur detail instead of a blank flat patch.
    tail_cream = [cream, (230, 185, 126, 255), (249, 222, 176, 255)]
    for y in range(44, 49):
        for x in range(14, 20):
            pixels[y][x] = (fur[(x + y) % len(fur)] if x in (14, 19)
                            else tail_cream[(x + y // 2) % len(tail_cream)])

    # A separate cuboid UV net gives all six faces of the terminal segment pale fur.
    tip_fur = [(250, 237, 211, 255), (242, 224, 191, 255),
               (255, 244, 220, 255), (247, 231, 202, 255)]
    for y in range(40, 49):
        for x in range(32, 52):
            pixels[y][x] = tip_fur[(x + y // 2) % len(tip_fur)]

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in pixels)
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", 64, 64, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))
    output = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk/textures/entity/squirrel.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(png)


if __name__ == "__main__":
    main()
