package com.bitmeds.app;

import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Pixel sprites + the progress ring, drawn on a 52x52 pixel grid and scaled up with no smoothing.
 * Grids and colours are copied from SPR / PAL in app/index.html; the syringe
 * and ring are ports of syringeRows() and drawRing() there.
 */
final class Sprites {
    static final int GRID = 52;
    static final int STATE_OK = 0, STATE_SOON = 1, STATE_DUE = 2;
    /** Widget panel colour. RGB_565-exact so the bitmap edge is invisible on the panel. */
    static final int PANEL = 0xFF101428;

    private static final Map<String, String[]> G = new HashMap<>();
    static {
        G.put("pill", new String[]{
            "................",
            "................",
            "................",
            "................",
            "...oooooooooo...",
            "..oGGGGGWWWWWo..",
            ".oGGGGGGWWWWWWo.",
            ".oGgGGGGWWWWkWo.",
            ".oGgGGGGWWWWkWo.",
            ".oGGGGGGWWWWWWo.",
            "..oGGGGGWWWWWo..",
            "...oooooooooo...",
            "................",
            "................",
            "................",
            "................"});
        G.put("tablet", new String[]{
            "................",
            "................",
            ".....oooooo.....",
            "...ooWWWWWWoo...",
            "..oWWWWWWWWWWo..",
            "..oWWWWWWWWWWo..",
            ".oWWWWWWWWWWWWo.",
            ".okkkkkkkkkkkko.",
            ".oWWWWWWWWWWWWo.",
            "..oWWWWWWWWWWo..",
            "..oWWWWWWWWWWo..",
            "...ooWWWWWWoo...",
            ".....oooooo.....",
            "................",
            "................",
            "................"});
        G.put("liquid", new String[]{
            "................",
            "......oooo......",
            "......oRRo......",
            "......oRRo......",
            ".....oooooo.....",
            "....oWWWWWWo....",
            "....oWkWWWWo....",
            "....oPPPPPPo....",
            "....oPpPPPPo....",
            "....oPpPPPPo....",
            "....oPPPPPPo....",
            "....oPPPPPPo....",
            "....oPPPPPPo....",
            ".....oooooo.....",
            "................",
            "................"});
        G.put("inhaler", new String[]{
            "................",
            "....oooo........",
            "....oYYo........",
            "....oYyo........",
            "....oYyo........",
            "....oYyo........",
            "....oYyo........",
            "....oYyooooooo..",
            "....oYYYYYYYYYo.",
            "....oYYYYYYYYYo.",
            "....oYYYYYooooo.",
            "....oooooo......",
            "................",
            "................",
            "................",
            "................"});
        G.put("drops", new String[]{
            "................",
            "........o.......",
            ".......oBo......",
            ".......oBo......",
            "......oBBBo.....",
            ".....oBBBBBo....",
            "....oBBBBBBBo...",
            "....oBkBBBBBo...",
            "...oBBkBBBBBBo..",
            "...oBBkBBBBBBo..",
            "...oBBBBBBBBBo..",
            "....oBBBBBBbo...",
            ".....obbbbbo....",
            "......ooooo.....",
            "................",
            "................"});
        G.put("cream", new String[]{
            "................",
            "................",
            "................",
            "................",
            ".ooooooooooo....",
            ".oWWWWWWWWWoooo.",
            ".oWGGGGGGGWoOOo.",
            ".oWGgGGGGGWoOOo.",
            ".oWGGGGGGGWoOOo.",
            ".oWWWWWWWWWoooo.",
            ".ooooooooooo....",
            "................",
            "................",
            "................",
            "................",
            "................"});
        G.put("patch", new String[]{
            "................",
            "................",
            "..oooooooooooo..",
            "..oWWWWWWWWWWo..",
            "..oWkkkkkkkkWo..",
            "..oWkooooookWo..",
            "..oWkoOOOOokWo..",
            "..oWkoOOOOokWo..",
            "..oWkoOOOOokWo..",
            "..oWkooooookWo..",
            "..oWkkkkkkkkWo..",
            "..oWWWWWWWWWWo..",
            "..oooooooooooo..",
            "................",
            "................",
            "................"});
        G.put("spray", new String[]{
            "................",
            ".......oo.......",
            "......oWWo.....s",
            "......oWWo...s..",
            ".....oooooo.....",
            "....oWWWWWWo....",
            "....oGGGGGGo....",
            "....oGgGGGGo....",
            "....oGgGGGGo....",
            "....oGgGGGGo....",
            "....oGGGGGGo....",
            "....oGGGGGGo....",
            ".....oooooo.....",
            "................",
            "................",
            "................"});
        G.put("other", new String[]{
            "................",
            "................",
            ".....oooooo.....",
            ".....oRRRRo.....",
            ".....oRRRRo.....",
            "..ooooRRRRoooo..",
            "..oRRRRRRRRRRo..",
            "..oRRRRRRRRRRo..",
            "..oRRRRRRRRRRo..",
            "..ooooRRRRoooo..",
            ".....oRRRRo.....",
            ".....oRRRRo.....",
            ".....oooooo.....",
            "................",
            "................",
            "................"});
    }

    static int color(char c) {
        switch (c) {
            case 'o': return 0xFF0B0B16;
            case 'W': return 0xFFF4F1FF;
            case 'G': return 0xFF34D399;
            case 'g': return 0xFF047857;
            case 'Y': return 0xFFFACC15;
            case 'y': return 0xFFA16207;
            case 'R': return 0xFFFF5D5D;
            case 'B': return 0xFF60A5FA;
            case 'b': return 0xFF1D4ED8;
            case 'P': return 0xFFA78BFA;
            case 'p': return 0xFF6D28D9;
            case 'O': return 0xFFFF9F1C;
            case 's': return 0xFF9A97B8;
            case 'k': return 0xFFC9C6E0;
            case 'q': return 0xFF2A2A4A;
            default: return 0;
        }
    }

    /** f = filled oil columns 0..6, drip = 0..3 (port of syringeRows in the web app). */
    static String[] syringe(int f, int drip) {
        char[][] g = new char[16][16];
        for (char[] r : g) java.util.Arrays.fill(r, '.');
        for (int x = 4; x <= 11; x++) { set(g, x, 4, 'o'); set(g, x, 9, 'o'); }
        for (int y = 4; y <= 9; y++) { set(g, 4, y, 'o'); set(g, 11, y, 'o'); }
        set(g, 3, 3, 'o'); set(g, 4, 3, 'o'); set(g, 3, 10, 'o'); set(g, 4, 10, 'o');
        for (int x = 5; x <= 10; x++) for (int y = 5; y <= 8; y++) {
            boolean full = x >= 11 - f;
            set(g, x, y, full ? (y == 5 ? 'W' : y == 8 ? 'y' : 'Y') : (y == 5 ? 'k' : 'q'));
        }
        for (int x = 6; x <= 10; x += 2) set(g, x, 5, x >= 11 - f ? 'O' : 's');
        int stop = 10 - f;
        if (stop >= 5) for (int y = 5; y <= 8; y++) set(g, stop, y, 'o');
        int plate = Math.max(0, Math.min(3, stop - 4));
        for (int y = 4; y <= 9; y++) set(g, plate, y, 'o');
        for (int x = plate + 1; x < Math.max(stop, 5); x++) { if (x == 4) continue; set(g, x, 6, 'k'); set(g, x, 7, 's'); }
        set(g, 12, 6, 'o'); set(g, 12, 7, 'o'); set(g, 12, 8, 'o');
        for (int x = 13; x <= 15; x++) set(g, x, 7, 'k');
        if (drip == 1) set(g, 15, 8, 'Y');
        if (drip == 2) { set(g, 15, 8, 'Y'); set(g, 15, 9, 'Y'); }
        if (drip == 3) set(g, 15, 11, 'Y');
        String[] out = new String[16];
        for (int y = 0; y < 16; y++) out[y] = new String(g[y]);
        return out;
    }

    private static void set(char[][] g, int x, int y, char c) { if (x >= 0 && x < 16 && y >= 0 && y < 16) g[y][x] = c; }

    static String[] rows(String type, int frame) {
        if ("injection".equals(type)) return syringe(6, new int[]{0, 1, 2, 3}[frame & 3]);
        String[] r = G.get(type);
        return r != null ? r : G.get("other");
    }

    // ring palettes: {main, light edge, dark edge}
    private static final int[] GREEN = {0xFF34D399, 0xFF6EE7B7, 0xFF047857}, YELLOW = {0xFFFACC15, 0xFFFDE68A, 0xFFA16207},
            RED = {0xFFFF5D5D, 0xFFFFA3A3, 0xFF991B1B}, TRACK = {0xFF24244A, 0xFF2F2F5C, 0xFF15152E}, DIM = {0xFF4A1F2A, 0xFF5C2633, 0xFF2A1018};

    /** One animation frame (0..3) of the ring + sprite, scaled by {@code scale}. Opaque RGB_565 to keep widget updates small. */
    static Bitmap frame(String type, float frac, int state, int k, int scale) {
        final int N = 36;
        int[] px = new int[GRID * GRID];
        java.util.Arrays.fill(px, PANEL);
        double c0 = (GRID - 1) / 2.0, rOut = 25.0, rIn = 20.0;
        int filled = (int) Math.floor(frac * N);
        boolean blink = (k & 1) == 0;
        int chase = k * 9;
        int[] tone = state == STATE_DUE ? RED : state == STATE_SOON ? YELLOW : GREEN;
        boolean[] ring = new boolean[GRID * GRID];
        int[] col = new int[GRID * GRID];
        for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) {
            double dx = x - c0, dy = y - c0, d = Math.sqrt(dx * dx + dy * dy);
            if (d < rIn || d > rOut) continue;
            double t = (Math.atan2(dy, dx) + Math.PI / 2) / (Math.PI * 2);
            t = (t + 1) % 1;
            int seg = (int) Math.floor(t * N);
            if ((t * N) % 1 > 0.74) continue;
            int[] pal = TRACK;
            if (state == STATE_DUE) pal = Math.floorMod(seg - chase, N) < 9 ? RED : DIM;
            else if (seg < filled) pal = tone;
            else if (seg == filled) pal = blink ? tone : TRACK;
            int shade = d > rOut - 1.2 ? 1 : d < rIn + 1.2 ? 2 : 0;
            ring[y * GRID + x] = true;
            col[y * GRID + x] = pal[shade];
        }
        // 1px drop shadow, then the ring
        for (int y = 0; y < GRID - 1; y++) for (int x = 0; x < GRID - 1; x++)
            if (ring[y * GRID + x] && !ring[(y + 1) * GRID + x + 1]) px[(y + 1) * GRID + x + 1] = 0xFF07070F;
        for (int i = 0; i < px.length; i++) if (ring[i]) px[i] = col[i];
        // orbiting spark
        if (state != STATE_DUE) {
            double a = -Math.PI / 2 + k / 4.0 * Math.PI * 2 + frac * Math.PI * 2;
            int sx = (int) Math.round(c0 + Math.cos(a) * 17.5), sy = (int) Math.round(c0 + Math.sin(a) * 17.5);
            for (int oy = 0; oy < 2; oy++) for (int ox = 0; ox < 2; ox++) put(px, sx + ox, sy + oy, 0xFFC9C6E0);
        }
        // sprite at 2x in the middle, with a little bob (or wobble when due)
        String[] rows = rows(type, k);
        int ox = 10 + (state == STATE_DUE ? ((k & 1) == 0 ? -1 : 1) : 0);
        int oy = 10 + (state != STATE_DUE && !"injection".equals(type) && (k & 1) == 1 ? -1 : 0);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int c = color(rows[y].charAt(x));
            if (c == 0) continue;
            for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) put(px, ox + x * 2 + b, oy + y * 2 + a, c);
        }
        int S = GRID * scale;
        int[] big = new int[S * S];
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) big[y * S + x] = px[(y / scale) * GRID + x / scale];
        return Bitmap.createBitmap(big, S, S, Bitmap.Config.RGB_565);
    }

    private static void put(int[] px, int x, int y, int c) { if (x >= 0 && x < GRID && y >= 0 && y < GRID) px[y * GRID + x] = c; }
}
