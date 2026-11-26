package com.bitmeds.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Home screen timer. The countdown is a Chronometer, so it ticks every second on its own; the ring +
 * animation is a 4-frame ViewFlipper in the med's chosen style (ring, battery, hourglass, heart, tank, hop),
 * drawn by Pixel.java in the med's colours; it turns yellow when the dose is close and red when it's due.
 * Bitmaps are redrawn whenever a dose is given, comes due or gets close, and every 30 min.
 */
public class MedWidget extends AppWidgetProvider {
    private static final int[] FRAMES = {R.id.f0, R.id.f1, R.id.f2, R.id.f3};
    private static final int C_GREEN = 0xFF34D399, C_YELLOW = 0xFFFACC15, C_RED = 0xFFFF5D5D;

    @Override public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) update(c, mgr, id);
        Alarms.reschedule(c);
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager mgr, int id, Bundle opts) { update(c, mgr, id); }

    @Override public void onDeleted(Context c, int[] ids) { for (int id : ids) Store.forgetWidget(c, id); }

    static void updateAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        for (int id : mgr.getAppWidgetIds(new ComponentName(c, MedWidget.class))) update(c, mgr, id);
    }

    static void update(Context c, AppWidgetManager mgr, int wid) {
        JSONArray meds = Store.load(c);
        JSONObject m = Store.forWidget(c, meds, wid);
        Bitmap[] frames = null;
        if (m != null) {
            long now = System.currentTimeMillis(), left = m.optLong("nextAt") - now;
            int state = left <= 0 ? Sprites.STATE_DUE : Sched.isSoon(m, left) ? Sprites.STATE_SOON : Sprites.STATE_OK;
            float frac = Sched.frac(m, now);
            frames = new Bitmap[4];
            for (int k = 0; k < 4; k++) frames[k] = Sprites.frame(m, frac, state, k, 4);
        }
        RemoteViews wide = build(c, wid, m, meds.length(), frames, R.layout.widget_wide);
        RemoteViews small = build(c, wid, m, meds.length(), frames, R.layout.widget_small);
        RemoteViews views;
        if (Build.VERSION.SDK_INT >= 31) {
            Map<SizeF, RemoteViews> map = new HashMap<>();
            map.put(new SizeF(100f, 100f), small);
            map.put(new SizeF(220f, 100f), wide);
            views = new RemoteViews(map);
        } else {
            Bundle o = mgr.getAppWidgetOptions(wid);
            views = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) >= 220 ? wide : small;
        }
        mgr.updateAppWidget(wid, views);
    }

    private static double lum(int[] c) { return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255; }

    /** "Right glute" → "R GLUTE" so it fits beside the due time. */
    static String shortSite(String site) {
        String s = site.toUpperCase(java.util.Locale.getDefault());
        return s.replaceFirst("^LEFT ", "L ").replaceFirst("^RIGHT ", "R ");
    }

    static RemoteViews build(Context c, int wid, JSONObject m, int count, Bitmap[] frames, int layout) {
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, open);
        if (m == null) {
            v.setViewVisibility(R.id.content, View.GONE);
            v.setViewVisibility(R.id.empty, View.VISIBLE);
            return v;
        }
        v.setViewVisibility(R.id.content, View.VISIBLE);
        v.setViewVisibility(R.id.empty, View.GONE);

        long now = System.currentTimeMillis(), next = m.optLong("nextAt"), left = next - now;
        boolean due = left <= 0, soon = Sched.isSoon(m, left);
        boolean armed = now - Store.armedAt(c, wid) <= GiveReceiver.CONFIRM_MS;

        // the med's colours: accent (main / light / dark) and its background
        int[][] acc = Pixel.shades(Pixel.hex(Sprites.accent(m)));
        int[] bg = Sprites.bg(m), white = {255, 255, 255};
        int tone = due ? C_RED : soon ? C_YELLOW : Sprites.argb(acc[0]);
        int[] face = armed ? new int[]{250, 204, 21} : due ? new int[]{255, 93, 93} : acc[0];
        v.setInt(R.id.panel_fill, "setColorFilter", Sprites.argb(bg));
        v.setInt(R.id.panel_border, "setColorFilter", due ? C_RED : Sprites.argb(Pixel.mix(bg, white, 0.16)));
        v.setInt(R.id.give_face, "setColorFilter", Sprites.argb(face));
        v.setInt(R.id.give_shadow, "setColorFilter", Sprites.argb(Pixel.shades(face)[2]));
        v.setTextColor(R.id.give_text, lum(face) > 0.55 ? 0xFF0B0B16 : 0xFFFFFFFF);

        for (int k = 0; k < 4; k++) v.setImageViewBitmap(FRAMES[k], frames[k]);
        v.setTextViewText(R.id.name, m.optString("name"));
        v.setTextViewText(R.id.dose, Sched.doseLine(m));
        v.setTextColor(R.id.dose, Sprites.argb(acc[1]));

        // Chronometer can't show days: show "3D" beside it and count the remainder. Alarms.reschedule
        // wakes us when the remainder hits zero so the day count steps down.
        long days = left >= Sched.DAY ? left / Sched.DAY : 0;
        v.setChronometer(R.id.chrono, SystemClock.elapsedRealtime() + left - days * Sched.DAY, null, true);
        v.setChronometerCountDown(R.id.chrono, true);
        v.setTextColor(R.id.chrono, tone);
        v.setTextViewText(R.id.days, due ? "LATE " : days > 0 ? days + "D " : "");
        v.setTextColor(R.id.days, tone);
        v.setViewVisibility(R.id.days, due || days > 0 ? View.VISIBLE : View.GONE);
        v.setTextViewText(R.id.due, (due ? "was due " : "due ") + Sched.when(c, next));
        v.setTextColor(R.id.due, Sprites.argb(Pixel.mix(bg, white, 0.58)));

        String site = Sched.nextSite(m);
        v.setViewVisibility(R.id.site, site.isEmpty() ? View.GONE : View.VISIBLE);
        v.setTextViewText(R.id.site, shortSite(site));
        v.setTextColor(R.id.site, Sprites.argb(acc[1]));
        v.setInt(R.id.site, "setBackgroundColor", Sprites.argb(Pixel.mix(bg, white, 0.08)));

        v.setTextViewText(R.id.give_text, armed ? "TAP AGAIN TO LOG" : layout == R.layout.widget_small ? "↺ GAVE IT" : "↺ GAVE IT · RESET");
        Intent give = new Intent(c, GiveReceiver.class).setAction(GiveReceiver.ACTION_WIDGET_GIVE)
                .putExtra(GiveReceiver.EXTRA_MED, m.optString("id")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
        v.setOnClickPendingIntent(R.id.give, PendingIntent.getBroadcast(c, wid * 8 + 1, give, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        v.setViewVisibility(R.id.cycle, count > 1 ? View.VISIBLE : View.GONE);
        v.setTextColor(R.id.cycle, Sprites.argb(acc[1]));
        Intent cyc = new Intent(c, GiveReceiver.class).setAction(GiveReceiver.ACTION_CYCLE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
        v.setOnClickPendingIntent(R.id.cycle, PendingIntent.getBroadcast(c, wid * 8 + 2, cyc, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
}
