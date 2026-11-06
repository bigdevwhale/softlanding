"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/*.jpg (1080 px wide renders of the demo, see tools/shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen is cut short under its caption.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONT = ROOT / "app/src/main/res/font"

PAPER = (0xF7, 0xF1, 0xE5)
SKY = (0xA9, 0xD1, 0xEC)
SKY_L = (0xDC, 0xED, 0xF7)
CLOUD = (0xFF, 0xFC, 0xF6)
MEADOW = (0x8F, 0xB5, 0x6C)
MEADOW_D = (0x6E, 0x9A, 0x4E)
INK = (0x1E, 0x26, 0x38)
RED = (0xD2, 0x55, 0x3F)

# The order they appear in the store, with the caption over each: an ink line and a red one.
PAGES = [
    ("1-flight", ("Every day is a flight", "to the next village"), ("Каждый день — полёт", "в следующую деревню")),
    ("2-add", ("Barcode, label photo", "or one line"), ("Штрихкод, фото этикетки", "или одна строка")),
    ("3-amount", ("A plate, a spoon, grams:", "count it your way"), ("Тарелка, ложка, граммы —", "как удобно")),
    ("4-map", ("Land softly, reach", "the next village"), ("Мягкая посадка —", "следующая деревня")),
    ("6-village", ("Every village", "gives a recipe"), ("Каждая деревня", "дарит рецепт")),
    ("5-journal", ("A month of landings", "in the logbook"), ("Месяц посадок", "в бортовом журнале")),
    ("9-dish", ("Weigh the pot once,", "every serving is exact"), ("Взвесь кастрюлю раз —", "и порции точные")),
    ("8-hangar", ("Twelve envelopes", "for soft landings"), ("Двенадцать куполов", "за мягкие посадки")),
]


def font(name, size):
    return ImageFont.truetype(str(FONT / name), size)


def fit(name, lines, size, width):
    while max(font(name, size).getlength(l) for l in lines) > width:
        size -= 2
    return font(name, size)


def cloud(d, x, y, s):
    """Three soft puffs on a flat bottom, like the clouds over the app's meadow."""
    d.ellipse((x, y + 18 * s, x + 60 * s, y + 58 * s), fill=CLOUD)
    d.ellipse((x + 22 * s, y, x + 82 * s, y + 58 * s), fill=CLOUD)
    d.ellipse((x + 58 * s, y + 14 * s, x + 112 * s, y + 58 * s), fill=CLOUD)
    d.rectangle((x + 20 * s, y + 38 * s, x + 92 * s, y + 58 * s), fill=CLOUD)


def sky(w, h, clouds=()):
    """A morning sky fading to the app's paper towards the bottom."""
    g = Image.new("RGB", (w, h), PAPER)
    d = ImageDraw.Draw(g)
    for y in range(h):
        t = min(1.0, y / (h * 0.75))
        c = tuple(round(a + (b - a) * t) for a, b in zip(SKY, PAPER))
        d.line((0, y, w, y), fill=c)
    for x, y, s in clouds:
        cloud(d, x, y, s)
    return g.convert("RGBA")


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=36):
    """A screen on the sky: a soft shadow under rounded corners."""
    sh = Image.new("RGBA", (card.width + 120, card.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((60, 76, 60 + card.width, 76 + card.height), r, fill=(30, 38, 56, 90))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(24)), (x - 60, y - 60))
    g.alpha_composite(rounded(card, r), (x, y))


def screenshot(src, caption):
    W, H = 1080, 1920
    g = sky(W, H, clouds=[(-50, 296, 0.8), (985, 286, 0.8)])
    d = ImageDraw.Draw(g)
    title = fit("prata.ttf", caption, 84, W - 140)
    d.text((W / 2, 140), caption[0], font=title, fill=INK, anchor="mm")
    d.text((W / 2, 245), caption[1], font=title, fill=RED, anchor="mm")
    im = Image.open(src).convert("RGB")
    w = 840
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 350
    # The screen runs off the bottom edge, the way the route goes on past the frame.
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = sky(W, H, clouds=[(390, 40, 0.8), (470, 330, 0.5)])
    d = ImageDraw.Draw(g)
    # Rolling meadow along the bottom.
    d.ellipse((-200, 420, 700, 760), fill=MEADOW)
    d.ellipse((380, 440, 1300, 820), fill=MEADOW_D)
    # The flight screen: the balloon over the fields and what's left for today.
    flight = Image.open(SHOTS / lang / "1-flight.jpg").convert("RGB").crop((0, 0, 1080, 1180))
    flight = flight.resize((round(flight.width * 420 / flight.height), 420), Image.LANCZOS)
    lay(g, flight, W - flight.width - 56, 40, r=22)
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((96, 96), Image.LANCZOS)
    g.alpha_composite(logo, (52, 60))
    name, lines = ("Soft Landing", ["A calorie counter where", "every day is a flight", "to the next village"]) if lang == "en" else \
        ("Мягкая посадка", ["Счётчик калорий, где", "каждый день — полёт", "в следующую деревню"])
    d.text((52, 176), name, font=fit("prata.ttf", [name], 64, 470), fill=INK)
    sub = fit("manrope_semibold.ttf", lines, 30, 450)
    for i, l in enumerate(lines):
        d.text((54, 278 + i * 42), l, font=sub, fill=RED if i == 2 else INK)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def icon():
    """The README logo has transparent rounded corners; Play wants a full square and rounds it itself."""
    im = Image.open(ROOT / "docs/logo.png").convert("RGBA").crop((34, 34, 478, 478))
    im.convert("RGB").resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
