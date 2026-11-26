package com.bitmeds.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Draws the whole widget as one pixel-art picture, sized to the widget's real size, so it looks the same
 * on every launcher (launchers ignore custom fonts in normal widget text). The live animation sits on top
 * in a ViewFlipper at {@link Geo#art}; the GAVE IT tap area sits at {@link Geo#give}.
 *
 * Text uses Press Start 2P at multiples of 8px with anti-aliasing off, so every glyph stays on the pixel grid.
 */
final class WidgetArt {
    /** Where the overlays go, in px of the returned bitmap. */
    static final class Geo {
        float d;                    // px per dp
        Rect art, give;             // single-med widget
        Rect[] rowArt, rowGive;     // list widget
    }

    private static Typeface font;
    private static final int RED = 0xFFFF5D5D, YELLOW = 0xFFFACC15, INK = 0xFFF4F1FF, BLACK = 0xFF000000;

    private static Typeface font(Context c) {
        if (font == null) {
            try { font = c.getResources().getFont(R.font.press_start); } catch (Exception e) { font = Typeface.MONOSPACE; }
        }
        return font;
    }

    // ------------------------------------------------------------------ single med

    static Bitmap single(Context c, JSONObject m, float wDp, float hDp, boolean armed, long now, Geo g) {
        float d = c.getResources().getDisplayMetrics().density;
        int W = Math.max(40, Math.round(wDp * d)), H = Math.max(40, Math.round(hDp * d));
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565);
        Canvas cv = new Canvas(bmp);
        Look L = new Look(c, m, now, armed);
        g.d = d;

        int sh = dp(d, 4);                                       // hard black drop shadow, like the app's cards
        Rect panel = new Rect(0, 0, W - sh, H - sh);
        cv.drawColor(0xFF000000);
        drawPanel(cv, new Rect(sh, sh, W, H), BLACK, BLACK, d, 0);
        drawPanel(cv, panel, Sprites.argb(L.bg), L.border, d, m.optString("id").hashCode());

        int pad = dp(d, 10);
        Rect in = new Rect(panel.left + pad, panel.top + pad, panel.right - pad, panel.bottom - pad);
        int btnH = clamp(Math.round(in.height() * 0.24f), dp(d, 30), dp(d, 48));
        Rect btn = new Rect(in.left, in.bottom - btnH, in.right, in.bottom);
        int barH = dp(d, 5), gap = dp(d, 7);
        Rect bar = new Rect(in.left, btn.top - gap - barH, in.right, btn.top - gap);
        Rect body = new Rect(in.left, in.top, in.right, bar.top - gap);

        boolean narrow = body.width() < dp(d, 230);
        if (narrow) {
            // square / narrow widget: stack the animation over a centred name + timer
            int ch = body.height();
            Rect rTime = new Rect(body.left, body.bottom - Math.round(ch * 0.21f), body.right, body.bottom);
            Rect rName = new Rect(body.left, rTime.top - Math.round(ch * 0.13f), body.right, rTime.top - Math.round(ch * 0.01f));
            if (!L.textOnly) {
                int side = Math.min(rName.top - body.top - dp(d, 4), body.width());
                Rect box = new Rect(body.centerX() - side / 2, body.top, body.centerX() - side / 2 + side, body.top + side);
                fill(cv, box, L.boxLine);
                Rect art = inset(box, dp(d, 3));
                fill(cv, art, Sprites.argb(L.bgSnap));
                g.art = snap52(art);
            } else {
                g.art = null;
                Rect rLabel = new Rect(body.left, body.top, body.right, body.top + Math.round(ch * 0.14f));
                text(cv, c, L.label, rLabel, L.accentLight, 9, false, false, true);
                rName = new Rect(body.left, rLabel.bottom + Math.round(ch * 0.06f), body.right, rLabel.bottom + Math.round(ch * 0.3f));
                rTime = new Rect(body.left, rName.bottom + Math.round(ch * 0.04f), body.right, body.bottom - Math.round(ch * 0.16f));
                text(cv, c, L.info, new Rect(body.left, body.bottom - Math.round(ch * 0.13f), body.right, body.bottom), L.muted, 9, false, false, true);
            }
            text(cv, c, L.name, rName, INK, 20, true, false, true);
            text(cv, c, L.time, rTime, L.tone, 64, true, true, true);
            bar(cv, bar, L, d);
            button(cv, c, btn, L, armed ? "AGAIN?" : "↺ GAVE IT", d);
            g.give = btn;
            return bmp;
        }

        Rect col = new Rect(body);
        if (!L.textOnly && body.width() > dp(d, 120)) {
            int side = Math.min(body.height(), Math.round(body.width() * 0.4f));
            Rect box = new Rect(body.left, body.top + (body.height() - side) / 2, body.left + side, body.top + (body.height() - side) / 2 + side);
            int frame = dp(d, 3);
            fill(cv, box, L.boxLine);
            Rect art = inset(box, frame);
            fill(cv, art, Sprites.argb(L.bgSnap));
            g.art = snap52(art);
            col = new Rect(box.right + dp(d, 10), body.top, body.right, body.bottom);
        } else g.art = null;

        // text column: label / name / timer / info, each auto-fitted to its share of the height
        int ch = col.height();
        Rect rLabel = new Rect(col.left, col.top, col.right, col.top + Math.round(ch * 0.13f));
        Rect rName = new Rect(col.left, rLabel.bottom + Math.round(ch * 0.04f), col.right, rLabel.bottom + Math.round(ch * 0.30f));
        Rect rTime = new Rect(col.left, rName.bottom + Math.round(ch * 0.05f), col.right, col.bottom - Math.round(ch * 0.19f));
        Rect rInfo = new Rect(col.left, col.bottom - Math.round(ch * 0.14f), col.right, col.bottom);

        text(cv, c, L.label, rLabel, L.accentLight, 9, false, false);
        text(cv, c, L.name, rName, INK, 20, true, false);
        text(cv, c, L.time, rTime, L.tone, 64, true, true);
        text(cv, c, L.info, rInfo, L.muted, 9, false, false);

        bar(cv, bar, L, d);
        button(cv, c, btn, L, armed ? "TAP AGAIN TO LOG" : "↺ GAVE IT · RESET", d);
        g.give = btn;
        return bmp;
    }

    // ------------------------------------------------------------------ several meds

    static Bitmap list(Context c, List<JSONObject> meds, int rows, float wDp, float hDp, int wid, long now, Geo g) {
        float d = c.getResources().getDisplayMetrics().density;
        int W = Math.max(40, Math.round(wDp * d)), H = Math.max(40, Math.round(hDp * d));
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565);
        Canvas cv = new Canvas(bmp);
        g.d = d;
        int shown = Math.min(rows, meds.size());
        g.rowArt = new Rect[shown];
        g.rowGive = new Rect[shown];

        int due = 0;
        for (JSONObject m : meds) if (m.optLong("nextAt") <= now) due++;
        int sh = dp(d, 4);
        Rect panel = new Rect(0, 0, W - sh, H - sh);
        cv.drawColor(0xFF000000);
        drawPanel(cv, new Rect(sh, sh, W, H), BLACK, BLACK, d, 0);
        drawPanel(cv, panel, 0xFF0B0B16, due > 0 ? RED : 0xFF34D399, d, 7);

        int pad = dp(d, 8);
        Rect in = new Rect(panel.left + pad, panel.top + pad, panel.right - pad, panel.bottom - pad);
        // header: bitmeds + status, like the RAILTIME header
        Rect head = new Rect(in.left, in.top, in.right, in.top + Math.max(dp(d, 12), Math.round(in.height() * 0.1f)));
        int extra = Math.max(0, meds.size() - shown);
        String status = meds.size() + (meds.size() == 1 ? " MED · " : " MEDS · ") + (due > 0 ? due + " DUE" : "ALL GOOD") + (extra > 0 ? " · +" + extra : "");
        int hs = text(cv, c, "bitmeds", new Rect(head.left, head.top, head.left + head.width() / 3, head.bottom), 0xFF34D399, 10, true, false);
        textRight(cv, c, status, new Rect(head.left + head.width() / 3, head.top, head.right, head.bottom), due > 0 ? RED : 0xFF9A97B8, hs);

        int gap = dp(d, 5);
        int top = head.bottom + gap;
        int rowH = shown > 0 ? (in.bottom - top - gap * (shown - 1)) / shown : 0;
        for (int i = 0; i < shown; i++) {
            JSONObject m = meds.get(i);
            boolean armed = now - Store.armedAt(c, wid, m.optString("id")) <= GiveReceiver.CONFIRM_MS;
            Look L = new Look(c, m, now, armed);
            Rect row = new Rect(in.left, top + i * (rowH + gap), in.right, top + i * (rowH + gap) + rowH);
            fill(cv, row, L.border);
            Rect rin = inset(row, dp(d, 2));
            fill(cv, rin, Sprites.argb(L.bg));
            stars(cv, rin, L, d, m.optString("id").hashCode());
            int p = dp(d, 5);
            Rect content = inset(rin, p);
            int btnW = clamp(Math.round(content.width() * 0.24f), dp(d, 48), dp(d, 96));
            int btnH = clamp(content.height(), dp(d, 24), dp(d, 44));
            Rect btn = new Rect(content.right - btnW, content.centerY() - btnH / 2, content.right, content.centerY() + btnH / 2);
            Rect txt = new Rect(content.left, content.top, btn.left - dp(d, 6), content.bottom);
            if (!L.textOnly) {
                int side = Math.min(content.height(), Math.round(content.width() * 0.22f));
                Rect art = new Rect(content.left, content.centerY() - side / 2, content.left + side, content.centerY() - side / 2 + side);
                fill(cv, art, Sprites.argb(L.bgSnap));
                g.rowArt[i] = snap52(art);
                txt.left = art.right + dp(d, 6);
            }
            int th = txt.height();
            text(cv, c, L.name, new Rect(txt.left, txt.top, txt.right, txt.top + Math.round(th * 0.38f)), INK, 16, true, false);
            text(cv, c, L.time, new Rect(txt.left, txt.top + Math.round(th * 0.44f), txt.right, txt.bottom), L.tone, 40, true, true);
            button(cv, c, btn, L, armed ? "AGAIN?" : "GAVE", d);
            g.rowGive[i] = btn;
        }
        if (meds.isEmpty()) text(cv, c, "NO MEDS YET", inset(in, dp(d, 20)), 0xFF9A97B8, 16, true, false);
        return bmp;
    }

    // ------------------------------------------------------------------ the med's look + text

    private static final class Look {
        int[] bg, bgSnap; int[][] acc;
        int tone, border, boxLine, accentLight, muted;
        boolean due, soon, textOnly;
        float frac;
        String name, label, time, info;

        Look(Context c, JSONObject m, long now, boolean armed) {
            long next = m.optLong("nextAt"), left = next - now;
            due = left <= 0; soon = Sched.isSoon(m, left);
            textOnly = Sprites.textOnly(m);
            acc = Pixel.shades(Pixel.hex(Sprites.accent(m)));
            bgSnap = Sprites.bg(m);
            bg = bgSnap;
            tone = due ? RED : soon ? YELLOW : Sprites.argb(acc[0]);
            border = due ? RED : Sprites.argb(acc[0]);
            boxLine = due ? 0xFF991B1B : Sprites.argb(acc[2]);
            accentLight = due ? RED : Sprites.argb(acc[1]);
            muted = Sprites.argb(Pixel.mix(bg, new int[]{255, 255, 255}, 0.62));
            frac = Sched.frac(m, now);
            name = m.optString("name");
            label = (due ? "⚠ DUE NOW" : "NEXT DOSE IN") + " · " + Sched.doseLine(m).toUpperCase(Locale.getDefault());
            time = (due ? "LATE " : "") + countdown(Math.abs(left));
            String site = Sched.nextSite(m);
            info = "";
            if (m.has("nextAt")) info = (due ? "WAS DUE " : "DUE ") + Sched.when(c, next).toUpperCase(Locale.getDefault());
            if (!site.isEmpty()) info += " · " + MedWidget.shortSite(site);
        }
    }

    /** Minute precision ("1D 19:04", "19:04", "0:42"); the widget redraws every minute. */
    static String countdown(long ms) {
        long mins = (ms + 59_999) / 60_000, days = mins / 1440, h = (mins % 1440) / 60, mi = mins % 60;
        String hm = h + ":" + (mi < 10 ? "0" : "") + mi;
        return days > 0 ? days + "D " + hm : hm;
    }

    // ------------------------------------------------------------------ drawing helpers

    private static void drawPanel(Canvas cv, Rect r, int fill, int border, float d, int seed) {
        Paint p = new Paint();
        p.setAntiAlias(false);
        float rad = dp(d, 12);
        p.setColor(border);
        cv.drawRoundRect(new RectF(r), rad, rad, p);
        if (fill == BLACK && border == BLACK) return;
        int bw = dp(d, 2);
        RectF inner = new RectF(r.left + bw, r.top + bw, r.right - bw, r.bottom - bw);
        p.setColor(fill);
        cv.drawRoundRect(inner, rad - bw, rad - bw, p);
        // soft top glow band + starfield, like the pixel weather widget
        int[] f = Pixel.unpack(fill & 0xFFFFFF);
        p.setColor(Sprites.argb(Pixel.mix(f, Pixel.unpack(border & 0xFFFFFF), 0.10)));
        cv.drawRoundRect(new RectF(inner.left, inner.top, inner.right, inner.top + inner.height() * 0.35f), rad - bw, rad - bw, p);
        p.setColor(fill);
        cv.drawRect(inner.left, inner.top + inner.height() * 0.2f, inner.right, inner.top + inner.height() * 0.35f, p);
        Random rnd = new Random(seed);
        int n = Math.round(r.width() * r.height() / (900f * d * d));
        int px = Math.max(1, Math.round(d));
        for (int i = 0; i < n; i++) {
            float x = inner.left + rnd.nextFloat() * inner.width(), y = inner.top + rnd.nextFloat() * inner.height();
            p.setColor(Sprites.argb(Pixel.mix(f, Pixel.unpack(border & 0xFFFFFF), 0.25 + rnd.nextFloat() * 0.35)));
            int s = rnd.nextInt(5) == 0 ? px * 2 : px;
            cv.drawRect(x, y, x + s, y + s, p);
        }
    }

    private static void stars(Canvas cv, Rect r, Look L, float d, int seed) {
        Paint p = new Paint();
        Random rnd = new Random(seed);
        int n = Math.round(r.width() * r.height() / (700f * d * d));
        int px = Math.max(1, Math.round(d));
        for (int i = 0; i < n; i++) {
            float x = r.left + rnd.nextFloat() * r.width(), y = r.top + rnd.nextFloat() * r.height();
            p.setColor(Sprites.argb(Pixel.mix(L.bg, L.acc[0], 0.2 + rnd.nextFloat() * 0.3)));
            cv.drawRect(x, y, x + px, y + px, p);
        }
    }

    private static void bar(Canvas cv, Rect r, Look L, float d) {
        int n = 24, gap = Math.max(1, dp(d, 2));
        float w = (r.width() - gap * (n - 1)) / (float) n;
        int filled = Math.round(L.frac * n);
        Paint p = new Paint();
        for (int i = 0; i < n; i++) {
            float x = r.left + i * (w + gap);
            p.setColor(i < filled || L.due ? L.tone : Sprites.argb(Pixel.mix(L.bg, new int[]{255, 255, 255}, 0.1)));
            cv.drawRect(x, r.top, x + w, r.bottom, p);
            if (i < filled || L.due) { p.setColor(0x33FFFFFF); cv.drawRect(x, r.top, x + w, r.top + Math.max(1, r.height() / 3f), p); }
        }
    }

    private static void button(Canvas cv, Context c, Rect r, Look L, String label, float d) {
        int[] face = label.startsWith("TAP") || label.startsWith("AGAIN") ? new int[]{250, 204, 21} : L.due ? new int[]{255, 93, 93} : L.acc[0];
        Paint p = new Paint();
        int lip = Math.max(2, dp(d, 4));
        p.setColor(Sprites.argb(Pixel.shades(face)[2]));
        cv.drawRect(r, p);
        p.setColor(Sprites.argb(face));
        cv.drawRect(r.left, r.top, r.right, r.bottom - lip, p);
        p.setColor(Sprites.argb(Pixel.shades(face)[1]));
        cv.drawRect(r.left, r.top, r.right, r.top + Math.max(1, dp(d, 2)), p);
        int ink = MedWidget.lum(face) > 0.55 ? 0xFF0B0B16 : 0xFFFFFFFF;
        Rect t = new Rect(r.left + dp(d, 6), r.top + dp(d, 4), r.right - dp(d, 6), r.bottom - lip - dp(d, 4));
        text(cv, c, label, t, ink, 14, false, false, true);
    }

    private static int text(Canvas cv, Context c, String s, Rect r, int color, int maxDp, boolean shadow, boolean glow) {
        return text(cv, c, s, r, color, maxDp, shadow, glow, false);
    }

    /**
     * Draws {@code s} as large as fits {@code r}: the size is a multiple of 8px (Press Start's pixel grid),
     * no bigger than maxDp. Returns the px size used.
     */
    private static int text(Canvas cv, Context c, String s, Rect r, int color, int maxDp, boolean shadow, boolean glow, boolean center) {
        if (s == null || s.isEmpty() || r.width() <= 0 || r.height() <= 0) return 8;
        Paint p = new Paint();
        p.setAntiAlias(false);
        p.setTypeface(font(c));
        float d = c.getResources().getDisplayMetrics().density;
        int max = Math.max(8, Math.round(maxDp * d));
        int size = 8;
        for (int sz = (Math.min(r.height(), max) / 8) * 8; sz >= 8; sz -= 8) {
            p.setTextSize(sz);
            if (p.measureText(s) <= r.width()) { size = sz; break; }
        }
        p.setTextSize(size);
        String out = s;
        while (p.measureText(out) > r.width() && out.length() > 2) out = out.substring(0, out.length() - 2) + "…";
        Rect b = new Rect();
        p.getTextBounds("M", 0, 1, b);
        float x = center ? r.left + (r.width() - p.measureText(out)) / 2f : r.left;
        float y = r.top + (r.height() + b.height()) / 2f;
        y = Math.round(y);
        int u = Math.max(1, size / 8);
        if (glow) {
            Paint gp = new Paint(p);
            gp.setColor(color);
            gp.setAlpha(110);
            gp.setShadowLayer(size * 0.6f, 0, 0, color);
            cv.drawText(out, x, y, gp);
        }
        if (shadow) {
            p.setColor(0xFF000000);
            cv.drawText(out, x + u, y + u, p);
        }
        p.setColor(color);
        cv.drawText(out, x, y, p);
        return size;
    }

    private static void textRight(Canvas cv, Context c, String s, Rect r, int color, int maxPx) {
        Paint p = new Paint();
        p.setAntiAlias(false);
        p.setTypeface(font(c));
        int size = 8;
        for (int sz = (Math.min(r.height(), maxPx) / 8) * 8; sz >= 8; sz -= 8) { p.setTextSize(sz); if (p.measureText(s) <= r.width()) { size = sz; break; } }
        p.setTextSize(size);
        Rect b = new Rect();
        p.getTextBounds("M", 0, 1, b);
        p.setColor(color);
        cv.drawText(s, r.right - p.measureText(s), Math.round(r.top + (r.height() + b.height()) / 2f), p);
    }

    /** Largest centred square inside r whose side is a multiple of 52 (the sprite grid). */
    static Rect snap52(Rect r) {
        int side = Math.max(52, (Math.min(r.width(), r.height()) / 52) * 52);
        int x = r.left + (r.width() - side) / 2, y = r.top + (r.height() - side) / 2;
        return new Rect(x, y, x + side, y + side);
    }

    private static void fill(Canvas cv, Rect r, int color) { Paint p = new Paint(); p.setColor(color); cv.drawRect(r, p); }

    private static Rect inset(Rect r, int by) { return new Rect(r.left + by, r.top + by, r.right - by, r.bottom - by); }

    static int dp(float d, float v) { return Math.round(v * d); }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
