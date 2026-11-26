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
                .put("siteIdx", 1).put("nextAt", nextAt).put("periodStart", periodStart).put("log", new JSONArray());
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

    @Test public void rendersWidgetSizes() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        long now = System.currentTimeMillis();
        JSONObject ok = testo(now + 2 * Sched.DAY + 13 * Sched.HOUR, now - 30 * Sched.HOUR);
        JSONObject due = testo(now - 12 * Sched.MIN, now - 4 * Sched.DAY);
        int n = 0;
        for (JSONObject m : new JSONObject[]{ok, due}) {
            Store.save(c, new JSONArray().put(m));
            Bitmap[] frames = new Bitmap[4];
            for (int k = 0; k < 4; k++) frames[k] = Sprites.frame("injection", Sched.frac(m, now), m.getLong("nextAt") < now ? 2 : 0, k, 4);
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
