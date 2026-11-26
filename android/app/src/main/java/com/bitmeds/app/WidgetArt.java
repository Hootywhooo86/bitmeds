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

        int pad = dp(d, 8);
        Rect in = new Rect(panel.left + pad, panel.top + pad, panel.right - pad, panel.bottom - pad);
        int btnH = clamp(Math.round(in.height() * 0.27f), dp(d, 36), dp(d, 64));
        Rect btn = new Rect(in.left, in.bottom - btnH, in.right, in.bottom);
        int gap = dp(d, 6);
        int barH = clamp(Math.round(in.height() * 0.05f), dp(d, 6), dp(d, 10));
        Rect bar = new Rect(in.left, btn.top - gap - barH, in.right, btn.top - gap);
        Rect body = new Rect(in.left, in.top, in.right, bar.top - gap);
        String btnLabel = armed ? "TAP AGAIN TO LOG" : body.width() < dp(d, 230) ? "GAVE IT" : "GAVE IT · RESET";

        if (body.width() < dp(d, 230)) {
            // square / narrow: big animation on top, name + timer under it, all centred
            int ch = body.height();
            Rect rTime = new Rect(body.left, body.bottom - Math.round(ch * 0.22f), body.right, body.bottom);
            Rect rName = new Rect(body.left, rTime.top - Math.round(ch * 0.14f), body.right, rTime.top);
            if (!L.textOnly) {
                int side = Math.min(rName.top - body.top - dp(d, 2), body.width());
                Rect box = new Rect(body.centerX() - side / 2, body.top, body.centerX() - side / 2 + side, body.top + side);
                g.art = artBox(cv, box, L, d);
            } else {
                g.art = null;
                int y = body.top;
                Rect rLabel = new Rect(body.left, y, body.right, y + Math.round(ch * 0.12f));
                text(cv, c, L.label, rLabel, L.accentLight, 10, false, false, true);
                rName = new Rect(body.left, rLabel.bottom + Math.round(ch * 0.04f), body.right, rLabel.bottom + Math.round(ch * 0.26f));
                rTime = new Rect(body.left, rName.bottom + Math.round(ch * 0.03f), body.right, body.bottom - Math.round(ch * 0.22f));
                chips(cv, c, new Rect(body.left, body.bottom - Math.round(ch * 0.19f), body.right, body.bottom), L, d, true);
            }
            text(cv, c, L.name, rName, INK, 22, true, false, true);
            text(cv, c, L.time, rTime, L.tone, 72, true, true, true);
        } else {
            Rect col = new Rect(body);
            if (!L.textOnly) {
                // the animation gets the full height of the body: no gaps above or below it
                int side = Math.min(body.height(), Math.round(body.width() * 0.44f));
                Rect box = new Rect(body.left, body.top, body.left + side, body.top + side);
                g.art = artBox(cv, box, L, d);
                col = new Rect(box.right + dp(d, 9), body.top, body.right, body.bottom);
            } else g.art = null;
            int ch = col.height();
            int y = col.top;
            Rect rLabel = new Rect(col.left, y, col.right, y += Math.round(ch * 0.12f));
            Rect rName = new Rect(col.left, y += Math.round(ch * 0.02f), col.right, y += Math.round(ch * 0.23f));
            Rect rTime = new Rect(col.left, y += Math.round(ch * 0.02f), col.right, y += Math.round(ch * 0.33f));
            Rect rInfo = new Rect(col.left, y += Math.round(ch * 0.02f), col.right, y += Math.round(ch * 0.11f));
            Rect rChips = new Rect(col.left, y + Math.round(ch * 0.03f), col.right, col.bottom);
            text(cv, c, L.label, rLabel, L.accentLight, 10, false, false);
            text(cv, c, L.name, rName, INK, 24, true, false);
            text(cv, c, L.time, rTime, L.tone, 72, true, true);
            text(cv, c, L.info, rInfo, L.muted, 10, false, false);
            chips(cv, c, rChips, L, d, false);
        }
        bar(cv, bar, L, d);
        button(cv, c, btn, L, btnLabel, d);
        g.give = btn;
        return bmp;
    }

    /** Framed box for the animation; returns the exact area the animation frames fill (no inner gap). */
    private static Rect artBox(Canvas cv, Rect box, Look L, float d) {
        fill(cv, box, L.boxLine);
        int f = dp(d, 3);
        Rect hi = new Rect(box.left, box.top, box.right, box.top + Math.max(1, dp(d, 1)));
        fill(cv, hi, Sprites.argb(L.acc[1]));
        Rect art = inset(box, f);
        fill(cv, art, Sprites.argb(L.bgSnap));
        return art;
    }

    /** Little pixel pills that turn spare space into useful info: supply left, streak, next site / schedule. */
    private static void chips(Canvas cv, Context c, Rect r, Look L, float d, boolean center) {
        if (r.height() < dp(d, 10) || L.chips.isEmpty()) return;
        int gap = dp(d, 5), n = Math.min(L.chips.size(), r.width() < dp(d, 170) ? 2 : 3);
        int w = (r.width() - gap * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            String[] chip = L.chips.get(i);
            Rect b = new Rect(r.left + i * (w + gap), r.top, r.left + i * (w + gap) + w, r.bottom);
            int col = chip[1].equals("warn") ? RED : chip[1].equals("hot") ? 0xFFFF9F1C : Sprites.argb(L.acc[0]);
            fill(cv, b, Sprites.argb(Pixel.mix(Pixel.unpack(col & 0xFFFFFF), new int[]{0, 0, 0}, 0.55)));
            fill(cv, new Rect(b.left, b.top, b.right, b.top + Math.max(1, dp(d, 2))), col);
            fill(cv, new Rect(b.left, b.top, b.left + Math.max(1, dp(d, 2)), b.bottom), col);
            text(cv, c, chip[0], inset(b, dp(d, 4)), INK, 12, true, false, true);
        }
    }

    // ------------------------------------------------------------------ layout: SYRINGE GAUGE

    /** Big syringe on the left filling with the med as dose time nears; timer, doses left and button on the right. */
    static Bitmap gaugeLayout(Context c, JSONObject m, float wDp, float hDp, boolean armed, long now, Geo g) {
        float d = c.getResources().getDisplayMetrics().density;
        int W = Math.max(40, Math.round(wDp * d)), H = Math.max(40, Math.round(hDp * d));
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565);
        Canvas cv = new Canvas(bmp);
        Look L = new Look(c, m, now, armed);
        g.d = d;
        int sh = dp(d, 4);
        Rect panel = new Rect(0, 0, W - sh, H - sh);
        cv.drawColor(0xFF000000);
        drawPanel(cv, new Rect(sh, sh, W, H), BLACK, BLACK, d, 0);
        drawPanel(cv, panel, Sprites.argb(L.bg), L.border, d, m.optString("id").hashCode());
        int pad = dp(d, 8);
        Rect in = new Rect(panel.left + pad, panel.top + pad, panel.right - pad, panel.bottom - pad);
        // syringe keeps the 24:60 shape of its pixel grid and takes the full height
        int gh = in.height(), gw = Math.round(gh * Pixel.GW / (float) Pixel.GH);
        if (gw > in.width() * 0.3f) { gw = Math.round(in.width() * 0.3f); gh = Math.round(gw * Pixel.GH / (float) Pixel.GW); }
        Rect syr = new Rect(in.left, in.centerY() - gh / 2, in.left + gw, in.centerY() - gh / 2 + gh);
        fill(cv, syr, Sprites.argb(L.bgSnap));
        g.art = syr;
        Rect col = new Rect(syr.right + dp(d, 10), in.top, in.right, in.bottom);
        int btnH = clamp(Math.round(col.height() * 0.26f), dp(d, 34), dp(d, 60));
        Rect btn = new Rect(col.left, col.bottom - btnH, col.right, col.bottom);
        Rect body = new Rect(col.left, col.top, col.right, btn.top - dp(d, 6));
        int ch = body.height(), y = body.top;
        Rect rLabel = new Rect(body.left, y, body.right, y += Math.round(ch * 0.13f));
        Rect rName = new Rect(body.left, y += Math.round(ch * 0.02f), body.right, y += Math.round(ch * 0.22f));
        Rect rTime = new Rect(body.left, y += Math.round(ch * 0.02f), body.right, y += Math.round(ch * 0.33f));
        Rect rDoses = new Rect(body.left, y + Math.round(ch * 0.04f), body.right, body.bottom);
        text(cv, c, L.label, rLabel, L.accentLight, 10, false, false);
        text(cv, c, L.name, rName, INK, 24, true, false);
        text(cv, c, L.time, rTime, L.tone, 72, true, true);
        doses(cv, c, rDoses, L, m, d);
        button(cv, c, btn, L, armed ? "TAP AGAIN" : col.width() < dp(d, 200) ? "GAVE IT" : "GAVE IT · RESET", d);
        g.give = btn;
        return bmp;
    }

    /** "19 DOSES LEFT" counter (big number + label), or the schedule when no supply is tracked. */
    private static void doses(Canvas cv, Context c, Rect r, Look L, JSONObject m, float d) {
        if (r.height() < dp(d, 10)) return;
        Supply sp = Supply.of(m);
        if (sp == null) { text(cv, c, L.info, r, L.muted, 12, false, false); return; }
        int col = sp.low ? RED : Sprites.argb(L.acc[0]);
        fill(cv, r, Sprites.argb(Pixel.mix(L.bg, new int[]{0, 0, 0}, 0.35)));
        fill(cv, new Rect(r.left, r.top, r.left + Math.max(2, dp(d, 3)), r.bottom), col);
        Rect in = new Rect(r.left + dp(d, 8), r.top + dp(d, 3), r.right - dp(d, 6), r.bottom - dp(d, 3));
        int nw = Math.round(in.width() * 0.38f);
        int used = text(cv, c, String.valueOf(sp.doses), new Rect(in.left, in.top, in.left + nw, in.bottom), col, 40, true, true);
        Paint p = new Paint(); p.setTypeface(font(c)); p.setTextSize(used);
        int x = in.left + Math.round(p.measureText(String.valueOf(sp.doses))) + dp(d, 6);
        Rect lab = new Rect(x, in.top, in.right, in.bottom);
        text(cv, c, (sp.doses == 1 ? "DOSE LEFT" : "DOSES LEFT") + (sp.low ? " · REFILL" : ""), new Rect(lab.left, lab.top, lab.right, lab.top + lab.height() / 2), sp.low ? RED : L.accentLight, 10, false, false);
        text(cv, c, "~" + Math.round(sp.daysLeft) + " DAYS", new Rect(lab.left, lab.top + lab.height() / 2, lab.right, lab.bottom), L.muted, 10, false, false);
    }

    // ------------------------------------------------------------------ layout: OVERLAY

    /** The animation fills the whole widget and the text sits on top of it. Returns 2 full frames (k = 0, 1). */
    static Bitmap[] overlay(Context c, JSONObject m, float wDp, float hDp, boolean armed, long now, Geo g) {
        float d = c.getResources().getDisplayMetrics().density;
        int W = Math.max(40, Math.round(wDp * d)), H = Math.max(40, Math.round(hDp * d));
        Look L = new Look(c, m, now, armed);
        g.d = d;
        int state = MedListWidget.stateOf(m, now), sh = dp(d, 4);
        Rect panel = new Rect(0, 0, W - sh, H - sh);
        int pad = dp(d, 10);
        Rect in = new Rect(panel.left + pad, panel.top + pad, panel.right - pad, panel.bottom - pad);
        int btnH = clamp(Math.round(in.height() * 0.26f), dp(d, 34), dp(d, 60));
        Rect btn = new Rect(in.left, in.bottom - btnH, in.right, in.bottom);
        Bitmap[] out = new Bitmap[2];
        for (int k = 0; k < 2; k++) {
            Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565);
            Canvas cv = new Canvas(bmp);
            cv.drawColor(0xFF000000);
            drawPanel(cv, new Rect(sh, sh, W, H), BLACK, BLACK, d, 0);
            // the art, scaled (nearest neighbour) to cover the panel
            int[] px = Pixel.render(Sprites.style(m), Sprites.icon(m), Sprites.accent(m), Sprites.hex(L.bgSnap), L.frac, state, k);
            // dim the picture toward the background so the text on top reads clearly
            int[] bgc = L.bgSnap;
            for (int i = 0; i < px.length; i++) {
                int r = px[i] >> 16 & 255, gg = px[i] >> 8 & 255, b = px[i] & 255;
                px[i] = (Math.round(r * 0.5f + bgc[0] * 0.5f) << 16) | (Math.round(gg * 0.5f + bgc[1] * 0.5f) << 8) | Math.round(b * 0.5f + bgc[2] * 0.5f);
            }
            int D = Math.max(panel.width(), panel.height()), ox = panel.left + (panel.width() - D) / 2, oy = panel.top + (panel.height() - D) / 2;
            int[] row = new int[panel.width()];
            for (int yy = panel.top; yy < panel.bottom; yy++) {
                int sy = (yy - oy) * Pixel.G / D;
                for (int xx = panel.left; xx < panel.right; xx++) row[xx - panel.left] = 0xFF000000 | px[sy * Pixel.G + (xx - ox) * Pixel.G / D];
                bmp.setPixels(row, 0, panel.width(), panel.left, yy, panel.width(), 1);
            }
            // stepped dark bands top + bottom so the text reads on any picture
            Paint p = new Paint();
            int bands = 6;
            for (int i = 0; i < bands; i++) {
                p.setColor(Math.round(120 * (1 - i / (float) bands)) << 24);
                int bh = Math.round(panel.height() * 0.42f / bands);
                cv.drawRect(panel.left, panel.top + i * bh, panel.right, panel.top + (i + 1) * bh, p);
                cv.drawRect(panel.left, panel.bottom - (i + 1) * bh - btnH, panel.right, panel.bottom - i * bh - btnH, p);
            }
            p.setColor(0xCC000000);
            cv.drawRect(panel.left, btn.top - dp(d, 6), panel.right, panel.bottom, p);
            // colour border on top
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(d, 3)); p.setColor(L.border); p.setAntiAlias(false);
            float rad = dp(d, 12), hw = dp(d, 3) / 2f;
            cv.drawRoundRect(new RectF(panel.left + hw, panel.top + hw, panel.right - hw, panel.bottom - hw), rad, rad, p);
            int ch = btn.top - in.top - dp(d, 6), y = in.top;
            Rect rLabel = new Rect(in.left, y, in.right, y += Math.round(ch * 0.13f));
            Rect rName = new Rect(in.left, y += Math.round(ch * 0.02f), in.right, y += Math.round(ch * 0.24f));
            Rect rTime = new Rect(in.left, y += Math.round(ch * 0.04f), in.right, y += Math.round(ch * 0.38f));
            Rect rChips = new Rect(in.left, y + Math.round(ch * 0.04f), in.right, btn.top - dp(d, 8));
            text(cv, c, L.label, rLabel, L.accentLight, 10, true, false);
            text(cv, c, L.name, rName, INK, 26, true, false);
            text(cv, c, L.time, rTime, L.tone, 80, true, true);
            chips(cv, c, rChips, L, d, false);
            button(cv, c, btn, L, armed ? "TAP AGAIN TO LOG" : in.width() < dp(d, 200) ? "GAVE IT" : "GAVE IT · RESET", d);
            out[k] = bmp;
        }
        g.give = btn;
        g.art = panel;
        return out;
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
                g.rowArt[i] = art;
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
        java.util.List<String[]> chips = new java.util.ArrayList<>();

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
            // chips: supply left, on-time streak, next site / schedule
            Supply sup = Supply.of(m);
            if (sup != null) chips.add(new String[]{sup.doses + (sup.doses == 1 ? " DOSE LEFT" : " DOSES LEFT"), sup.low ? "warn" : "ok"});
            int streak = Supply.streak(m);
            if (streak > 0) chips.add(new String[]{"STREAK " + streak, "hot"});
            if (!site.isEmpty()) chips.add(new String[]{"SITE " + MedWidget.shortSite(site), "ok"});
            if (chips.size() < 2) chips.add(new String[]{Supply.schedLabel(m), "ok"});
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

    /** Chunky 3D pixel button: dark lip, bevel, shine stripe, pixel check icon, drop-shadowed label. */
    private static void button(Canvas cv, Context c, Rect r, Look L, String label, float d) {
        boolean confirm = label.startsWith("TAP") || label.startsWith("AGAIN");
        int[] face = confirm ? new int[]{250, 204, 21} : L.due ? new int[]{255, 93, 93} : L.acc[0];
        int[][] sh = Pixel.shades(face);
        int u = Math.max(1, dp(d, 1)), lip = Math.max(3, dp(d, 5));
        fill(cv, r, 0xFF000000);                                                    // outline
        Rect body = new Rect(r.left + u, r.top + u, r.right - u, r.bottom - u);
        fill(cv, body, Sprites.argb(sh[2]));                                         // lip
        Rect top = new Rect(body.left, body.top, body.right, body.bottom - lip);
        fill(cv, top, Sprites.argb(face));
        fill(cv, new Rect(top.left, top.top, top.right, top.top + 2 * u), Sprites.argb(sh[1]));     // bevel light
        fill(cv, new Rect(top.left, top.top, top.left + 2 * u, top.bottom), Sprites.argb(sh[1]));
        fill(cv, new Rect(top.left, top.bottom - u, top.right, top.bottom), Sprites.argb(Pixel.mix(face, new int[]{0, 0, 0}, 0.25)));
        // shine: two little diagonal pixel blocks near the left
        int sx = top.left + 6 * u, sy = top.top + 4 * u, b = 2 * u;
        fill(cv, new Rect(sx, sy, sx + b, sy + b), 0xFFFFFFFF);
        fill(cv, new Rect(sx + b, sy - b + b, sx + 2 * b, sy + b), 0xCCFFFFFF);
        int ink = MedWidget.lum(face) > 0.55 ? 0xFF0B0B16 : 0xFFFFFFFF;
        Rect t = new Rect(top.left + dp(d, 8), top.top + dp(d, 3), top.right - dp(d, 8), top.bottom - dp(d, 3));
        // pixel check mark icon + label, centred together
        Paint p = new Paint(); p.setAntiAlias(false); p.setTypeface(font(c));
        int size = fitSize(p, label, t.width() - t.height(), Math.min(t.height(), Math.round(16 * d)));
        p.setTextSize(size);
        float tw = p.measureText(label), icon = size * 1.2f, total = icon + size * 0.6f + tw;
        float x = t.left + (t.width() - total) / 2f;
        int q = Math.max(1, size / 8);
        float iy = t.centerY() - 4 * q;
        int[][] CHECK = {{6,0},{7,0},{5,1},{6,1},{4,2},{5,2},{0,3},{1,3},{3,3},{4,3},{1,4},{2,4},{3,4},{2,5}};
        Paint ip = new Paint(); ip.setColor(0xFF000000);
        for (int[] px : CHECK) cv.drawRect(x + px[0] * q + q, iy + px[1] * q + q, x + px[0] * q + 2 * q, iy + px[1] * q + 2 * q, ip);
        ip.setColor(ink);
        for (int[] px : CHECK) cv.drawRect(x + px[0] * q, iy + px[1] * q, x + px[0] * q + q, iy + px[1] * q + q, ip);
        Rect bb = new Rect(); p.getTextBounds("M", 0, 1, bb);
        float ty = Math.round(t.top + (t.height() + bb.height()) / 2f), tx = x + icon + size * 0.6f;
        p.setColor(ink == 0xFFFFFFFF ? 0xFF000000 : Sprites.argb(sh[1]));
        cv.drawText(label, tx + q, ty + q, p);
        p.setColor(ink);
        cv.drawText(label, tx, ty, p);
    }

    private static int fitSize(Paint p, String s, int maxW, int maxH) {
        for (int sz = (maxH / 8) * 8; sz >= 8; sz -= 8) { p.setTextSize(sz); if (p.measureText(s) <= maxW) return sz; }
        return 8;
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
