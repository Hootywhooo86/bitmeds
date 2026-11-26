package com.bitmeds.app;

/**
 * Port of render() and the 6 widget animation styles in app/pixelart.js — keep the two in step
 * (PixelParityTest-style check: android/tools/parity.sh renders both and compares every pixel).
 * Pure Java (no Android classes) so it can be tested on a plain JVM. Pixels are 0xRRGGBB.
 */
final class Pixel {
    static final int G = 52;
    static final String[] STYLES = {"ring", "battery", "hourglass", "heart", "tank", "hop", "rocket"};
    static final String DEFAULT_COLOR = "#34d399", DEFAULT_BG = "#101428";
    private static final int[] WHITE = {255, 255, 255}, BLACK = {0, 0, 0}, OUT = {7, 7, 15}, RED = {255, 93, 93}, YEL = {250, 204, 21};

    // ---- colour helpers ----
    static boolean validHex(String c) { return c != null && c.matches("#[0-9a-fA-F]{6}"); }

    static int[] hex(String c) {
        return new int[]{Integer.parseInt(c.substring(1, 3), 16), Integer.parseInt(c.substring(3, 5), 16), Integer.parseInt(c.substring(5, 7), 16)};
    }

    static int[] mix(int[] c, int[] d, double t) {
        return new int[]{(int) Math.round(c[0] + (d[0] - c[0]) * t), (int) Math.round(c[1] + (d[1] - c[1]) * t), (int) Math.round(c[2] + (d[2] - c[2]) * t)};
    }

    static int pack(int[] c) { return (c[0] << 16) | (c[1] << 8) | c[2]; }

    static int[] unpack(int p) { return new int[]{p >> 16 & 255, p >> 8 & 255, p & 255}; }

    static int[][] shades(int[] c) { return new int[][]{c, mix(c, WHITE, 0.45), mix(c, BLACK, 0.45)}; }

    // ---- icons ----
    static String[] iconRows(String icon, int k) {
        if ("syringe".equals(icon)) return syringeRows(6, new int[]{0, 1, 2, 3}[k & 3]);
        String[] r = SpriteData.G.get(icon);
        return r != null ? r : SpriteData.G.get("cross");
    }

    static String[] syringeRows(int f, int drip) {
        char[][] g = new char[16][16];
        for (char[] r : g) java.util.Arrays.fill(r, '.');
        for (int x = 4; x <= 11; x++) { s(g, x, 4, 'o'); s(g, x, 9, 'o'); }
        for (int y = 4; y <= 9; y++) { s(g, 4, y, 'o'); s(g, 11, y, 'o'); }
        s(g, 3, 3, 'o'); s(g, 4, 3, 'o'); s(g, 3, 10, 'o'); s(g, 4, 10, 'o');
        for (int x = 5; x <= 10; x++) for (int y = 5; y <= 8; y++) {
            boolean full = x >= 11 - f;
            s(g, x, y, full ? (y == 5 ? 'W' : y == 8 ? 'a' : 'A') : (y == 5 ? 'k' : 'q'));
        }
        for (int x = 6; x <= 10; x += 2) s(g, x, 5, x >= 11 - f ? 'L' : 's');
        int stop = 10 - f;
        if (stop >= 5) for (int y = 5; y <= 8; y++) s(g, stop, y, 'o');
        int plate = Math.max(0, Math.min(3, stop - 4));
        for (int y = 4; y <= 9; y++) s(g, plate, y, 'o');
        for (int x = plate + 1; x < Math.max(stop, 5); x++) { if (x == 4) continue; s(g, x, 6, 'k'); s(g, x, 7, 's'); }
        s(g, 12, 6, 'o'); s(g, 12, 7, 'o'); s(g, 12, 8, 'o');
        for (int x = 13; x <= 15; x++) s(g, x, 7, 'k');
        if (drip == 1) s(g, 15, 8, 'A');
        if (drip == 2) { s(g, 15, 8, 'A'); s(g, 15, 9, 'A'); }
        if (drip == 3) s(g, 15, 11, 'A');
        String[] out = new String[16];
        for (int y = 0; y < 16; y++) out[y] = new String(g[y]);
        return out;
    }

    private static void s(char[][] g, int x, int y, char c) { if (x >= 0 && x < 16 && y >= 0 && y < 16) g[y][x] = c; }

    private static int[] charColor(char ch, int[][] ac) {
        if (ch == 'A') return ac[0];
        if (ch == 'a') return ac[2];
        if (ch == 'L') return ac[1];
        int p = SpriteData.pal(ch);
        return p < 0 ? null : unpack(p);
    }

    // ---- renderer ----
    private static final class C {
        int[] px; int k, st, wob, bob; double frac; int[][] T, TR, DIM, ac; String icon;
    }

    /** @param state 0 = ok, 1 = soon, 2 = due;  k = animation frame 0..3 */
    static int[] render(String style, String icon, String accent, String bg, double frac, int state, int k) {
        int[] b = hex(validHex(bg) ? bg : DEFAULT_BG);
        int[] px = new int[G * G];
        java.util.Arrays.fill(px, pack(b));
        C c = new C();
        c.px = px;
        c.ac = shades(hex(validHex(accent) ? accent : DEFAULT_COLOR));
        c.st = state;
        c.k = k & 3;
        c.frac = Math.max(0, Math.min(1, frac));
        c.T = state == 2 ? shades(RED) : state == 1 ? shades(YEL) : c.ac;
        c.TR = new int[][]{mix(b, WHITE, 0.10), mix(b, WHITE, 0.18), mix(b, BLACK, 0.35)};
        c.DIM = new int[][]{mix(c.T[0], b, 0.72), mix(c.T[0], b, 0.6), mix(c.T[0], BLACK, 0.72)};
        c.icon = icon == null ? "syringe" : icon;
        c.wob = state == 2 ? ((c.k & 1) == 1 ? 1 : -1) : 0;
        c.bob = state != 2 && !"syringe".equals(icon) && (c.k & 1) == 1 ? -1 : 0;
        switch (style == null ? "ring" : style) {
            case "battery": battery(c); break;
            case "hourglass": hourglass(c); break;
            case "heart": heart(c); break;
            case "tank": tank(c); break;
            case "hop": hop(c); break;
            case "rocket": rocket(c); break;
            default: ring(c);
        }
        return px;
    }

    private static void put(int[] px, int x, int y, int[] col) { if (x >= 0 && x < G && y >= 0 && y < G) px[y * G + x] = pack(col); }

    private static void icon(C c, int ox, int oy, int scale) {
        String[] rows = iconRows(c.icon, c.k);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int[] col = charColor(rows[y].charAt(x), c.ac);
            if (col == null) continue;
            for (int a = 0; a < scale; a++) for (int b = 0; b < scale; b++) put(c.px, ox + x * scale + b, oy + y * scale + a, col);
        }
    }

    private static int fmod(int a, int n) { return ((a % n) + n) % n; }

    // 1. RING
    private static void ring(C c) {
        final int N = 36;
        double c0 = (G - 1) / 2.0, rOut = 25.0, rIn = 20.0;
        int filled = (int) Math.floor(c.frac * N), chase = c.k * 9;
        boolean blink = (c.k & 1) == 0;
        boolean[] mask = new boolean[G * G];
        int[][] col = new int[G * G][];
        for (int y = 0; y < G; y++) for (int x = 0; x < G; x++) {
            double dx = x - c0, dy = y - c0, d = Math.sqrt(dx * dx + dy * dy);
            if (d < rIn || d > rOut) continue;
            double t = (Math.atan2(dy, dx) + Math.PI / 2) / (Math.PI * 2);
            t = (t + 1) % 1;
            int seg = (int) Math.floor(t * N);
            if ((t * N) % 1 > 0.74) continue;
            int[][] pal = c.TR;
            if (c.st == 2) pal = fmod(seg - chase, N) < 9 ? c.T : c.DIM;
            else if (seg < filled) pal = c.T;
            else if (seg == filled) pal = blink ? c.T : c.TR;
            int shade = d > rOut - 1.2 ? 1 : d < rIn + 1.2 ? 2 : 0;
            mask[y * G + x] = true;
            col[y * G + x] = pal[shade];
        }
        for (int y = 0; y < G - 1; y++) for (int x = 0; x < G - 1; x++)
            if (mask[y * G + x] && !mask[(y + 1) * G + x + 1]) put(c.px, x + 1, y + 1, OUT);
        for (int i = 0; i < G * G; i++) if (mask[i]) c.px[i] = pack(col[i]);
        if (c.st != 2) {
            double a = -Math.PI / 2 + c.k / 4.0 * Math.PI * 2 + c.frac * Math.PI * 2;
            int sx = (int) Math.round(c0 + Math.cos(a) * 17.5), sy = (int) Math.round(c0 + Math.sin(a) * 17.5);
            put(c.px, sx, sy, c.T[1]); put(c.px, sx + 1, sy, c.T[1]); put(c.px, sx, sy + 1, c.T[1]); put(c.px, sx + 1, sy + 1, c.T[1]);
        }
        icon(c, 10 + c.wob, 10 + c.bob, 2);
    }

    // 2. BATTERY
    private static void battery(C c) {
        for (int x = 4; x <= 45; x++) for (int y = 20; y <= 43; y++)
            put(c.px, x + c.wob, y, (x < 6 || x > 43 || y < 22 || y > 41) ? OUT : c.TR[2]);
        for (int x = 46; x <= 48; x++) for (int y = 27; y <= 36; y++) put(c.px, x + c.wob, y, (x < 48 && y > 28 && y < 35) ? c.TR[1] : OUT);
        int filled = (int) Math.floor(c.frac * 9), glint = filled > 0 ? (c.k * 2) % filled : -1;
        for (int i = 0; i < 9; i++) {
            int[][] pal = null;
            if (c.st == 2) pal = (i + c.k) % 2 == 0 ? c.T : c.DIM;
            else if (i < filled) pal = c.T;
            else if (i == filled && (c.k & 1) == 0) pal = c.T;
            if (pal == null) continue;
            int x0 = 7 + i * 4;
            for (int x = x0; x <= x0 + 2; x++) for (int y = 23; y <= 40; y++)
                put(c.px, x + c.wob, y, y == 23 ? pal[1] : y == 40 ? pal[2] : (c.st != 2 && i == glint && x == x0 + 1) ? pal[1] : pal[0]);
        }
        icon(c, 18 + c.wob, 2 + c.bob, 1);
    }

    // 3. HOURGLASS
    private static void hourglass(C c) {
        int cx = 19 + c.wob;
        for (int x = 6; x <= 32; x++) for (int y = 3; y <= 5; y++) {
            put(c.px, x + c.wob, y, y == 3 ? c.T[1] : c.T[2]);
            put(c.px, x + c.wob, y + 43, y == 3 ? c.T[1] : c.T[2]);
        }
        for (int y = 6; y <= 45; y++) { put(c.px, 7 + c.wob, y, c.T[2]); put(c.px, 31 + c.wob, y, c.T[2]); }
        int hTop = (int) Math.round((1 - c.frac) * 15), hBot = (int) Math.round(c.frac * 15);
        for (int y = 7; y <= 44; y++) {
            double d = Math.abs(y - 25.5);
            int w = Math.min(10, (int) Math.floor(d * 0.75) + 1);
            boolean edge = y == 7 || y == 44;
            for (int x = cx - w - 1; x <= cx + w + 1; x++) {
                boolean outline = edge || x == cx - w - 1 || x == cx + w + 1;
                int[] col = outline ? c.TR[1] : c.TR[2];
                if (!outline) {
                    if (y <= 25 && 25 - y < hTop) col = (25 - y == hTop - 1) ? c.T[1] : c.T[0];
                    if (y >= 26 && y > 43 - hBot) col = (y == 44 - hBot) ? c.T[1] : c.T[0];
                }
                put(c.px, x, y, col);
            }
        }
        if (c.st != 2 && hTop > 0) for (int y = 26; y <= 43 - hBot; y++) if ((y + c.k * 2) % 4 < 2) put(c.px, cx, y, c.T[1]);
        icon(c, 35, 18 + c.bob, 1);
    }

    // 4. HEART
    private static void heart(C c) {
        double s = c.st == 2 ? new double[]{1, 1.09, 0.98, 1.09}[c.k] : new double[]{1, 1.07, 1, 0.96}[c.k];
        double R = 19 * s, cy = 27.75;
        boolean[] m = new boolean[G * G];
        int minY = G, maxY = -1;
        for (int y = 0; y < G; y++) for (int x = 0; x < G; x++) {
            double nx = (x - 25.5) / R, ny = (cy - y) / R, a = nx * nx + ny * ny - 1;
            if (a * a * a - nx * nx * ny * ny * ny <= 0) { m[y * G + x] = true; if (y < minY) minY = y; if (y > maxY) maxY = y; }
        }
        double level = maxY - c.frac * (maxY - minY);
        int surf = (int) Math.ceil(level);
        for (int y = 0; y < G; y++) for (int x = 0; x < G; x++) {
            if (!m[y * G + x]) continue;
            boolean edge = x == 0 || y == 0 || x == G - 1 || y == G - 1 || !m[y * G + x - 1] || !m[y * G + x + 1] || !m[(y - 1) * G + x] || !m[(y + 1) * G + x];
            if (edge) { put(c.px, x, y, OUT); continue; }
            boolean full = y >= level;
            double nx2 = (x - 25.5) / R, ny2 = (cy - y) / R;
            boolean shine = nx2 < -0.42 && nx2 > -0.66 && ny2 > 0.5 && ny2 < 0.7 && !(nx2 < -0.58 && ny2 > 0.62);
            put(c.px, x, y, full ? (shine || y == surf ? c.T[1] : c.T[0]) : (shine ? c.TR[1] : c.TR[0]));
        }
        icon(c, 18 + c.wob, 17 + c.bob, 1);
    }

    // 5. TANK
    private static void tank(C c) {
        final int[] WAVE = {0, 0, 1, 1, 0, 0, -1, -1};
        for (int x = 7; x <= 44; x++) for (int y = 5; y <= 47; y++)
            put(c.px, x, y, (x < 9 || x > 42 || y < 7 || y > 45) ? OUT : c.TR[2]);
        for (int y = 9; y < 45; y += 6) { put(c.px, 40, y, c.TR[1]); put(c.px, 41, y, c.TR[1]); }
        int h = (int) Math.round(c.frac * 39), sy = 46 - h;
        if (h > 0) for (int x = 9; x <= 42; x++) {
            int top = h >= 39 ? 7 : Math.max(7, sy + WAVE[(x + c.k * 2) % 8]);
            for (int y = top; y <= 45; y++)
                put(c.px, x, y, y == top ? ((c.st == 2 && (c.k & 1) == 1) ? c.T[0] : c.T[1]) : y >= 44 ? c.T[2] : x == 11 ? c.T[1] : c.T[0]);
        }
        if (h > 8) for (int i = 0; i < 3; i++) {
            int bx = 15 + i * 9, by = 45 - ((c.k * 3 + i * 7) % (h - 4));
            if (by - 1 > sy + 1) { put(c.px, bx, by, c.T[1]); put(c.px, bx + 1, by, c.T[1]); put(c.px, bx, by - 1, c.T[1]); put(c.px, bx + 1, by - 1, c.T[1]); }
        }
        int iy = Math.max(1, Math.min(30, sy - 15 + ((c.k & 1) == 1 ? 1 : 0)));
        icon(c, 18 + c.wob, iy, 1);
    }

    // 6. HOP
    private static void hop(C c) {
        int[] JUMP = c.st == 2 ? new int[]{-6, 0, -6, 0} : new int[]{0, -3, -5, -3};
        int filled = (int) Math.floor(c.frac * 8);
        for (int i = 0; i < 8; i++) {
            int x0 = 2 + i * 6;
            int[][] pal = i < filled || (c.st == 2) ? c.T : (i == filled && (c.k & 1) == 0) ? c.T : null;
            if (c.st == 2 && (i + c.k) % 2 == 1) pal = c.DIM;
            for (int x = x0; x <= x0 + 4; x++) for (int y = 40; y <= 44; y++) {
                int[] col = pal != null ? (y == 40 ? pal[1] : y == 44 ? pal[2] : pal[0]) : (y == 40 ? c.TR[1] : c.TR[0]);
                put(c.px, x, y, col);
            }
            if (pal != null) put(c.px, x0 + 2, 42, pal[2]);
            for (int x = x0; x <= x0 + 4; x++) put(c.px, x + 1, 45, OUT);
        }
        for (int x = 0; x < G; x++) { put(c.px, x, 46, c.TR[2]); put(c.px, x, 47, c.TR[2]); }
        int ix = 1 + (int) Math.round(c.frac * 18), jump = JUMP[c.k], iy = 10 + jump;
        int sw = 10 + jump;
        for (int x = ix + 16 - (sw >> 1); x < ix + 16 + (sw >> 1); x++) put(c.px, x, 39, c.TR[2]);
        if (c.st != 2 && c.frac > 0) for (int i = 0; i < 3; i++) put(c.px, ix - 2 - i * 3, iy + 18 + ((c.k + i) % 3) * 3, c.T[1]);
        icon(c, ix, iy, 2);
        if (c.st == 2) {
            int ex = Math.min(ix + 33, 48);
            for (int y = 4; y <= 9; y++) { put(c.px, ex, y, c.T[0]); put(c.px, ex + 1, y, c.T[0]); }
            put(c.px, ex, 11, c.T[0]); put(c.px, ex + 1, 11, c.T[0]);
        }
    }

    // 7. ROCKET
    private static void rocket(C c) {
        int[][] ST = {{4, 6}, {13, 3}, {26, 8}, {47, 24}, {33, 30}, {45, 40}, {7, 38}, {29, 44}, {16, 18}, {49, 4}, {3, 26}, {36, 18}};
        for (int i = 0; i < ST.length; i++) if ((i + c.k) % 3 != 0) put(c.px, ST[i][0], ST[i][1], c.TR[1]);
        int[][] moon = c.st == 2 ? c.T : new int[][]{{201, 198, 224}, {244, 241, 255}, {120, 117, 150}};
        for (int y = 2; y <= 18; y++) for (int x = 33; x <= 49; x++) {
            int dx = x - 41, dy = y - 10, d = dx * dx + dy * dy;
            if (d > 49) continue;
            int[] col = d >= 37 ? moon[2] : (dx < -1 && dy < -1 && d < 26) ? moon[1] : moon[0];
            if ((x == 43 && y == 8) || (x == 39 && y == 12) || (x == 44 && y == 13) || (x == 40 && y == 7)) col = moon[2];
            put(c.px, x, y, col);
        }
        for (int x = 0; x < G; x++) for (int y = 48; y < G; y++) put(c.px, x, y, y == 48 ? c.TR[1] : c.TR[2]);
        for (int x = 12; x <= 31; x++) put(c.px, x, 47, c.TR[1]);
        int iy = 20 - (int) Math.round(c.frac * 22) + (c.st == 2 ? 0 : (c.k & 1) == 1 ? -1 : 0), ix = 6 + c.wob;
        for (int y = iy + 30; y < 47; y += 3) put(c.px, 22, y, y < iy + 38 ? c.T[1] : c.T[2]);
        if (c.st != 2 && c.frac < 1) {
            int len = new int[]{3, 5, 4, 6}[c.k];
            int[][] FL = {{250, 204, 21}, {255, 159, 28}, {255, 93, 93}};
            for (int y = 0; y < len; y++) for (int x = 20; x <= 23; x++) {
                if ((x == 20 || x == 23) && y > len - 3) continue;
                put(c.px, x, iy + 25 + y, FL[Math.min(2, y / 2)]);
            }
        }
        icon(c, ix, iy, 2);
        if (c.st == 2 && (c.k & 1) == 0) {
            for (int y = 22; y <= 27; y++) { put(c.px, 44, y, c.T[0]); put(c.px, 45, y, c.T[0]); }
            put(c.px, 44, 29, c.T[0]); put(c.px, 45, 29, c.T[0]);
        }
    }
}
