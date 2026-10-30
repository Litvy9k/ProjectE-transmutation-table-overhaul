"""Generates assets/transtable_overhaul/textures/gui/terminal.png.

Sprite positions must match client/screen/TerminalTexture.java.
Run: python tools/gen_textures.py [--preview out.png]
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/transtable_overhaul/textures/gui/terminal.png"

CLEAR = (0, 0, 0, 0)
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
PANEL = (198, 198, 198, 255)
SHADOW = (85, 85, 85, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_FILL = (139, 139, 139, 255)
GHOST = (118, 118, 118, 255)
DISPLAY = (16, 18, 26, 255)
TRACK = (116, 116, 116, 255)
GOLD = (255, 214, 92, 255)
GOLD_DARK = (176, 128, 32, 255)
GREEN = (92, 200, 92, 255)
RED = (220, 72, 72, 255)
BLUE = (110, 150, 220, 255)
ICON = (64, 64, 64, 255)

WIDTH = 195
HEADER_H = 17
ROW_H = 18
BOTTOM_H = 118


def rect(img, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), color)


def panel(img, x0, y0, w, h):
    """Vanilla style window: rounded black outline, white top/left bevel, dark bottom/right bevel."""
    rect(img, x0, y0, w, h, PANEL)
    for x in range(w):
        for y in range(h):
            edge_x = x in (0, w - 1)
            edge_y = y in (0, h - 1)
            if (edge_x and y < 2 or edge_x and y > h - 3) and (edge_y or x in (0, w - 1)):
                # rounded corners
                if (x in (0, w - 1) and y in (0, 1, h - 2, h - 1)) or (y in (0, h - 1) and x in (0, 1, w - 2, w - 1)):
                    img.putpixel((x0 + x, y0 + y), CLEAR)
                    continue
            if edge_x or edge_y:
                img.putpixel((x0 + x, y0 + y), BLACK)
            elif x in (1, 2) and y < h - 3 or y in (1, 2) and x < w - 3:
                img.putpixel((x0 + x, y0 + y), WHITE)
            elif x in (w - 3, w - 2) and y > 2 or y in (h - 3, h - 2) and x > 2:
                img.putpixel((x0 + x, y0 + y), SHADOW)
    # inner corner pixels
    for (x, y, c) in ((1, 1, BLACK), (w - 2, 1, BLACK), (1, h - 2, BLACK), (w - 2, h - 2, BLACK)):
        img.putpixel((x0 + x, y0 + y), c)
    for (x, y) in ((1, 2), (2, 1)):
        img.putpixel((x0 + x, y0 + y), WHITE)
    for (x, y) in ((w - 3, h - 2), (w - 2, h - 3)):
        img.putpixel((x0 + x, y0 + y), SHADOW)


def sunken(img, x0, y0, w, h, fill=SLOT_FILL):
    """Slot style recessed box: dark top/left, white bottom/right."""
    rect(img, x0, y0, w, h, fill)
    for x in range(w):
        img.putpixel((x0 + x, y0), SLOT_DARK)
        img.putpixel((x0 + x, y0 + h - 1), WHITE)
    for y in range(h):
        img.putpixel((x0, y0 + y), SLOT_DARK)
        img.putpixel((x0 + w - 1, y0 + y), WHITE)
    img.putpixel((x0 + w - 1, y0), fill)
    img.putpixel((x0, y0 + h - 1), fill)


def slot(img, x, y):
    sunken(img, x, y, 18, 18)


def raised(img, x0, y0, w, h, fill, light=WHITE, dark=SHADOW, outline=None):
    rect(img, x0, y0, w, h, fill)
    for x in range(w):
        img.putpixel((x0 + x, y0), light)
        img.putpixel((x0 + x, y0 + h - 1), dark)
    for y in range(h):
        img.putpixel((x0, y0 + y), light)
        img.putpixel((x0 + w - 1, y0 + y), dark)
    if outline:
        for x in range(w):
            img.putpixel((x0 + x, y0), outline)
            img.putpixel((x0 + x, y0 + h - 1), outline)
        for y in range(h):
            img.putpixel((x0, y0 + y), outline)
            img.putpixel((x0 + w - 1, y0 + y), outline)


def draw_mask(img, x0, y0, rows, colors):
    """rows: list of strings; each char is a key into colors ('.' = skip)."""
    for y, line in enumerate(rows):
        for x, ch in enumerate(line):
            if ch != "." and ch != " ":
                img.putpixel((x0 + x, y0 + y), colors[ch])


# 3x5 pixel font for icon labels
FONT = {
    "A": ["###", "#.#", "###", "#.#", "#.#"],
    "Z": ["###", "..#", ".#.", "#..", "###"],
    "E": ["###", "#..", "##.", "#..", "###"],
    "M": ["#.#", "###", "###", "#.#", "#.#"],
    "C": ["###", "#..", "#..", "#..", "###"],
    "O": ["###", "#.#", "#.#", "#.#", "###"],
    "D": ["##.", "#.#", "#.#", "#.#", "##."],
    "@": ["###", "#.#", "###", "#..", "###"],
}


def text(img, x, y, s, color):
    for i, ch in enumerate(s):
        draw_mask(img, x + i * 4, y, FONT[ch], {"#": color})


# ---------------------------------------------------------------- ghost icons (16x16, drawn inside empty slots)

FLAME = [
    "................",
    ".......#........",
    "......##........",
    "......###.......",
    ".....####.#.....",
    ".....#####......",
    "....######.#....",
    "....#######.....",
    "...####.####....",
    "...###...###....",
    "...##.....##....",
    "...##.....##....",
    "....##...##.....",
    ".....#####......",
    "................",
    "................",
]
UNLEARN = [
    "................",
    "..##########....",
    "..#........#....",
    "..#.##..##.#....",
    "..#..####..#....",
    "..#...##...#....",
    "..#..####..#....",
    "..#.##..##.#....",
    "..#........#....",
    "..#........#....",
    "..##########....",
    "...#########....",
    "................",
    "................",
    "................",
    "................",
]
STAR = [
    "................",
    ".......##.......",
    ".......##.......",
    "......####......",
    "......####......",
    ".##############.",
    "..############..",
    "....########....",
    ".....######.....",
    "....###..###....",
    "....##....##....",
    "...##......##...",
    "................",
    "................",
    "................",
    "................",
]
LOCK = [
    "................",
    "......####......",
    ".....#....#.....",
    "....#......#....",
    "....#......#....",
    "....#......#....",
    "...##########...",
    "...##########...",
    "...####..####...",
    "...####..####...",
    "...#####.####...",
    "...##########...",
    "...##########...",
    "................",
    "................",
    "................",
]


def ghost(img, x, y, mask):
    draw_mask(img, x, y + 1, mask, {"#": GHOST})


# ---------------------------------------------------------------- toolbar icons (16x16)

def icon_sort_name(img, x, y):
    text(img, x + 2, y + 2, "A", ICON)
    text(img, x + 2, y + 9, "Z", ICON)
    draw_mask(img, x + 9, y + 2, [
        "..#..", ".###.", "#.#.#", "..#..", "..#..", "..#..", "..#..", "..#..", "#.#.#", ".###.", "..#..",
    ], {"#": BLUE})


def icon_sort_emc(img, x, y):
    # gold gem
    draw_mask(img, x + 1, y + 2, [
        "...########...",
        "..#hhoooooo#..",
        ".#hoooooooooo#",
        "#oooooooooooo#",
        "##############",
        ".#oooooooooo#.",
        "..#oooooooo#..",
        "...#oooooo#...",
        "....#oooo#....",
        ".....#oo#.....",
        "......##......",
    ], {"#": GOLD_DARK, "o": GOLD, "h": WHITE})


def icon_sort_mod(img, x, y):
    # puzzle piece
    draw_mask(img, x + 2, y + 1, [
        "....##....",
        "...####...",
        "##########",
        "##########",
        "########..",
        "#######...",
        "########..",
        "##########",
        "##########",
    ], {"#": BLUE})
    text(img, x + 2, y + 10, "@", ICON)


def icon_arrow(img, x, y, up):
    bars = [
        "##..........",
        "####........",
        "######......",
        "########....",
    ]
    rows = []
    for i, b in enumerate(bars if up else list(reversed(bars))):
        rows += [b, "............"]
    draw_mask(img, x + 2, y + 4, rows[:-1], {"#": ICON})
    arrow_up = ["..#..", ".###.", "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."]
    draw_mask(img, x + 10, y + 3, arrow_up if up else list(reversed(arrow_up)), {"#": BLUE})


def icon_view(img, x, y, affordable):
    for gy in range(3):
        for gx in range(3):
            filled = not affordable or (gx + gy) % 2 == 0
            raised(img, x + 2 + gx * 4, y + 2 + gy * 4, 3, 3, GOLD if filled and affordable else ICON if filled else SLOT_FILL,
                   light=GOLD if affordable and filled else ICON if filled else SLOT_FILL,
                   dark=GOLD_DARK if affordable and filled else SLOT_DARK if filled else SLOT_FILL)
    if affordable:
        draw_mask(img, x + 7, y + 8, [
            ".......#",
            "......##",
            "#....##.",
            "##..##..",
            ".####...",
            "..##....",
        ], {"#": GREEN})


def icon_size(img, x, y, height):
    top = 14 - height
    rect(img, x + 4, y + top, 8, height, ICON)
    rect(img, x + 5, y + top + 1, 6, height - 2, SLOT_FILL)
    for gy in range(y + top + 2, y + 13, 3):
        rect(img, x + 6, gy, 4, 1, ICON)


def icon_clear(img, x, y):
    for i in range(10):
        for (px, py) in ((3 + i, 3 + i), (12 - i, 3 + i)):
            img.putpixel((x + px, y + py), RED)
            if px + 1 < 14:
                img.putpixel((x + px + 1, y + py), RED)


SWAP_ARROWS = [
    ".......#....",
    ".......##...",
    "###########.",
    ".......##...",
    ".......#....",
    "....#.......",
    "...##.......",
    ".###########",
    "...##.......",
    "....#.......",
]
BROWN = (139, 94, 52, 255)
BROWN_DARK = (92, 60, 30, 255)


def icon_source(img, x, y, inventory):
    # Arrows centered, the source marker sits on top in the corner
    draw_mask(img, x + 2, y + 3, SWAP_ARROWS, {"#": ICON})
    marker(img, x, y, inventory)


CYCLE_ARROWS = [
    "....#####...",
    "..##.....#..",
    ".#........#.",
    ".#......#####",
    "#........###.",
    "#.........#..",
    "..#.........#",
    ".###........#",
    "#####......#.",
    ".#........#.",
    "..#.....##..",
    "...#####....",
]


def icon_recipes(img, x, y):
    draw_mask(img, x + 2, y + 2, CYCLE_ARROWS, {"#": ICON})


MARKER_GEM = [
    ".#####.",
    "#hoooo#",
    "#######",
    ".#ooo#.",
    "..#o#..",
    "...#...",
    ".......",
]
MARKER_CHEST = [
    "#######",
    "#ooooo#",
    "###y###",
    "#ooooo#",
    "#ooooo#",
    "#######",
    ".......",
]


def marker(img, x, y, inventory):
    if inventory:
        draw_mask(img, x, y, MARKER_CHEST, {"#": BROWN_DARK, "o": BROWN, "y": GOLD})
    else:
        draw_mask(img, x, y, MARKER_GEM, {"#": GOLD_DARK, "o": GOLD, "h": WHITE})


def crafting_section(img, x0, y0):
    """3x3 grid, arrow and result slot. Drawn on a taller panel and cropped so it tiles with the other slices."""
    tmp = Image.new("RGBA", (WIDTH, 100), CLEAR)
    panel(tmp, 0, 0, WIDTH, 100)
    top = 20
    for r in range(3):
        for c in range(3):
            slot(tmp, 25 + c * 18, top + r * 18)
    for r in range(15):
        head = 7 - abs(r - 7)
        for x in range(14, 14 + head + 1):
            tmp.putpixel((88 + x, top + 20 + r), SLOT_FILL)
        if 5 <= r <= 9:
            for x in range(0, 14):
                tmp.putpixel((88 + x, top + 20 + r), SLOT_FILL)
    sunken(tmp, 119, top + 14, 26, 26)
    img.alpha_composite(tmp.crop((0, top, WIDTH, top + 58)), (x0, y0))


def blue_tint(source):
    """A ProjectE transmutation texture with its light grey parts tinted light blue."""
    im = Image.open(Path(__file__).with_name(source)).convert("RGBA")
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if max(r, g, b) - min(r, g, b) <= 12 and r >= 140:
                im.putpixel((x, y), (min(255, int(r * 0.88)), min(255, int(r * 1.17)), min(255, int(r * 1.38)), a))
    return im


def icon_sync(img, x, y, on):
    color = GREEN if on else ICON
    draw_mask(img, x + 1, y + 1, [
        "..####......",
        ".#....#.....",
        "#......#....",
        "#......#....",
        "#......#....",
        "#......#....",
        ".#....#.....",
        "..####.#....",
        ".......##...",
        "........##..",
        ".........##.",
        "..........#.",
    ], {"#": color})
    if on:
        draw_mask(img, x + 9, y + 9, ["..#..", ".###.", "#####", ".###.", "..#.."], {"#": GREEN})
    else:
        draw_mask(img, x + 9, y + 9, ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"], {"#": RED})


# ---------------------------------------------------------------- assembly

def build():
    img = Image.new("RGBA", (256, 256), CLEAR)

    # Main panel drawn with exactly one grid row, then used as three slices: header / row / bottom
    total_h = HEADER_H + ROW_H + BOTTOM_H
    panel(img, 0, 0, WIDTH, total_h)
    # Search field
    sunken(img, 80, 3, 92, 12, DISPLAY)
    # Grid row slot frames
    for c in range(9):
        slot(img, 7 + c * 18, HEADER_H)
    # Scrollbar track: top border in header, plain inner in the row, bottom border in the bottom slice
    rect(img, 174, HEADER_H - 1, 14, ROW_H + 2, TRACK)
    rect(img, 174, HEADER_H - 1, 14, 1, SLOT_DARK)
    rect(img, 174, HEADER_H - 1, 1, ROW_H + 2, SLOT_DARK)
    rect(img, 187, HEADER_H - 1, 1, ROW_H + 2, WHITE)
    rect(img, 175, HEADER_H + ROW_H, 13, 1, WHITE)

    b = HEADER_H + ROW_H
    # EMC display + burn/unlearn slots
    sunken(img, 7, b + 3, 124, 18, DISPLAY)
    slot(img, 133, b + 3)
    ghost(img, 134, b + 4, FLAME)
    slot(img, 151, b + 3)
    ghost(img, 152, b + 4, UNLEARN)
    # Player inventory
    for r in range(3):
        for c in range(9):
            slot(img, 7 + c * 18, b + 35 + r * 18)
    for c in range(9):
        slot(img, 7 + c * 18, b + 93)

    # Side panel: Klein stars + lock
    px = 196
    panel(img, px, 0, 50, 112)
    for r in range(4):
        for c in range(2):
            slot(img, px + 7 + c * 18, 7 + r * 18)
            ghost(img, px + 8 + c * 18, 8 + r * 18, STAR)
    rect(img, px + 5, 82, 40, 1, SHADOW)
    rect(img, px + 5, 83, 40, 1, WHITE)
    slot(img, px + 16, 87)
    ghost(img, px + 17, 88, LOCK)

    # Scrollbar thumbs
    raised(img, 0, 160, 12, 15, PANEL)
    for gy in (5, 7, 9):
        rect(img, 3, 160 + gy, 6, 1, SHADOW)
    raised(img, 12, 160, 12, 15, SLOT_FILL, light=PANEL, dark=SLOT_DARK)

    # Toolbar button backgrounds
    for i, fill in enumerate((PANEL, (200, 214, 240, 255))):
        bx = 24 + i * 16
        rect(img, bx, 160, 16, 16, SLOT_DARK)
        raised(img, bx + 1, 161, 14, 14, fill)

    # Toolbar icons
    iy = 176
    icon_sort_name(img, 0, iy)
    icon_sort_emc(img, 16, iy)
    icon_sort_mod(img, 32, iy)
    icon_arrow(img, 48, iy, True)
    icon_arrow(img, 64, iy, False)
    icon_view(img, 80, iy, False)
    icon_view(img, 96, iy, True)
    icon_size(img, 112, iy, 6)
    icon_size(img, 128, iy, 9)
    icon_size(img, 144, iy, 12)
    icon_sync(img, 160, iy, False)
    icon_sync(img, 176, iy, True)
    icon_clear(img, 192, iy)
    icon_source(img, 208, iy, False)
    icon_source(img, 224, iy, True)
    icon_recipes(img, 240, iy)
    # Grid slot source markers (7x7)
    marker(img, 56, 160, False)
    marker(img, 56, 168, True)

    crafting_section(img, 0, 192)
    return img


def logo():
    """32x32 pixel-art icon: a terminal screen with an item grid and an EMC gem in front."""
    img = Image.new("RGBA", (32, 32), CLEAR)
    raised(img, 1, 1, 30, 26, PANEL, outline=None)
    rect(img, 0, 2, 1, 24, BLACK)
    rect(img, 31, 2, 1, 24, BLACK)
    rect(img, 2, 0, 28, 1, BLACK)
    rect(img, 2, 27, 28, 1, BLACK)
    sunken(img, 3, 3, 26, 20, DISPLAY)
    cells = [(255, 85, 85), (85, 170, 255), (85, 220, 110), (255, 200, 60), (190, 110, 255),
             (240, 240, 240), (255, 140, 40), (90, 230, 230), (160, 160, 160)]
    for gy in range(4):
        for gx in range(5):
            r, g, bl = cells[(gx * 3 + gy * 2) % len(cells)]
            rect(img, 5 + gx * 5, 5 + gy * 4, 3, 3, (r, g, bl, 255))
            img.putpixel((5 + gx * 5, 5 + gy * 4), (min(255, r + 60), min(255, g + 60), min(255, bl + 60), 255))
    # stand
    rect(img, 12, 28, 8, 2, SHADOW)
    rect(img, 9, 30, 14, 2, SLOT_DARK)
    # ProjectE's Philosopher's Stone in the corner (texture from ProjectE, MIT licensed)
    stone = Image.open(Path(__file__).with_name("projecte_philosophers_stone.png")).convert("RGBA")
    img.alpha_composite(stone, (16, 14))
    return img


def preview(atlas, rows=5, scale=3):
    """Composes the terminal like the screen does, for eyeballing the texture."""
    h = HEADER_H + rows * ROW_H + BOTTOM_H
    out = Image.new("RGBA", (18 + 2 + WIDTH + 2 + 50 + 4, h + 4), (40, 40, 48, 255))
    ox = 20
    out.alpha_composite(atlas.crop((0, 0, WIDTH, HEADER_H)), (ox, 2))
    for r in range(rows):
        out.alpha_composite(atlas.crop((0, HEADER_H, WIDTH, HEADER_H + ROW_H)), (ox, 2 + HEADER_H + r * ROW_H))
    out.alpha_composite(atlas.crop((0, HEADER_H + ROW_H, WIDTH, HEADER_H + ROW_H + BOTTOM_H)), (ox, 2 + HEADER_H + rows * ROW_H))
    out.alpha_composite(atlas.crop((196, 0, 246, 112)), (ox + WIDTH + 2, 2))
    out.alpha_composite(atlas.crop((0, 160, 12, 175)), (ox + 175, 2 + 17 + 20))
    for i, icon in enumerate((1, 4, 5, 9, 10)):
        out.alpha_composite(atlas.crop((24, 160, 40, 176)), (ox - 18, 2 + 8 + i * 18))
        out.alpha_composite(atlas.crop((icon * 16, 176, icon * 16 + 16, 192)), (ox - 18, 2 + 8 + i * 18))
    return out.resize((out.width * scale, out.height * scale), Image.NEAREST)


if __name__ == "__main__":
    atlas = build()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(OUT)
    print("wrote", OUT)
    block_top = ROOT / "src/main/resources/assets/transtable_overhaul/textures/block/transmutation_crafting_table_top.png"
    block_top.parent.mkdir(parents=True, exist_ok=True)
    blue_tint("projecte_transmutation_top.png").save(block_top)
    portable = ROOT / "src/main/resources/assets/transtable_overhaul/textures/item/portable_transmutation_crafting_table.png"
    portable.parent.mkdir(parents=True, exist_ok=True)
    blue_tint("projecte_transmutation_tablet.png").save(portable)
    icon = logo()
    mod_logo = ROOT / "src/main/resources/transtable_overhaul.png"
    icon.resize((128, 128), Image.NEAREST).save(mod_logo)
    avatar = ROOT / "docs/curseforge_logo.png"
    avatar.parent.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (400, 400), (40, 44, 56, 255))
    canvas.alpha_composite(icon.resize((352, 352), Image.NEAREST), (24, 24))
    canvas.save(avatar)
    print("wrote", mod_logo, avatar)
    if "--preview" in sys.argv:
        target = Path(sys.argv[sys.argv.index("--preview") + 1])
        preview(atlas).save(target)
        atlas.resize((768, 768), Image.NEAREST).save(target.with_name(target.stem + "_atlas.png"))
        print("wrote", target)
