package com.bitmeds.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * "bitmeds · all meds": several meds in one widget, overdue/soonest first, each in its own square in its own
 * colours with its animation, timer and GAVE button (tap twice). As many rows as fit (up to 4); one med
 * uses the full single-med look so the widget is always filled.
 */
public class MedListWidget extends AppWidgetProvider {
    static final int MAX = 4;
    private static final int[] FLIP = {R.id.r0_flip, R.id.r1_flip, R.id.r2_flip, R.id.r3_flip};
    private static final int[] HIT = {R.id.r0_hit, R.id.r1_hit, R.id.r2_hit, R.id.r3_hit};
    private static final int[][] FR = {{R.id.r0_f0, R.id.r0_f1, R.id.r0_f2, R.id.r0_f3}, {R.id.r1_f0, R.id.r1_f1, R.id.r1_f2, R.id.r1_f3},
            {R.id.r2_f0, R.id.r2_f1, R.id.r2_f2, R.id.r2_f3}, {R.id.r3_f0, R.id.r3_f1, R.id.r3_f2, R.id.r3_f3}};

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

    static int stateOf(JSONObject m, long now) {
        long left = m.optLong("nextAt") - now;
        return left <= 0 ? Sprites.STATE_DUE : Sched.isSoon(m, left) ? Sprites.STATE_SOON : Sprites.STATE_OK;
    }

    /** Rows that fit: each med row wants ~46dp of height. */
    static int rowsFor(float hDp) { return Math.max(1, Math.min(MAX, (int) ((hDp - 30) / 46))); }

    static void update(Context c, AppWidgetManager mgr, int wid) {
        long now = System.currentTimeMillis();
        List<JSONObject> meds = sorted(Store.load(c));
        float[] sz = MedWidget.sizeDp(mgr, wid, 300, 200);
        mgr.updateAppWidget(wid, build(c, wid, meds, sz[0], sz[1], now));
    }

    static RemoteViews build(Context c, int wid, List<JSONObject> meds, float wDp, float hDp, long now) {
        if (meds.size() == 1) return MedWidget.build(c, wid, meds.get(0), 1, wDp, hDp, now);  // one med: full look, no empty rows
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_list_canvas);
        v.setOnClickPendingIntent(R.id.root, MedWidget.openApp(c));
        WidgetArt.Geo g = new WidgetArt.Geo();
        int rows = rowsFor(hDp);
        v.setImageViewBitmap(R.id.bg, WidgetArt.list(c, meds, rows, wDp, hDp, wid, now, g));
        for (int i = 0; i < MAX; i++) {
            boolean show = i < g.rowGive.length;
            v.setViewVisibility(HIT[i], show ? View.VISIBLE : View.GONE);
            v.setViewVisibility(FLIP[i], show && g.rowArt[i] != null ? View.VISIBLE : View.GONE);
            if (!show) continue;
            JSONObject m = meds.get(i);
            if (g.rowArt[i] != null) {
                int scale = Math.max(1, g.rowArt[i].width() / Pixel.G), state = stateOf(m, now);
                for (int k = 0; k < 4; k++) v.setImageViewBitmap(FR[i][k], Sprites.frame(m, Sched.frac(m, now), state, k, scale));
                MedWidget.place(v, FLIP[i], g.rowArt[i], g.d);
            }
            MedWidget.place(v, HIT[i], g.rowGive[i], g.d);
            Intent give = GiveReceiver.widgetIntent(c, GiveReceiver.ACTION_WIDGET_GIVE, wid, m.optString("id"));
            v.setOnClickPendingIntent(HIT[i], PendingIntent.getBroadcast(c, 0, give, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return v;
    }
}
