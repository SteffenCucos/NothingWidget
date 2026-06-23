#!/usr/bin/env python3
"""Render a deterministic preview of the NothingWidget app-widget layout.

This is a lightweight CI renderer for the RemoteViews XML layout. It is not a
full Android LayoutLib screenshot, but it intentionally mirrors the current
widget_solar_event.xml defaults so every CI run publishes a visual artifact.
"""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Optional

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
LAYOUT_PATH = ROOT / "app" / "src" / "main" / "res" / "layout" / "widget_solar_event.xml"
OUT_PATH = ROOT / "artifacts" / "widget-layout-preview.png"

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


def android_attr(element: ET.Element, name: str, default: Optional[str] = None) -> Optional[str]:
    return element.attrib.get(f"{ANDROID_NS}{name}", default)


def parse_dimension(value: Optional[str], fallback: int) -> int:
    if not value:
        return fallback
    match = re.match(r"([0-9.]+)(?:dp|sp)?", value)
    if not match:
        return fallback
    return int(round(float(match.group(1))))


def find_by_id(root: ET.Element, view_id: str) -> Optional[ET.Element]:
    target = f"@+id/{view_id}"
    for element in root.iter():
        if android_attr(element, "id") == target:
            return element
    return None


def text_for(root: ET.Element, view_id: str, fallback: str) -> str:
    element = find_by_id(root, view_id)
    return android_attr(element, "text", fallback) if element is not None else fallback


def progress_for(root: ET.Element, view_id: str, fallback: int) -> int:
    element = find_by_id(root, view_id)
    if element is None:
        return fallback
    try:
        return int(android_attr(element, "progress", str(fallback)) or fallback)
    except ValueError:
        return fallback


def text_size_for(root: ET.Element, view_id: str, fallback: int) -> int:
    element = find_by_id(root, view_id)
    return parse_dimension(android_attr(element, "textSize") if element is not None else None, fallback)


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationMono-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation2/LiberationMono-Regular.ttf",
        "/Library/Fonts/Menlo.ttc",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def text_height(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont) -> int:
    box = draw.textbbox((0, 0), text, font=font)
    return box[3] - box[1]


def draw_centered_text(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    text: str,
    font: ImageFont.ImageFont,
    fill: tuple[int, int, int, int],
) -> None:
    box = draw.textbbox((0, 0), text, font=font)
    width = box[2] - box[0]
    height = box[3] - box[1]
    left, top, right, bottom = xy
    draw.text((left + (right - left - width) / 2, top + (bottom - top - height) / 2 - 1), text, font=font, fill=fill)


def render() -> None:
    layout = ET.parse(LAYOUT_PATH).getroot()

    # A 260x110dp widget rendered at 2x density gives a readable CI artifact.
    density = 2
    width, height = 260 * density, 110 * density
    image = Image.new("RGBA", (width, height), (245, 245, 245, 255))
    draw = ImageDraw.Draw(image)

    margin = 8 * density
    padding = parse_dimension(android_attr(layout, "padding"), 16) * density
    corner_radius = 24 * density
    root_box = (margin, margin, width - margin, height - margin)

    draw.rounded_rectangle(root_box, radius=corner_radius, fill=(17, 17, 17, 255), outline=(51, 51, 51, 255), width=1 * density)

    x0, y0, x1, y1 = root_box
    content_x = x0 + padding
    content_y = y0 + padding
    content_height = y1 - y0 - 2 * padding

    right_width = 64 * density
    right_x0 = x1 - padding - right_width
    left_x0 = content_x
    left_x1 = right_x0 - 6 * density

    event_status = text_for(layout, "eventStatus", "NEXT")
    event_label = text_for(layout, "eventLabel", "SUNSET")
    event_time = text_for(layout, "eventTime", "8:45 PM")
    event_remaining = text_for(layout, "eventRemaining", "2h 14m left")
    event_icon = text_for(layout, "eventIcon", "◐")
    progress_text = text_for(layout, "progressText", "50%")
    progress = max(0, min(100, progress_for(layout, "eventProgress", 50)))

    font_status = load_font(text_size_for(layout, "eventStatus", 11) * density)
    font_label = load_font(text_size_for(layout, "eventLabel", 20) * density, bold=True)
    font_time = load_font(text_size_for(layout, "eventTime", 22) * density, bold=True)
    font_remaining = load_font(text_size_for(layout, "eventRemaining", 12) * density)
    font_icon = load_font(text_size_for(layout, "eventIcon", 24) * density)
    font_percent = load_font(text_size_for(layout, "progressText", 11) * density)

    text_rows = [
        (event_status, font_status, (189, 189, 189, 255), 0),
        (event_label, font_label, (255, 255, 255, 255), 4 * density),
        (event_time, font_time, (255, 255, 255, 255), 2 * density),
        (event_remaining, font_remaining, (208, 208, 208, 255), 4 * density),
    ]
    progress_height = 6 * density
    progress_top_margin = 10 * density
    total_left_height = sum(text_height(draw, text, font) + margin_top for text, font, _, margin_top in text_rows) + progress_top_margin + progress_height
    current_y = content_y + (content_height - total_left_height) // 2

    for text, font, fill, margin_top in text_rows:
        current_y += margin_top
        draw.text((left_x0, current_y), text, font=font, fill=fill)
        current_y += text_height(draw, text, font)

    current_y += progress_top_margin
    bar_box = (left_x0, current_y, left_x1, current_y + progress_height)
    draw.rounded_rectangle(bar_box, radius=3 * density, fill=(44, 44, 44, 255))
    progress_right = left_x0 + int((left_x1 - left_x0) * progress / 100)
    draw.rounded_rectangle((left_x0, current_y, progress_right, current_y + progress_height), radius=3 * density, fill=(245, 245, 245, 255))

    ring_size = 48 * density
    right_center_x = right_x0 + right_width // 2
    percent_height = text_height(draw, progress_text, font_percent)
    right_group_height = ring_size + 6 * density + percent_height
    ring_y0 = content_y + (content_height - right_group_height) // 2
    ring_x0 = right_center_x - ring_size // 2
    ring_box = (ring_x0, ring_y0, ring_x0 + ring_size, ring_y0 + ring_size)

    draw.ellipse(ring_box, fill=(23, 23, 23, 255), outline=(106, 106, 106, 255), width=3 * density)
    draw_centered_text(draw, ring_box, event_icon, font_icon, (255, 255, 255, 255))

    percent_box = draw.textbbox((0, 0), progress_text, font=font_percent)
    percent_width = percent_box[2] - percent_box[0]
    percent_y = ring_y0 + ring_size + 6 * density
    draw.text((right_center_x - percent_width / 2, percent_y), progress_text, font=font_percent, fill=(189, 189, 189, 255))

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT_PATH)
    print(f"Rendered {OUT_PATH.relative_to(ROOT)}")


if __name__ == "__main__":
    render()
