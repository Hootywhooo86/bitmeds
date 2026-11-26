"""Draws the Android launcher icons and notification icon.
Run from android/:  python3 tools/gen_assets.py   (needs Pillow)
The syringe and ring are ports of syringeRows()/drawRing() in app/index.html and Sprites.java."""
import math, os
from PIL import Image, ImageDraw, ImageFont

PAL = {'o':'#0b0b16','W':'#f4f1ff','Y':'#facc15','y':'#a16207','O':'#ff9f1c','s':'#9a97b8','k':'#c9c6e0','q':'#2a2a4a'}
RES = 'app/src/main/res'
FONT = os.path.join(RES, 'font/press_start.ttf')

def syringe(f=6, drip=0):
    g = [['.'] * 16 for _ in range(16)]
    def s(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16: g[y][x] = c
    for x in range(4, 12): s(x, 4, 'o'); s(x, 9, 'o')
    for y in range(4, 10): s(4, y, 'o'); s(11, y, 'o')
    s(3, 3, 'o'); s(4, 3, 'o'); s(3, 10, 'o'); s(4, 10, 'o')
    for x in range(5, 11):
        for y in range(5, 9):
            full = x >= 11 - f
            s(x, y, ('W' if y == 5 else 'y' if y == 8 else 'Y') if full else ('k' if y == 5 else 'q'))
    for x in range(6, 11, 2): s(x, 5, 'O' if x >= 11 - f else 's')
    stop = 10 - f
    if stop >= 5:
        for y in range(5, 9): s(stop, y, 'o')
    plate = max(0, min(3, stop - 4))
    for y in range(4, 10): s(plate, y, 'o')
    for x in range(plate + 1, max(stop, 5)):
        if x == 4: continue
        s(x, 6, 'k'); s(x, 7, 's')
    s(12, 6, 'o'); s(12, 7, 'o'); s(12, 8, 'o')
    for x in range(13, 16): s(x, 7, 'k')
    if drip == 1: s(15, 8, 'Y')
    return [''.join(r) for r in g]

GREEN = ['#34d399', '#6ee7b7', '#047857']; TRACK = ['#24244a', '#2f2f5c', '#15152e']

def ring_frame(frac, bg='#101428', grid=52):
    im = Image.new('RGB', (grid, grid), bg); px = im.load()
    c0, rOut, rIn, N = (grid - 1) / 2, 25.0, 20.0, 36
    filled = math.floor(frac * N)
    pts = {}
    for y in range(grid):
        for x in range(grid):
            dx, dy = x - c0, y - c0; d = math.hypot(dx, dy)
            if d < rIn or d > rOut: continue
            t = ((math.atan2(dy, dx) + math.pi / 2) / (2 * math.pi) + 1) % 1
            seg = math.floor(t * N)
            if (t * N) % 1 > 0.74: continue
            pal = GREEN if seg <= filled else TRACK
            shade = 1 if d > rOut - 1.2 else 2 if d < rIn + 1.2 else 0
            pts[(x, y)] = pal[shade]
    for (x, y) in pts:
        if (x + 1, y + 1) not in pts and x + 1 < grid and y + 1 < grid: px[x + 1, y + 1] = (7, 7, 15)
    for (x, y), c in pts.items(): px[x, y] = tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))
    for y, row in enumerate(syringe(6, 1)):
        for x, ch in enumerate(row):
            if ch in PAL:
                c = tuple(int(PAL[ch][i:i + 2], 16) for i in (1, 3, 5))
                for a in range(2):
                    for b in range(2): px[10 + x * 2 + b, 10 + y * 2 + a] = c
    return im

def up(im, s): return im.resize((im.width * s, im.height * s), Image.NEAREST)

# launcher icons
for name, size in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
    base = ring_frame(0.72, bg='#0b0b16')
    s = max(1, size // 52 + 1)
    im = up(base, s).resize((size, size), Image.NEAREST)
    os.makedirs(f'{RES}/mipmap-{name}', exist_ok=True)
    im.save(f'{RES}/mipmap-{name}/ic_launcher.png')

# widget picker previews come from real renders: WidgetTest.rendersWidgets (single_0.png, list_1.png)

# notification icon: white silhouette of the syringe as a vector
rows = syringe(6, 0); rects = []
for y, row in enumerate(rows):
    x = 0
    while x < 16:
        if row[x] == '.': x += 1; continue
        w = 1
        while x + w < 16 and row[x + w] != '.': w += 1
        rects.append(f'M{x},{y}h{w}v1h-{w}z'); x += w
open(f'{RES}/drawable/ic_stat_syringe.xml', 'w').write(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="16" android:viewportHeight="16">
    <path android:fillColor="#FFFFFFFF" android:pathData="{''.join(rects)}" />
</vector>
''')
print('ok')
