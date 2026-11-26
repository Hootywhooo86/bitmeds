package com.bitmeds.app;

import android.graphics.Bitmap;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** A med's look (icon, colour, background, animation style) and its widget animation frames. */
final class Sprites {
    static final int STATE_OK = 0, STATE_SOON = 1, STATE_DUE = 2;
    private static final Map<String, String> TYPE_ICON = new HashMap<>();
    static {
        TYPE_ICON.put("injection", "syringe"); TYPE_ICON.put("pill", "capsule"); TYPE_ICON.put("tablet", "tablet");
        TYPE_ICON.put("liquid", "bottle"); TYPE_ICON.put("inhaler", "inhaler"); TYPE_ICON.put("drops", "drop");
        TYPE_ICON.put("cream", "tube"); TYPE_ICON.put("patch", "patch"); TYPE_ICON.put("spray", "spray");
    }

    static String icon(JSONObject m) {
        String i = m.optString("icon", "");
        if (!i.isEmpty()) return i;
        String t = TYPE_ICON.get(m.optString("type"));
        return t != null ? t : "cross";
    }

    static String accent(JSONObject m) { String c = m.optString("color"); return Pixel.validHex(c) ? c : Pixel.DEFAULT_COLOR; }

    /** "text" = just the name and a big timer, no animation. */
    static boolean textOnly(JSONObject m) { return "text".equals(m.optString("style")); }

    static String style(JSONObject m) {
        String s = m.optString("style");
        for (String x : Pixel.STYLES) if (x.equals(s)) return s;
        return "ring";
    }

    /** Background snapped to a colour RGB_565 can hold exactly, so the bitmap edge vanishes into the tinted panel. */
    static int[] bg(JSONObject m) {
        String c = m.optString("bg");
        int[] b = Pixel.hex(Pixel.validHex(c) ? c : Pixel.DEFAULT_BG);
        int r5 = b[0] >> 3, g6 = b[1] >> 2, b5 = b[2] >> 3;
        return new int[]{(r5 << 3) | (r5 >> 2), (g6 << 2) | (g6 >> 4), (b5 << 3) | (b5 >> 2)};
    }

    static String hex(int[] c) { return String.format("#%02x%02x%02x", c[0], c[1], c[2]); }

    static int argb(int[] c) { return 0xFF000000 | Pixel.pack(c); }

    /** Animation frame drawn at exactly sizePx (nearest-neighbour, so it stays crisp and fills its box with no gap). */
    static Bitmap frameExact(JSONObject m, float frac, int state, int k, int sizePx) {
        int[] px = Pixel.render(style(m), icon(m), accent(m), hex(bg(m)), frac, state, k);
        int G = Pixel.G, S = Math.max(G, Math.min(sizePx, G * 6));
        int[] big = new int[S * S];
        for (int y = 0; y < S; y++) { int sy = y * G / S; for (int x = 0; x < S; x++) big[y * S + x] = 0xFF000000 | px[sy * G + x * G / S]; }
        return Bitmap.createBitmap(big, S, S, Bitmap.Config.RGB_565);
    }

    /** Syringe-gauge frame drawn at exactly w x h px (nearest neighbour). */
    static Bitmap gaugeFrame(JSONObject m, float frac, int state, int k, int w, int h) {
        int[] px = Pixel.gauge(accent(m), hex(bg(m)), frac, state, k);
        w = Math.max(Pixel.GW, Math.min(w, Pixel.GW * 12)); h = Math.max(Pixel.GH, Math.min(h, Pixel.GH * 12));
        int[] big = new int[w * h];
        for (int y = 0; y < h; y++) { int sy = y * Pixel.GH / h; for (int x = 0; x < w; x++) big[y * w + x] = 0xFF000000 | px[sy * Pixel.GW + x * Pixel.GW / w]; }
        return Bitmap.createBitmap(big, w, h, Bitmap.Config.RGB_565);
    }

    /** One animation frame (0..3) of the med's chosen style, scaled up with no smoothing. Opaque RGB_565 keeps widget updates small. */
    static Bitmap frame(JSONObject m, float frac, int state, int k, int scale) {
        int[] px = Pixel.render(style(m), icon(m), accent(m), hex(bg(m)), frac, state, k);
        int G = Pixel.G, S = G * scale;
        int[] big = new int[S * S];
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) big[y * S + x] = 0xFF000000 | px[(y / scale) * G + x / scale];
        return Bitmap.createBitmap(big, S, S, Bitmap.Config.RGB_565);
    }
}
