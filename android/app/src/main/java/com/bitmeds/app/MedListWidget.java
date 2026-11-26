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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "bitmeds · all meds": several meds in one widget, soonest first. Each med gets its own square in its
 * own colours with its animation, countdown and GAVE IT button (tap twice, like the single widget).
 * 2 rows at the default 4x2 size, 4 rows plus a header when stretched taller.
 */
public class MedListWidget extends AppWidgetProvider {
    static final int MAX = 4;
    // per row: box, bg, line, frame0..3, name, chrono, give, give shadow, give face, give text
    private static final int[][] ROW = {
            {R.id.row0, R.id.r0_bg, R.id.r0_line, R.id.r0_f0, R.id.r0_f1, R.id.r0_f2, R.id.r0_f3, R.id.r0_name, R.id.r0_chrono, R.id.r0_give, R.id.r0_give_shadow, R.id.r0_give_face, R.id.r0_give_text},
            {R.id.row1, R.id.r1_bg, R.id.r1_line, R.id.r1_f0, R.id.r1_f1, R.id.r1_f2, R.id.r1_f3, R.id.r1_name, R.id.r1_chrono, R.id.r1_give, R.id.r1_give_shadow, R.id.r1_give_face, R.id.r1_give_text},
            {R.id.row2, R.id.r2_bg, R.id.r2_line, R.id.r2_f0, R.id.r2_f1, R.id.r2_f2, R.id.r2_f3, R.id.r2_name, R.id.r2_chrono, R.id.r2_give, R.id.r2_give_shadow, R.id.r2_give_face, R.id.r2_give_text},
            {R.id.row3, R.id.r3_bg, R.id.r3_line, R.id.r3_f0, R.id.r3_f1, R.id.r3_f2, R.id.r3_f3, R.id.r3_name, R.id.r3_chrono, R.id.r3_give, R.id.r3_give_shadow, R.id.r3_give_face, R.id.r3_give_text}
    };
    private static final int C_YELLOW = 0xFFFACC15, C_RED = 0xFFFF5D5D;

    @Override public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) update(c, mgr, id);
        Alarms.reschedule(c);
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager mgr, int id, Bundle opts) { update(c, mgr, id); }

    @Override public void onDeleted(Context c, int[] ids) { for (int id : ids) Store.forgetWidget(c, id); }

    static void updateAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        for (int id : mgr.getAppWidgetIds(new ComponentName(c, MedListWidget.class))) update(c, mgr, id);
    }

    static List<JSONObject> sorted(JSONArray meds) {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < meds.length(); i++) if (meds.optJSONObject(i) != null) list.add(meds.optJSONObject(i));
        list.sort((a, b) -> Long.compare(a.optLong("nextAt"), b.optLong("nextAt")));
        return list;
    }

    static Bitmap[][] frames(List<JSONObject> meds, long now) {
        Bitmap[][] f = new Bitmap[Math.min(MAX, meds.size())][4];
        for (int i = 0; i < f.length; i++) {
            JSONObject m = meds.get(i);
            long left = m.optLong("nextAt") - now;
            int state = left <= 0 ? Sprites.STATE_DUE : Sched.isSoon(m, left) ? Sprites.STATE_SOON : Sprites.STATE_OK;
            for (int k = 0; k < 4; k++) f[i][k] = Sprites.frame(m, Sched.frac(m, now), state, k, 2);
        }
        return f;
    }

    static void update(Context c, AppWidgetManager mgr, int wid) {
        long now = System.currentTimeMillis();
        List<JSONObject> meds = sorted(Store.load(c));
        Bitmap[][] f = frames(meds, now);
        RemoteViews two = build(c, wid, meds, f, 2, now), four = build(c, wid, meds, f, 4, now);
        RemoteViews views;
        if (Build.VERSION.SDK_INT >= 31) {
            Map<SizeF, RemoteViews> map = new HashMap<>();
            map.put(new SizeF(180f, 100f), two);
            map.put(new SizeF(180f, 200f), four);
            views = new RemoteViews(map);
        } else {
            views = mgr.getAppWidgetOptions(wid).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110) >= 200 ? four : two;
        }
        mgr.updateAppWidget(wid, views);
    }

    static RemoteViews build(Context c, int wid, List<JSONObject> meds, Bitmap[][] frames, int rows, long now) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_list);
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, open);
        boolean empty = meds.isEmpty();
        v.setViewVisibility(R.id.content, empty ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.empty, empty ? View.VISIBLE : View.GONE);

        int due = 0;
        for (JSONObject m : meds) if (m.optLong("nextAt") <= now) due++;
        v.setViewVisibility(R.id.list_header, rows > 2 ? View.VISIBLE : View.GONE);
        int extra = Math.max(0, meds.size() - rows);
        v.setTextViewText(R.id.list_status, meds.size() + (meds.size() == 1 ? " MED · " : " MEDS · ")
                + (due > 0 ? due + " DUE" : "ALL GOOD") + (extra > 0 ? " · +" + extra + " MORE" : ""));
        v.setTextColor(R.id.list_status, due > 0 ? C_RED : 0xFF9A97B8);
        v.setInt(R.id.panel_border, "setColorFilter", due > 0 ? C_RED : 0xFF2A2A4A);

        int[] white = {255, 255, 255};
        for (int i = 0; i < MAX; i++) {
            int[] id = ROW[i];
            boolean show = i < rows && i < meds.size();
            v.setViewVisibility(id[0], show ? View.VISIBLE : View.GONE);
            if (!show) continue;
            JSONObject m = meds.get(i);
            String medId = m.optString("id");
            long left = m.optLong("nextAt") - now;
            boolean isDue = left <= 0, soon = Sched.isSoon(m, left);
            boolean armed = now - Store.armedAt(c, wid, medId) <= GiveReceiver.CONFIRM_MS;
            int[][] acc = Pixel.shades(Pixel.hex(Sprites.accent(m)));
            int[] bg = Sprites.bg(m);
            int tone = isDue ? C_RED : soon ? C_YELLOW : Sprites.argb(acc[0]);
            int[] face = armed ? new int[]{250, 204, 21} : isDue ? new int[]{255, 93, 93} : acc[0];

            v.setInt(id[1], "setColorFilter", Sprites.argb(bg));
            v.setInt(id[2], "setColorFilter", isDue ? C_RED : Sprites.argb(Pixel.mix(bg, white, 0.16)));
            for (int k = 0; k < 4; k++) v.setImageViewBitmap(id[3 + k], frames[i][k]);
            v.setTextViewText(id[7], m.optString("name"));

            long days = left >= Sched.DAY ? left / Sched.DAY : 0;
            String fmt = days > 0 && !isDue ? days + "D %s" : null; // late = red + minus sign, keeps rows short
            v.setChronometer(id[8], SystemClock.elapsedRealtime() + left - days * Sched.DAY, fmt, true);
            v.setChronometerCountDown(id[8], true);
            v.setTextColor(id[8], tone);

            v.setInt(id[11], "setColorFilter", Sprites.argb(face));
            v.setInt(id[10], "setColorFilter", Sprites.argb(Pixel.shades(face)[2]));
            v.setTextViewText(id[12], armed ? "AGAIN?" : "↺ GAVE");
            v.setTextColor(id[12], MedWidget.lum(face) > 0.55 ? 0xFF0B0B16 : 0xFFFFFFFF);
            Intent give = GiveReceiver.widgetIntent(c, GiveReceiver.ACTION_WIDGET_GIVE, wid, medId);
            v.setOnClickPendingIntent(id[9], PendingIntent.getBroadcast(c, 0, give, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return v;
    }
}
