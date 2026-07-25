"""Regenerates the derived energy bar skins from the default texture set.

The default skin in assets/mixenergy/textures/gui/energy_bar is hand-drawn and is the
only set edited by hand. The other skins are produced from it by a colour filter so
every skin keeps the exact pixel geometry the HUD renderer expects:

  aqua      - hue rotation of the default gold fill into light blue, greys untouched.
  amethyst  - purple recolour plus a redrawn gem in place of the lightning bolt.

Run from the repository root:

    python tools/generate_bar_skins.py
"""

import colorsys
import os
from collections import Counter

from PIL import Image

TEXTURE_ROOT = os.path.join(
    "src", "main", "resources", "assets", "mixenergy", "textures", "gui", "energy_bar"
)

FILE_NAMES = [
    "center.png",
    "energy_bar_left.png",
    "energy_bar_right.png",
    "energy_bar_bg_left.png",
    "energy_bar_bg_right.png",
    "left_frame.png",
    "right_frame.png",
    "left_frame_full.png",
    "right_frame_full.png",
] + ["center_full_%d.png" % index for index in range(1, 19)]

# Pixels below this saturation are the grey chassis of the bar rather than its fill.
NEUTRAL_SATURATION = 0.12

# Rows and columns of center.png that hold the icon; the rest is border and animation.
ICON_ROWS = range(2, 8)
ICON_COLUMNS = range(1, 10)


def is_neutral(pixel):
    red, green, blue, alpha = pixel
    if alpha == 0:
        return False
    _, saturation, _ = colorsys.rgb_to_hsv(red / 255.0, green / 255.0, blue / 255.0)
    return saturation < NEUTRAL_SATURATION


def recolor(pixel, fill_hue, fill_saturation_scale, neutral_hue, neutral_saturation,
            neutral_value_scale):
    """Maps one pixel through the skin's colour filter, preserving alpha and value."""
    red, green, blue, alpha = pixel
    if alpha == 0:
        return pixel

    hue, saturation, value = colorsys.rgb_to_hsv(red / 255.0, green / 255.0, blue / 255.0)
    if saturation < NEUTRAL_SATURATION:
        if neutral_hue is None:
            return pixel
        hue = neutral_hue
        saturation = neutral_saturation
        value = value * neutral_value_scale
    else:
        hue = (hue + fill_hue) % 1.0 if fill_saturation_scale is None else fill_hue
        saturation = min(1.0, saturation * (fill_saturation_scale or 1.0))

    red, green, blue = colorsys.hsv_to_rgb(hue, saturation, min(1.0, value))
    return (round(red * 255), round(green * 255), round(blue * 255), alpha)


def apply_filter(image, **options):
    result = Image.new("RGBA", image.size)
    for y in range(image.height):
        for x in range(image.width):
            result.putpixel((x, y), recolor(image.getpixel((x, y)), **options))
    return result


def flatten_icon_fill(image):
    """Clears the lightning bolt so a different icon can be drawn over the plate.

    Each bolt pixel takes the colour of the closest grey plate pixel on its row, which
    keeps the left-to-right shading the "energy full" animation frames paint there.
    """
    original = image.copy()
    for y in ICON_ROWS:
        neutrals = {
            x: original.getpixel((x, y))
            for x in ICON_COLUMNS
            if is_neutral(original.getpixel((x, y)))
        }
        if not neutrals:
            continue
        fallback = Counter(neutrals.values()).most_common(1)[0][0]
        for x in ICON_COLUMNS:
            if is_neutral(original.getpixel((x, y))):
                continue
            nearest = min(neutrals, key=lambda candidate: abs(candidate - x), default=None)
            image.putpixel((x, y), neutrals[nearest] if nearest is not None else fallback)


# A cut gem, lit from the upper left. Keyed by (column, row) inside the icon area.
GEM_HIGHLIGHT = (0xF7, 0xEC, 0xFF, 0xFF)
GEM_TOP = (0xF2, 0xDD, 0xFF, 0xFF)
GEM_LIGHT = (0xE0, 0xAA, 0xFF, 0xFF)
GEM_MID = (0xC2, 0x73, 0xF5, 0xFF)
GEM_DARK = (0x8F, 0x3A, 0xD6, 0xFF)
GEM_BOTTOM = (0x6A, 0x26, 0xA8, 0xFF)
GEM_SHAPE = {
    2: (4, 5, 6),
    3: (3, 4, 5, 6, 7),
    4: (3, 4, 5, 6, 7),
    5: (3, 4, 5, 6, 7),
    6: (4, 5, 6),
    7: (5,),
}


def gem_color(x, y):
    if y == 2:
        # The table of the gem catches the most light.
        return (GEM_TOP, GEM_LIGHT, GEM_MID)[x - 4]
    if y == 7:
        return GEM_BOTTOM
    if (x, y) == (4, 3):
        return GEM_HIGHLIGHT
    if y == 6:
        return GEM_MID if x < 6 else GEM_DARK
    if x <= 4:
        return GEM_LIGHT
    if x == 5:
        return GEM_MID
    return GEM_DARK


def draw_gem(image):
    flatten_icon_fill(image)
    for y, columns in GEM_SHAPE.items():
        for x in columns:
            image.putpixel((x, y), gem_color(x, y))


def build_skin(directory, options, redraw_icon):
    target = os.path.join(TEXTURE_ROOT, directory)
    os.makedirs(target, exist_ok=True)
    for name in FILE_NAMES:
        source = Image.open(os.path.join(TEXTURE_ROOT, name)).convert("RGBA")
        result = apply_filter(source, **options)
        if redraw_icon and name.startswith("center"):
            draw_gem(result)
        result.save(os.path.join(target, name))
    print("wrote %d textures to %s" % (len(FILE_NAMES), target))


def main():
    if not os.path.isdir(TEXTURE_ROOT):
        raise SystemExit("run this script from the repository root")

    # Gold (hue ~35 degrees) rotated onto light blue (hue ~205 degrees). The greys are
    # left alone so the bar keeps the same metal chassis as the default skin.
    build_skin(
        "aqua",
        dict(
            fill_hue=170.0 / 360.0,
            fill_saturation_scale=None,
            neutral_hue=None,
            neutral_saturation=0.0,
            neutral_value_scale=1.0,
        ),
        redraw_icon=False,
    )

    # Every fill tone collapses onto one violet hue and the chassis is tinted with it,
    # so this skin reads as a single material rather than a recoloured default.
    build_skin(
        "amethyst",
        dict(
            fill_hue=278.0 / 360.0,
            fill_saturation_scale=0.95,
            neutral_hue=272.0 / 360.0,
            neutral_saturation=0.42,
            neutral_value_scale=0.92,
        ),
        redraw_icon=True,
    )


if __name__ == "__main__":
    main()
