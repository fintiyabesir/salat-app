#!/usr/bin/env python3
"""Generate Salat's deterministic 1024x1024 iOS app icon using stdlib only."""

import argparse
import struct
import zlib
from pathlib import Path

SIZE = 1024
CX = CY = SIZE // 2
CREAM = (247, 244, 236)
MINT_OUTER = (229, 239, 236)
MINT_INNER = (221, 234, 230)
TEAL = (47, 104, 101)
GOLD = (194, 150, 83)


def inside_polygon(x: int, y: int, points: list[tuple[int, int]]) -> bool:
    inside = False
    j = len(points) - 1
    for i, (xi, yi) in enumerate(points):
        xj, yj = points[j]
        if (yi > y) != (yj > y):
            boundary = (xj - xi) * (y - yi) / (yj - yi) + xi
            if x < boundary:
                inside = not inside
        j = i
    return inside


def pixel(x: int, y: int) -> tuple[int, int, int]:
    dx, dy = x - CX, y - CY
    distance2 = dx * dx + dy * dy
    color = CREAM
    if distance2 <= 430 * 430:
        color = MINT_OUTER
    if distance2 <= 330 * 330:
        color = MINT_INNER
    if abs(dx) + abs(dy) <= 245:
        color = TEAL
    if abs(dx) + abs(dy) <= 160:
        color = CREAM

    needle = [
        (CX, CY - 132),
        (CX + 56, CY + 54),
        (CX, CY + 25),
        (CX - 56, CY + 54),
    ]
    if inside_polygon(x, y, needle):
        color = GOLD
    if distance2 <= 24 * 24:
        color = TEAL
    return color


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return (
        struct.pack(">I", len(payload))
        + kind
        + payload
        + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)
    )


def generate(path: Path) -> None:
    rows = bytearray()
    for y in range(SIZE):
        rows.append(0)  # PNG filter: None
        for x in range(SIZE):
            rows.extend(pixel(x, y))

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(png_chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0)))
    png.extend(png_chunk(b"IDAT", zlib.compress(bytes(rows), level=9)))
    png.extend(png_chunk(b"IEND", b""))

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    generate(args.output)
