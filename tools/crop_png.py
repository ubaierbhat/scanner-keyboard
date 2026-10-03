#!/usr/bin/env python3

import struct
import sys
import zlib
from pathlib import Path


def read_png(path):
    data = Path(path).read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"not a png file: {path}")
    pos = 8
    width = height = None
    idat = bytearray()
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        tag = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", payload[:10])
            if depth != 8 or color != 6:
                raise ValueError("only 8-bit RGBA png files are supported")
            stride = width * 4
        elif tag == b"IDAT":
            idat += payload
        elif tag == b"IEND":
            break
        pos += 12 + length
    raw = zlib.decompress(bytes(idat))
    pixels = bytearray(height * stride)
    prev = bytearray(stride)
    for y in range(height):
        line_start = y * (stride + 1)
        filter_type = raw[line_start]
        line = bytearray(raw[line_start + 1:line_start + 1 + stride])
        apply_filter(filter_type, line, prev, stride)
        pixels[y * stride:(y + 1) * stride] = line
        prev = line
    return width, height, pixels


def apply_filter(filter_type, line, prev, stride):
    if filter_type == 0:
        return
    for i in range(stride):
        a = line[i - 4] if i >= 4 else 0
        b = prev[i]
        c = prev[i - 4] if i >= 4 else 0
        if filter_type == 1:
            line[i] = (line[i] + a) & 0xFF
        elif filter_type == 2:
            line[i] = (line[i] + b) & 0xFF
        elif filter_type == 3:
            line[i] = (line[i] + ((a + b) >> 1)) & 0xFF
        elif filter_type == 4:
            p = a + b - c
            pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
            pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
            line[i] = (line[i] + pred) & 0xFF
        else:
            raise ValueError(f"unknown png filter {filter_type}")


def write_png(path, width, height, pixels):
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw += pixels[y * stride:(y + 1) * stride]
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(png_chunk(b"IHDR", ihdr))
        f.write(png_chunk(b"IDAT", idat))
        f.write(png_chunk(b"IEND", b""))


def png_chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(
        ">I", zlib.crc32(tag + data) & 0xFFFFFFFF
    )


def crop(src, dst, box, scale=1):
    width, height, pixels = read_png(src)
    x0, y0, x1, y1 = box
    x0, y0 = max(0, x0), max(0, y0)
    x1, y1 = min(width, x1), min(height, y1)
    cw, ch = x1 - x0, y1 - y0
    out_w, out_h = cw * scale, ch * scale
    out = bytearray(out_w * out_h * 4)
    src_stride = width * 4
    out_stride = out_w * 4
    for dy in range(out_h):
        sy = y0 + dy // scale
        src_row = sy * src_stride
        row_start = dy * out_stride
        for dx in range(out_w):
            sx = x0 + dx // scale
            s = src_row + sx * 4
            d = row_start + dx * 4
            out[d:d + 4] = pixels[s:s + 4]
    write_png(dst, out_w, out_h, out)
    print(f"wrote {dst} ({out_w}x{out_h})")


def main(argv):
    src, dst, x0, y0, x1, y1 = argv[:6]
    scale = int(argv[6]) if len(argv) > 6 else 1
    crop(src, dst, (int(x0), int(y0), int(x1), int(y1)), scale)


if __name__ == "__main__":
    main(sys.argv[1:])
