"""Generate the 16x16 inventory icon for Kirill's white-framed red glasses."""

from pathlib import Path
import struct
import zlib


def main() -> None:
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    shadow = (168, 169, 173, 255)
    frame = (247, 247, 246, 255)
    shine = (255, 255, 255, 255)
    lens = (215, 21, 29, 255)
    highlight = (255, 72, 75, 255)

    def rectangle(x0: int, y0: int, x1: int, y1: int, color: tuple[int, ...]) -> None:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                pixels[y][x] = color

    rectangle(1, 5, 14, 10, shadow)
    rectangle(2, 4, 13, 9, frame)
    rectangle(3, 5, 6, 8, lens)
    rectangle(9, 5, 12, 8, lens)
    rectangle(3, 5, 4, 6, highlight)
    rectangle(9, 5, 10, 6, highlight)
    rectangle(7, 6, 8, 6, shadow)
    pixels[4][2] = shine
    pixels[4][9] = shine
    rectangle(0, 6, 1, 7, frame)
    rectangle(14, 6, 15, 7, frame)

    output = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbk/textures/item/kirill_glasses.png"
    raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in pixels)

    def chunk(name: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + name + data + struct.pack(">I", zlib.crc32(name + data))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw))
           + chunk(b"IEND", b""))
    output.write_bytes(png)


if __name__ == "__main__":
    main()
