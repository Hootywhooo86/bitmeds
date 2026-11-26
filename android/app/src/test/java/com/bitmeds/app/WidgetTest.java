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

    @Test public void rendersListWidget() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        long now = System.currentTimeMillis();
        JSONArray meds = new JSONArray()
                .put(testo(now + 2 * Sched.DAY + 13 * Sched.HOUR, now - 30 * Sched.HOUR))
                .put(testo(now + 5 * Sched.HOUR, now - 60 * Sched.HOUR).put("id", "v").put("name", "Vitamin D").put("icon", "capsule").put("color", "#f472b6").put("bg", "#1e0f2e").put("style", "heart"))
                .put(testo(now - 7 * Sched.MIN, now - 9 * Sched.HOUR).put("id", "i").put("name", "Insulin long-acting pen injection").put("icon", "pen").put("color", "#00e5ff").put("bg", "#0a1a2f").put("style", "tank"))
                .put(testo(now + 20 * Sched.MIN, now - 7 * Sched.HOUR).put("id", "a").put("name", "Amoxicillin").put("icon", "bottle").put("color", "#a3e635").put("bg", "#0c1f17").put("style", "hourglass"))
                .put(testo(now + 3 * Sched.DAY, now - 3 * Sched.DAY).put("id", "z").put("name", "Fish oil").put("icon", "drop").put("color", "#60a5fa").put("style", "hop"));
        java.util.List<JSONObject> list = MedListWidget.sorted(meds);
        assertEquals("Insulin long-acting pen injection", list.get(0).getString("name")); // overdue first
        Bitmap[][] f = MedListWidget.frames(list, now);
        float d = c.getResources().getDisplayMetrics().density;
        int n = 0;
        for (int[] size : new int[][]{{320, 150, 2}, {320, 290, 4}, {200, 150, 2}}) {
            RemoteViews rv = MedListWidget.build(c, 9, list, f, size[2], now);
            shot(c, rv.apply(c, new FrameLayout(c)), (int) (size[0] * d), (int) (size[1] * d), "list_" + (n++));
        }
    }

    private static void shot(Context c, View v, int w, int h, String name) throws Exception {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out); cv.drawColor(0xFF2B3A4A); v.draw(cv);
        File f = new File(System.getProperty("widgetOut", "build"), name + ".png");
        f.getParentFile().mkdirs();
        try (FileOutputStream o = new FileOutputStream(f)) { out.compress(Bitmap.CompressFormat.PNG, 100, o); }
        assertTrue(f.length() > 0);
    }

    /** Renders every widget at several sizes (phones put widgets in all sorts of shapes) to check nothing leaves dead space. */
    @Test public void fillsEverySize() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        long now = System.currentTimeMillis();
        JSONObject anim = testo(now + 2 * Sched.DAY + 13 * Sched.HOUR, now - 30 * Sched.HOUR);
        JSONObject text = testo(now + 5 * Sched.HOUR, now - 60 * Sched.HOUR).put("id", "v").put("name", "Vitamin D").put("style", "text")
                .put("color", "#f472b6").put("bg", "#1e0f2e").put("sites", new JSONArray());
        anim.put("name", "Testosterone");
        float d = c.getResources().getDisplayMetrics().density;
        int n = 0;
        for (JSONObject m : new JSONObject[]{anim, text}) {
            Store.save(c, new JSONArray().put(m));
            Bitmap[] frames = null;
            if (!Sprites.textOnly(m)) { frames = new Bitmap[4]; for (int k = 0; k < 4; k++) frames[k] = Sprites.frame(m, Sched.frac(m, now), 0, k, 4); }
            for (int[] sz : new int[][]{{320, 150}, {320, 240}, {250, 110}}) {
                RemoteViews rv = MedWidget.build(c, 5, m, 1, frames, R.layout.widget_wide);
                shot(c, rv.apply(c, new FrameLayout(c)), (int) (sz[0] * d), (int) (sz[1] * d), "fill_wide_" + n + "_" + sz[0] + "x" + sz[1]);
            }
            for (int[] sz : new int[][]{{150, 150}, {120, 200}, {200, 150}}) {
                RemoteViews rv = MedWidget.build(c, 5, m, 1, frames, R.layout.widget_small);
                shot(c, rv.apply(c, new FrameLayout(c)), (int) (sz[0] * d), (int) (sz[1] * d), "fill_small_" + n + "_" + sz[0] + "x" + sz[1]);
            }
            n++;
        }
        // list: 1 med in a tall widget, 2 meds tall, mixed animated + text-only
        java.util.List<JSONObject> one = java.util.Collections.singletonList(anim);
        java.util.List<JSONObject> two = java.util.Arrays.asList(anim, text);
        Bitmap[] big = new Bitmap[4]; for (int k = 0; k < 4; k++) big[k] = Sprites.frame(anim, Sched.frac(anim, now), 0, k, 4);
        shot(c, MedWidget.build(c, 9, anim, 1, big, R.layout.widget_wide).apply(c, new FrameLayout(c)), (int) (320 * d), (int) (240 * d), "fill_list_1"); // list with 1 med uses this
        shot(c, MedListWidget.build(c, 9, two, MedListWidget.frames(two, now), 4, now).apply(c, new FrameLayout(c)), (int) (320 * d), (int) (240 * d), "fill_list_2");
        shot(c, MedListWidget.build(c, 9, two, MedListWidget.frames(two, now), 2, now).apply(c, new FrameLayout(c)), (int) (320 * d), (int) (150 * d), "fill_list_3");
    }

    @Test public void rendersWidgetSizes() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        long now = System.currentTimeMillis();
        JSONObject ok = testo(now + 2 * Sched.DAY + 13 * Sched.HOUR, now - 30 * Sched.HOUR);
        JSONObject due = testo(now - 12 * Sched.MIN, now - 4 * Sched.DAY);
        JSONObject pink = testo(now + 5 * Sched.HOUR, now - 60 * Sched.HOUR).put("name", "Vitamin D").put("icon", "capsule")
                .put("color", "#f472b6").put("bg", "#1e0f2e").put("style", "heart").put("sites", new JSONArray());
        JSONObject cyan = testo(now + 20 * Sched.HOUR, now - 10 * Sched.HOUR).put("name", "Insulin long-acting pen injection").put("icon", "pen")
                .put("color", "#00e5ff").put("bg", "#0a1a2f").put("style", "tank");
        int n = 0;
        for (JSONObject m : new JSONObject[]{ok, due, pink, cyan}) {
            Store.save(c, new JSONArray().put(m));
            Bitmap[] frames = new Bitmap[4];
            for (int k = 0; k < 4; k++) frames[k] = Sprites.frame(m, Sched.frac(m, now), m.getLong("nextAt") < now ? 2 : 0, k, 4);
            for (int layout : new int[]{R.layout.widget_wide, R.layout.widget_small}) {
                RemoteViews rv = MedWidget.build(c, 5, m, 2, frames, layout);
                FrameLayout parent = new FrameLayout(c);
                View v = rv.apply(c, parent);          // throws if any action targets a missing view / bad method
                float d = c.getResources().getDisplayMetrics().density;
                int w = (int) ((layout == R.layout.widget_wide ? 320 : 150) * d), h = (int) ((layout == R.layout.widget_wide ? 150 : 150) * d);
                v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                v.layout(0, 0, w, h);
                Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(out); cv.drawColor(0xFF2B3A4A); v.draw(cv);
                File f = new File(System.getProperty("widgetOut", "build"), "widget_" + (n++) + ".png");
                f.getParentFile().mkdirs();
                try (FileOutputStream o = new FileOutputStream(f)) { out.compress(Bitmap.CompressFormat.PNG, 100, o); }
                assertTrue(f.length() > 0);
            }
        }
    }
}
