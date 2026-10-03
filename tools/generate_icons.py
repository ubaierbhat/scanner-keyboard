#!/usr/bin/env python3

import math
import struct
import zlib
from pathlib import Path

CHARCOAL = (0x1B, 0x1B, 0x1F)
WHITE = (0xFF, 0xFF, 0xFF)
ACCENT_GREEN = (0x00, 0xC8, 0x53)

CONTENT_UNITS = 100.0

BAR_TOP = 10.0
BAR_X0 = 16.0
BAR_X1 = 88.0
BAR_WIDTHS = (5.0, 2.5, 3.0, 5.0, 3.0, 2.0, 3.0, 5.0, 3.0, 2.5, 5.0)
BAR_LENGTHS = (20.0, 26.0, 22.0, 30.0, 24.0, 34.0, 24.0, 30.0, 22.0, 26.0, 20.0)

DECK_TOP = 46.0
DECK_ROW_HEIGHT = 14.0
DECK_ROW_GAP = 3.0
DECK_ROW_OUTER_WIDTHS = ((15.0, 15.0, 15.0, 15.0), (15.0, 15.0, 15.0, 15.0))
DECK_BOTTOM_ROW_WIDTHS = (12.0, 12.0, 12.0, 24.0)
DECK_COL_GAP = 4.0

SCAN_GLYPH_BAR_COUNT = 4
SCAN_GLYPH_BAR_WIDTH = 2.5
SCAN_GLYPH_BAR_PITCH = 5.0
SCAN_GLYPH_HEIGHT = 8.0

LEGACY_DENSITIES = ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
LEGACY_SIZES = (48, 72, 96, 144, 192)
LEGACY_CONTENT_SPAN = 0.84
ROUND_CONTENT_SPAN = 0.78
STORE_SIZES = (512, 192)
FOREGROUND_VIEWPORT = 108.0
FOREGROUND_CONTENT_DP = 56.0
FOREGROUND_PREVIEW_PX = 216

ROOT = Path(__file__).resolve().parent.parent
MIPMAP_DIR = ROOT / "app/src/main/res"
STORE_DIR = ROOT / "docs/store"


def bar_rects():
    gap = (BAR_X1 - BAR_X0 - sum(BAR_WIDTHS)) / (len(BAR_WIDTHS) - 1)
    rects = []
    x = BAR_X0
    for width, length in zip(BAR_WIDTHS, BAR_LENGTHS):
        rects.append((x, BAR_TOP, x + width, BAR_TOP + length))
        x += width + gap
    return rects


def row_xs(widths):
    total = sum(widths) + DECK_COL_GAP * (len(widths) - 1)
    xs = []
    x = BAR_X0 + (BAR_X1 - BAR_X0 - total) / 2.0
    for width in widths:
        xs.append((x, x + width))
        x += width + DECK_COL_GAP
    return xs


def row_y_band(index):
    top = DECK_TOP + index * (DECK_ROW_HEIGHT + DECK_ROW_GAP)
    return top, top + DECK_ROW_HEIGHT


def plain_key_rects():
    rects = []
    for index, widths in enumerate(DECK_ROW_OUTER_WIDTHS):
        y0, y1 = row_y_band(index)
        for x0, x1 in row_xs(widths):
            rects.append((x0, y0, x1, y1))
    y0, y1 = row_y_band(2)
    for x0, x1 in row_xs(DECK_BOTTOM_ROW_WIDTHS)[:3]:
        rects.append((x0, y0, x1, y1))
    return rects


def scan_key_rect():
    y0, y1 = row_y_band(2)
    x0, x1 = row_xs(DECK_BOTTOM_ROW_WIDTHS)[3]
    return (x0, y0, x1, y1)


def scan_glyph_rects():
    x0, y0, x1, y1 = scan_key_rect()
    total = SCAN_GLYPH_BAR_COUNT * SCAN_GLYPH_BAR_WIDTH + (
        SCAN_GLYPH_BAR_COUNT - 1
    ) * (SCAN_GLYPH_BAR_PITCH - SCAN_GLYPH_BAR_WIDTH)
    start = x0 + (x1 - x0 - total) / 2.0
    top = y0 + (y1 - y0 - SCAN_GLYPH_HEIGHT) / 2.0
    rects = []
    for i in range(SCAN_GLYPH_BAR_COUNT):
        bx = start + i * SCAN_GLYPH_BAR_PITCH
        rects.append((bx, top, bx + SCAN_GLYPH_BAR_WIDTH, top + SCAN_GLYPH_HEIGHT))
    return rects


def glyph_layers():
    layers = [(rect, WHITE) for rect in bar_rects()]
    layers += [(rect, WHITE) for rect in plain_key_rects()]
    layers.append((scan_key_rect(), ACCENT_GREEN))
    layers += [(rect, CHARCOAL) for rect in scan_glyph_rects()]
    return layers


def new_canvas(size):
    px = []
    for _ in range(size * size):
        px.append([0.0, 0.0, 0.0, 0.0])
    return px


def paint_rect(canvas, size, rect, color):
    fx0, fy0, fx1, fy1 = rect
    x0 = max(0, int(math.floor(fx0)))
    x1 = min(size, int(math.ceil(fx1)))
    y0 = max(0, int(math.floor(fy0)))
    y1 = min(size, int(math.ceil(fy1)))
    sr, sg, sb = color
    pre = (float(sr), float(sg), float(sb), 1.0)
    for py in range(y0, y1):
        col_top = max(fy0, py)
        col_bottom = min(fy1, py + 1)
        fy = col_bottom - col_top
        if fy <= 0.0:
            continue
        row = py * size
        for px in range(x0, x1):
            cr_left = max(fx0, px)
            cr_right = min(fx1, px + 1)
            fx = cr_right - cr_left
            if fx <= 0.0:
                continue
            cov = fx * fy
            cell = canvas[row + px]
            inv = 1.0 - cov
            cell[0] = pre[0] * cov + cell[0] * inv
            cell[1] = pre[1] * cov + cell[1] * inv
            cell[2] = pre[2] * cov + cell[2] * inv
            cell[3] = pre[3] * cov + cell[3] * inv


def apply_round_mask(canvas, size):
    radius = size / 2.0
    center = size / 2.0
    for py in range(size):
        row = py * size
        dy = py + 0.5 - center
        for px in range(size):
            dx = px + 0.5 - center
            dist = math.sqrt(dx * dx + dy * dy)
            factor = min(1.0, max(0.0, radius + 0.5 - dist))
            cell = canvas[row + px]
            cell[0] *= factor
            cell[1] *= factor
            cell[2] *= factor
            cell[3] *= factor


def canvas_to_bytes(canvas, size):
    out = bytearray(size * size * 4)
    for i, cell in enumerate(canvas):
        a = cell[3]
        if a <= 0.0:
            continue
        o = i * 4
        out[o] = int(round(min(255.0, cell[0] / a)))
        out[o + 1] = int(round(min(255.0, cell[1] / a)))
        out[o + 2] = int(round(min(255.0, cell[2] / a)))
        out[o + 3] = int(round(min(255.0, a * 255.0)))
    return bytes(out)


def write_png(path, size, data):
    raw = bytearray()
    stride = size * 4
    for y in range(size):
        raw.append(0)
        raw += data[y * stride:(y + 1) * stride]
    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", idat))
        f.write(chunk(b"IEND", b""))


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(
        ">I", zlib.crc32(tag + data) & 0xFFFFFFFF
    )


def render_icon(size, background, content_fraction, round_mask):
    canvas = new_canvas(size)
    if background is not None:
        paint_rect(canvas, size, (0.0, 0.0, float(size), float(size)), background)
    span = size * content_fraction
    scale = span / CONTENT_UNITS
    offset = (size - span) / 2.0
    for rect, color in glyph_layers():
        mapped = (
            offset + rect[0] * scale,
            offset + rect[1] * scale,
            offset + rect[2] * scale,
            offset + rect[3] * scale,
        )
        paint_rect(canvas, size, mapped, color)
    if round_mask:
        apply_round_mask(canvas, size)
    return canvas_to_bytes(canvas, size)


def fmt(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return s if s not in ("", "-0") else "0"


def rect_path_data(rects, to_px):
    parts = []
    for rect in rects:
        x0 = fmt(to_px(rect[0]))
        y0 = fmt(to_px(rect[1]))
        w = fmt(to_px(rect[2]) - to_px(rect[0]))
        h = fmt(to_px(rect[3]) - to_px(rect[1]))
        parts.append(f"M{x0} {y0}h{w}v{h}h-{w}z")
    return "".join(parts)


def foreground_mapper():
    scale = FOREGROUND_CONTENT_DP / CONTENT_UNITS
    offset = (FOREGROUND_VIEWPORT - FOREGROUND_CONTENT_DP) / 2.0

    def to_px(unit):
        return offset + unit * scale

    return to_px


def write_foreground_vector(path):
    to_px = foreground_mapper()
    white = rect_path_data(bar_rects() + plain_key_rects(), to_px)
    accent = rect_path_data([scan_key_rect()], to_px)
    holes = rect_path_data(scan_glyph_rects(), to_px)
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
        f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="{white}" />\n'
        f'    <path\n        android:fillColor="#FF{accent_hex()}"\n        android:pathData="{accent}" />\n'
        f'    <path\n        android:fillColor="#FF1B1B1F"\n        android:pathData="{holes}" />\n'
        "</vector>\n"
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(xml, encoding="utf-8")


def write_monochrome_vector(path):
    to_px = foreground_mapper()
    shapes = rect_path_data(bar_rects() + plain_key_rects() + [scan_key_rect()], to_px)
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
        f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="{shapes}" />\n'
        "</vector>\n"
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(xml, encoding="utf-8")


def accent_hex():
    return "".join(f"{c:02X}" for c in ACCENT_GREEN)


def main():
    for density, size in zip(LEGACY_DENSITIES, LEGACY_SIZES):
        square = MIPMAP_DIR / f"mipmap-{density}" / "ic_launcher.png"
        round_ = MIPMAP_DIR / f"mipmap-{density}" / "ic_launcher_round.png"
        write_png(square, size, render_icon(size, CHARCOAL, LEGACY_CONTENT_SPAN, False))
        write_png(round_, size, render_icon(size, CHARCOAL, ROUND_CONTENT_SPAN, True))
    for size in STORE_SIZES:
        path = STORE_DIR / f"icon-{size}.png"
        write_png(path, size, render_icon(size, CHARCOAL, LEGACY_CONTENT_SPAN, False))
    foreground_preview = STORE_DIR / "adaptive-foreground-216.png"
    write_png(
        foreground_preview,
        FOREGROUND_PREVIEW_PX,
        render_icon(
            FOREGROUND_PREVIEW_PX,
            None,
            FOREGROUND_CONTENT_DP / FOREGROUND_VIEWPORT,
            False,
        ),
    )
    write_foreground_vector(MIPMAP_DIR / "drawable" / "ic_launcher_foreground.xml")
    write_monochrome_vector(MIPMAP_DIR / "drawable" / "ic_launcher_monochrome.xml")
    print(f"generated {len(LEGACY_SIZES) * 2 + len(STORE_SIZES) + 1} png files and 2 vector xml files")


if __name__ == "__main__":
    main()
