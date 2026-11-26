package com.bitmeds.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WidgetTest {

    private static JSONObject testo(long nextAt, long periodStart) throws Exception {
        return new JSONObject().put("id", "t1").put("type", "injection").put("name", "Testosterone")
                .put("amount", 0.5).put("unit", "ml").put("strength", "200 mg/ml")
                .put("sched", "days").put("days", new JSONArray().put(1).put(4)).put("time", "20:00")
                .put("sites", new JSONArray().put("Left glute").put("Right glute").put("Left thigh"))
                .put("siteIdx", 1).put("icon", "syringe").put("color", "#ff9f1c").put("bg", "#101428").put("style", "ring").put("nextAt", nextAt).put("periodStart", periodStart).put("log", new JSONArray());
    }

    private static long at(int year, int month, int day, int h, int m) {
        Calendar c = Calendar.getInstance(); c.clear(); c.set(year, month, day, h, m); return c.getTimeInMillis();
    }

    @Test public void supplyHistoryAndSkip() throws Exception {
        long mon8pm = at(2026, Calendar.NOVEMBER, 30, 20, 0);
        JSONObject m = testo(mon8pm, mon8pm - 4 * Sched.DAY).put("supply", new JSONObject().put("left", 2.0).put("size", 10));
        Sched.give(m, mon8pm + 10 * Sched.MIN);
        assertEquals(1.5, m.getJSONObject("supply").getDouble("left"), 1e-9);
        assertEquals(1, Supply.streak(m));
        Supply s = Supply.of(m);
        assertEquals(3, s.doses);
        assertTrue(s.low);                       // 3 doses ≈ 10 days < 14
        long beforeSkip = m.getLong("nextAt");
        Sched.skip(m, beforeSkip - Sched.HOUR);
        assertTrue(m.getLong("nextAt") > beforeSkip);
        assertEquals(1.5, m.getJSONObject("supply").getDouble("left"), 1e-9);   // skipping uses no supply
        assertEquals(1, Supply.streak(m));       // a deliberate skip doesn't break the streak
        Sched.give(m, m.getLong("nextAt") + 9 * Sched.HOUR);                    // 9h late
        assertEquals(0, Supply.streak(m));
    }

    @Test public void twiceWeeklyScheduleAndSiteRotation() throws Exception {
        long mon8pm = at(2026, Calendar.NOVEMBER, 30, 20, 0), thu8pm = at(2026, Calendar.DECEMBER, 3, 20, 0);
        JSONObject m = testo(mon8pm, mon8pm - 4 * Sched.DAY);
        // given 1h early on Monday → counts for Monday, next is Thursday
        Sched.give(m, mon8pm - Sched.HOUR);
        assertEquals(thu8pm, m.getLong("nextAt"));
        assertEquals("Right glute", m.getString("lastSite"));
        assertEquals("Left thigh", Sched.nextSite(m));
        // given a day late (Friday) → next is the following Monday
        Sched.give(m, thu8pm + Sched.DAY);
        assertEquals(at(2026, Calendar.DECEMBER, 7, 20, 0), m.getLong("nextAt"));
        assertEquals("Left glute", Sched.nextSite(m));
        assertEquals(2, m.getJSONArray("log").length());
    }

    @Test public void everyStyleRendersAllFrames() {
        for (String style : Pixel.STYLES) for (int state = 0; state < 3; state++) for (int k = 0; k < 4; k++) {
            int[] px = Pixel.render(style, "syringe", "#34d399", "#101428", 0.5, state, k);
            assertEquals(Pixel.G * Pixel.G, px.length);
            java.util.Set<Integer> colours = new java.util.HashSet<>();
            for (int p : px) colours.add(p);
            assertTrue(style + " draws something", colours.size() > 4);
        }
    }

    private static JSONObject look(JSONObject m, String id, String name, String icon, String color, String bg, String style) throws Exception {
        return m.put("id", id).put("name", name).put("icon", icon).put("color", color).put("bg", bg).put("style", style);
    }

    /** Renders the widgets at many sizes and states (they're drawn to the real size, so every size must be filled). */
    @Test public void rendersWidgets() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        long now = System.currentTimeMillis();
        JSONObject testo = look(testo(now + Sched.DAY + 19 * Sched.HOUR + 4 * Sched.MIN, now - 40 * Sched.HOUR), "t", "Testosterone", "syringe", "#facc15", "#101428", "tank").put("amount", 0.5).put("strength", "");
        testo.put("supply", new JSONObject().put("left", 6.5).put("size", 10).put("lowDays", 14));
        JSONArray h = new JSONArray();
        for (int i = 0; i < 12; i++) h.put(new JSONObject().put("t", now - (i + 1) * 3 * Sched.DAY).put("due", now - (i + 1) * 3 * Sched.DAY + Sched.HOUR));
        testo.put("hist", h);
        JSONObject late = look(testo(now - 7 * Sched.MIN, now - 84 * Sched.HOUR), "t2", "Testosterone", "syringe", "#ff9f1c", "#101428", "ring");
        late.put("supply", new JSONObject().put("left", 1.0).put("size", 10).put("lowDays", 14));
        JSONObject text = look(testo(now + 5 * Sched.HOUR, now - 19 * Sched.HOUR), "v", "Vitamin D", "capsule", "#f472b6", "#1e0f2e", "text").put("sites", new JSONArray());
        JSONObject heart = look(testo(now + 20 * Sched.MIN, now - 7 * Sched.HOUR), "a", "Amoxicillin", "bottle", "#a3e635", "#0c1f17", "heart").put("sites", new JSONArray());
        JSONObject pen = look(testo(now + 3 * Sched.DAY, now - 3 * Sched.DAY), "i", "Insulin long-acting pen injection", "pen", "#00e5ff", "#0a1a2f", "hop");
        int n = 0;
        for (JSONObject m : new JSONObject[]{testo, late, text, heart}) {
            for (float[] sz : new float[][]{{330, 160}, {330, 250}, {160, 160}}) {
                RemoteViews rv = MedWidget.build(c, 5, m, 2, sz[0], sz[1], now);
                render(c, rv, sz[0], sz[1], "single_" + (n++));
            }
        }
        List<JSONObject> many = MedListWidget.sorted(new JSONArray().put(testo).put(text).put(heart).put(pen).put(late));
        assertEquals("t2", many.get(0).getString("id"));               // overdue first
        render(c, MedListWidget.build(c, 9, many, 330, 160, now), 330, 160, "list_0");
        render(c, MedListWidget.build(c, 9, many, 330, 290, now), 330, 290, "list_1");
        render(c, MedListWidget.build(c, 9, many.subList(0, 2), 330, 250, now), 330, 250, "list_2");
        render(c, MedListWidget.build(c, 9, many.subList(1, 2), 330, 160, now), 330, 160, "list_3");  // one med → full look
        assertEquals("1D 19:04", WidgetArt.countdown(Sched.DAY + 19 * Sched.HOUR + 4 * Sched.MIN));
        assertEquals("0:07", WidgetArt.countdown(7 * Sched.MIN));
    }

    private static void render(Context c, RemoteViews rv, float wDp, float hDp, String name) throws Exception {
        View v = rv.apply(c, new FrameLayout(c));   // throws if an action targets a missing view / unsupported method
        float d = c.getResources().getDisplayMetrics().density;
        int w = Math.round(wDp * d), h = Math.round(hDp * d);
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out); cv.drawColor(0xFF000000); v.draw(cv);
        File f = new File(System.getProperty("widgetOut", "build"), name + ".png");
        f.getParentFile().mkdirs();
        try (FileOutputStream o = new FileOutputStream(f)) { out.compress(Bitmap.CompressFormat.PNG, 100, o); }
        assertTrue(f.length() > 0);
    }
}
