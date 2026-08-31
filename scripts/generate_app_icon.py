#!/usr/bin/env python3
"""Generate Awqat's app icon using stdlib only.

Renders artboard 2d, "Gece Kâbesi", from docs/design/awqat-final-designs.dc.html.
The design is built entirely from rectangles plus one dashed ring, so it maps
directly onto a per-pixel painter and needs no imaging dependency in CI.

Geometry is expressed in the design's own 1024x1024 coordinate space with the
Kaaba group translated to (512, 524), so the numbers here can be diffed against
the SVG in the design file.
"""

import argparse
import math
import struct
import zlib
from pathlib import Path

SIZE = 1024
CX, CY = 512, 524

FIELD = (0x1E, 0x3A, 0x32)      # Kahraman yüzey
RING = (0x46, 0x7A, 0x69)       # Vurgu
BODY = (0x10, 0x16, 0x13)       # Kaaba body
KISWAH = (0x2C, 0x4A, 0x3F)     # top curve and base
GOLD = (0xC2, 0x96, 0x53)       # hizam and door frame
GOLD_DEEP = (0x9E, 0x74, 0x34)  # band pattern and door panel
SEAM = (0x1E, 0x26, 0x21)       # fabric seams

RING_RADIUS = 394
RING_WIDTH = 26
RING_DASH_ON = 5
RING_DASH_PERIOD = 67  # 5 on, 62 off

# (x, y, width, height, corner radius, colour) in group coordinates, painted in order.
SHAPES = [
    (-252, -216, 504, 424, 20, BODY),
    (-252, -216, 504, 30, 15, KISWAH),
    (-252, -142, 504, 62, 0, GOLD),
    (-224, -127, 66, 32, 6, GOLD_DEEP),
    (-134, -127, 66, 32, 6, GOLD_DEEP),
    (-44, -127, 66, 32, 6, GOLD_DEEP),
    (46, -127, 66, 32, 6, GOLD_DEEP),
    (136, -127, 66, 32, 6, GOLD_DEEP),
    (-96, -80, 6, 288, 0, SEAM),
    (90, -80, 6, 288, 0, SEAM),
    (118, 46, 96, 162, 10, GOLD),
    (132, 62, 68, 146, 6, GOLD_DEEP),
    (132, 62, 68, 10, 0, GOLD),
    (-268, 196, 536, 26, 13, KISWAH),
]


def in_rounded_rect(px: float, py: float, x: float, y: float, w: float, h: float, r: float) -> bool:
    if not (x <= px <= x + w and y <= py <= y + h):
        return False
    if r <= 0:
        return True
    r = min(r, w / 2.0, h / 2.0)
    # Outside the corner boxes the rectangle test above is already conclusive.
    cx = min(max(px, x + r), x + w - r)
    cy = min(max(py, y + r), y + h - r)
    dx, dy = px - cx, py - cy
    return dx * dx + dy * dy <= r * r


def on_dashed_ring(dx: float, dy: float) -> bool:
    distance = math.hypot(dx, dy)
    if abs(distance - RING_RADIUS) > RING_WIDTH / 2.0:
        return False
    # SVG starts the dash pattern at 0rad and runs clockwise.
    angle = math.atan2(dy, dx) % (2 * math.pi)
    return (angle * RING_RADIUS) % RING_DASH_PERIOD < RING_DASH_ON


def pixel(x: int, y: int) -> tuple[int, int, int]:
    dx, dy = x - CX, y - CY
    colour = FIELD
    if on_dashed_ring(dx, dy):
        colour = RING
    for rx, ry, w, h, r, shape_colour in SHAPES:
        if in_rounded_rect(dx, dy, rx, ry, w, h, r):
            colour = shape_colour
    return colour


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
    path.write_bytes(bytes(png))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path)
    generate(parser.parse_args().output)


if __name__ == "__main__":
    main()
