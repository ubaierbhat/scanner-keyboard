#!/usr/bin/env python3
"""Generate the Scanner Keyboard Play Store feature graphic (1024x500 RGB PNG).

Run:
    python3 -m venv /tmp/fgvenv && /tmp/fgvenv/bin/pip -q install pillow==12.3.0
    /tmp/fgvenv/bin/python tools/generate_feature_graphic.py [out.png]

Default output: docs/store/feature-graphic.png. Deterministic: fixed size,
no randomness, no timestamps in output. Rendered at 2x and downsampled
(LANCZOS) for antialiasing. Verified with Pillow 12.3.0 (pinned).
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

TOOLS_DIR = Path(__file__).resolve().parent
ROOT = TOOLS_DIR.parent
sys.path.insert(0, str(TOOLS_DIR))

import generate_icons as icons  # noqa: E402  (shared brand geometry, single source)

S = 2  # supersample factor
W, H = 1024 * S, 500 * S

CHARCOAL = icons.CHARCOAL
WHITE = icons.WHITE
GREEN = icons.ACCENT_GREEN
MUTED = (0x9C, 0x9C, 0xA5)
GREEN_FILL = (25, 40, 32)  # GREEN at 8% over charcoal, pre-blended

MARGIN = 64 * S

# background vertical gradient: charcoal +/- 4 levels
GRAD_TOP = (CHARCOAL[0] + 4, CHARCOAL[1] + 4, CHARCOAL[2] + 4)
GRAD_BOT = (CHARCOAL[0] - 4, CHARCOAL[1] - 4, CHARCOAL[2] - 4)

# motif placement; geometry comes from generate_icons unit grid (0..100)
MOTIF_CX = 203 * S
MOTIF_CY = 250 * S
MOTIF_SCALE = 3.55 * S  # px per content unit
MOTIF_UNIT_CX, MOTIF_UNIT_CY = 52.0, 52.0  # content bbox center in units

VF_X0, VF_Y0, VF_X1, VF_Y1 = 66 * S, 82 * S, 340 * S, 418 * S  # viewfinder box
BRACKET_LEG = 24 * S
BRACKET_THICK = 7 * S
SCAN_LINE_Y = 170 * S
SCAN_LINE_THICK = 6 * S
GLOW_THICK = 16 * S
GLOW_ALPHA = 31  # ~12% of 255

TXT_X = 372 * S
MAX_TEXT_W = 1024 * S - MARGIN - TXT_X

HEAD_SIZE = 88 * S
HEAD_LINE_STEP = 100 * S
HEAD_SUB_GAP = 26 * S

SUB_SIZE = 36 * S
SUB_CHIP_GAP = 22 * S

CHIP_SIZES = ((24, 2.0, 16), (22, 1.4, 15), (20, 1.0, 13))  # (size, ls, pad-x)
CHIP_PAD_Y = 9 * S
CHIP_RADIUS = 14 * S
CHIP_STROKE = 2 * S
CHIP_GAP = 14 * S
CHIPS = ("100% OFFLINE", "NO ACCOUNT", "NO ADS")

ARIAL_BLACK = "/System/Library/Fonts/Supplemental/Arial Black.ttf"
HELVETICA_TTC = "/System/Library/Fonts/Helvetica.ttc"


def font_black(size):
    return ImageFont.truetype(ARIAL_BLACK, int(size), index=0)


def font_helv(size):
    return ImageFont.truetype(HELVETICA_TTC, int(size), index=0)


def map_unit_rect(rect):
    x0, y0, x1, y1 = rect
    return (
        MOTIF_CX + (x0 - MOTIF_UNIT_CX) * MOTIF_SCALE,
        MOTIF_CY + (y0 - MOTIF_UNIT_CY) * MOTIF_SCALE,
        MOTIF_CX + (x1 - MOTIF_UNIT_CX) * MOTIF_SCALE,
        MOTIF_CY + (y1 - MOTIF_UNIT_CY) * MOTIF_SCALE,
    )


def make_background():
    img = Image.new("RGB", (W, H), CHARCOAL)
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / (H - 1)
        c = tuple(round(GRAD_TOP[i] + (GRAD_BOT[i] - GRAD_TOP[i]) * t) for i in range(3))
        d.line([(0, y), (W, y)], fill=c)
    return img


def draw_motif(d):
    for rect in icons.bar_rects():
        d.rectangle(map_unit_rect(rect), fill=WHITE)
    for rect in icons.plain_key_rects():
        d.rectangle(map_unit_rect(rect), fill=WHITE)
    d.rectangle(map_unit_rect(icons.scan_key_rect()), fill=GREEN)
    for rect in icons.scan_glyph_rects():
        d.rectangle(map_unit_rect(rect), fill=CHARCOAL)


def draw_viewfinder(d):
    for x0, y0, x1, y1 in (
        (VF_X0, VF_Y0, VF_X0 + BRACKET_LEG, VF_Y0 + BRACKET_THICK),
        (VF_X0, VF_Y0, VF_X0 + BRACKET_THICK, VF_Y0 + BRACKET_LEG),
        (VF_X1 - BRACKET_LEG, VF_Y0, VF_X1, VF_Y0 + BRACKET_THICK),
        (VF_X1 - BRACKET_THICK, VF_Y0, VF_X1, VF_Y0 + BRACKET_LEG),
        (VF_X0, VF_Y1 - BRACKET_THICK, VF_X0 + BRACKET_LEG, VF_Y1),
        (VF_X0, VF_Y1 - BRACKET_LEG, VF_X0 + BRACKET_THICK, VF_Y1),
        (VF_X1 - BRACKET_LEG, VF_Y1 - BRACKET_THICK, VF_X1, VF_Y1),
        (VF_X1 - BRACKET_THICK, VF_Y1 - BRACKET_LEG, VF_X1, VF_Y1),
    ):
        d.rectangle((x0, y0, x1, y1), fill=GREEN)
    glow = GREEN + (GLOW_ALPHA,)
    d.rectangle(
        (VF_X0, SCAN_LINE_Y - GLOW_THICK // 2, VF_X1, SCAN_LINE_Y + GLOW_THICK // 2),
        fill=glow,
    )
    d.rectangle(
        (VF_X0, SCAN_LINE_Y - SCAN_LINE_THICK // 2,
         VF_X1, SCAN_LINE_Y + SCAN_LINE_THICK // 2),
        fill=GREEN,
    )


def text_w(font, text):
    box = font.getbbox(text)
    return box[2] - box[0]


def draw_spaced(d, font, x, y, text, ls, fill):
    cx = x
    for ch in text:
        d.text((cx, y), ch, font=font, fill=fill, anchor="la")
        cx += text_w(font, ch) + ls


def fit_headline():
    font = font_black(HEAD_SIZE)
    if text_w(font, "SCANNER KEYBOARD") <= MAX_TEXT_W:
        return font, ["SCANNER KEYBOARD"]
    lines = ["SCANNER", "KEYBOARD"]
    size = HEAD_SIZE
    while any(text_w(font, ln) > MAX_TEXT_W for ln in lines) and size > 40 * S:
        size -= 2 * S
        font = font_black(size)
    return font, lines


def build():
    img = make_background()
    d = ImageDraw.Draw(img, "RGBA")

    draw_motif(d)
    draw_viewfinder(d)

    head_font, head_lines = fit_headline()
    sub_text = "The scanner that lives in your keyboard."
    sub_limit = MAX_TEXT_W - 20 * S
    sub_size = SUB_SIZE
    sub_font = font_helv(sub_size)
    while text_w(sub_font, sub_text) > sub_limit and sub_size > 26 * S:
        sub_size -= S
        sub_font = font_helv(sub_size)

    chip_font, chip_ls, chip_pad_x = None, 0.0, 0
    for size, ls, pad_x in CHIP_SIZES:
        font = font_black(size * S)
        widths = [
            sum(text_w(font, ch) for ch in c) + ls * S * (len(c) - 1) + 2 * pad_x * S
            for c in CHIPS
        ]
        total = sum(widths) + CHIP_GAP * (len(CHIPS) - 1)
        if total <= MAX_TEXT_W:
            chip_font, chip_ls, chip_pad_x, chip_widths = font, ls * S, pad_x * S, widths
            break
    assert chip_font is not None, "chips do not fit at any configured size"
    chip_ink = chip_font.getbbox("H")  # (l, top, r, bottom) rel. to 'la' anchor
    cap_h = chip_ink[3] - chip_ink[1]
    chip_h = cap_h + 2 * CHIP_PAD_Y

    # ink-based vertical centering (no phantom line-box padding)
    head_cap = head_font.getbbox("H")  # 'la' anchor offsets
    head_ink_h = head_cap[3] - head_cap[1]
    sub_ink = sub_font.getbbox(sub_text)
    sub_ink_h = sub_ink[3] - sub_ink[1]
    head_block_h = head_ink_h + (len(head_lines) - 1) * HEAD_LINE_STEP
    group_h = head_block_h + HEAD_SUB_GAP + sub_ink_h + SUB_CHIP_GAP + chip_h
    top = int((H - group_h) / 2)

    y = top - head_cap[1]
    for line in head_lines:
        d.text((TXT_X, y), line, font=head_font, fill=WHITE, anchor="la")
        y += HEAD_LINE_STEP
    sub_y = top + head_block_h + HEAD_SUB_GAP - sub_ink[1]
    d.text((TXT_X, sub_y), sub_text, font=sub_font, fill=MUTED, anchor="la")
    y = top + head_block_h + HEAD_SUB_GAP + sub_ink_h + SUB_CHIP_GAP

    cx = TXT_X
    for chip, cw in zip(CHIPS, chip_widths):
        d.rounded_rectangle(
            (cx, y, cx + cw, y + chip_h),
            radius=CHIP_RADIUS,
            fill=GREEN_FILL,
            outline=GREEN,
            width=CHIP_STROKE,
        )
        draw_spaced(
            d, chip_font, cx + chip_pad_x, y + CHIP_PAD_Y - chip_ink[1],
            chip, chip_ls, GREEN,
        )
        cx += cw + CHIP_GAP

    return img.convert("RGB").resize((1024, 500), Image.LANCZOS)


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "docs/store/feature-graphic.png"
    img = build()
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG", optimize=True, compress_level=9)
    print(f"{out} {img.size[0]}x{img.size[1]} {out.stat().st_size} bytes")


if __name__ == "__main__":
    main()
