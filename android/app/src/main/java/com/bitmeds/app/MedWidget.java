package com.bitmeds.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Single-med home screen timer. The whole widget is a pixel-art picture drawn at its real size by
 * {@link WidgetArt} (pixel font, starfield, glowing timer, progress bar, GAVE IT button); the med's chosen
 * animation plays on top in a 4-frame ViewFlipper. Redrawn every minute (Alarms.scheduleTick), when a
 * dose is given / comes due, and when the widget is resized.
 */
public class MedWidget extends AppWidgetProvider {
    static final int[] FRAMES = {R.id.f0, R.id.f1, R.id.f2, R.id.f3};

    @Override public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) update(c, mgr, id);
        Alarms.reschedule(c);
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager mgr, int id, Bundle opts) { update(c, mgr, id); }

    @Override public void onDeleted(Context c, int[] ids) { for (int id : ids) Store.forgetWidget(c, id); }

    /** Redraws every bitmeds widget: single-med timers and the multi-med list. */
    static void updateAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        for (int id : mgr.getAppWidgetIds(new ComponentName(c, MedWidget.class))) update(c, mgr, id);
        MedListWidget.updateAll(c);
    }

    static boolean anyWidgets(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        return mgr.getAppWidgetIds(new ComponentName(c, MedWidget.class)).length > 0
                || mgr.getAppWidgetIds(new ComponentName(c, MedListWidget.class)).length > 0;
    }

    /** The widget's size in dp as the launcher shows it in portrait. */
    static float[] sizeDp(AppWidgetManager mgr, int wid, float defW, float defH) {
        Bundle o = mgr.getAppWidgetOptions(wid);
        if (o == null) return new float[]{defW, defH};
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<SizeF> sizes = o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if (sizes != null && !sizes.isEmpty()) return new float[]{sizes.get(0).getWidth(), sizes.get(0).getHeight()};
        }
        int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        return w > 0 && h > 0 ? new float[]{w, h} : new float[]{defW, defH};
    }

    static void update(Context c, AppWidgetManager mgr, int wid) {
        JSONArray meds = Store.load(c);
        JSONObject m = Store.forWidget(c, meds, wid);
        float[] sz = sizeDp(mgr, wid, 300, 150);
        mgr.updateAppWidget(wid, build(c, wid, m, meds.length(), sz[0], sz[1], System.currentTimeMillis()));
    }

    static RemoteViews build(Context c, int wid, JSONObject m, int count, float wDp, float hDp, long now) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_canvas);
        v.setOnClickPendingIntent(R.id.root, openApp(c));
        if (m == null) {
            WidgetArt.Geo g = new WidgetArt.Geo();
            v.setImageViewBitmap(R.id.bg, WidgetArt.list(c, new ArrayList<>(), 1, wDp, hDp, wid, now, g));
            v.setViewVisibility(R.id.flip, View.GONE);
            v.setViewVisibility(R.id.give, View.GONE);
            return v;
        }
        boolean armed = now - Store.armedAt(c, wid, m.optString("id")) <= GiveReceiver.CONFIRM_MS;
        WidgetArt.Geo g = new WidgetArt.Geo();
        v.setImageViewBitmap(R.id.bg, WidgetArt.single(c, m, wDp, hDp, armed, now, g));

        if (g.art != null) {
            int state = MedListWidget.stateOf(m, now);
            for (int k = 0; k < 4; k++) v.setImageViewBitmap(FRAMES[k], Sprites.frameExact(m, Sched.frac(m, now), state, k, g.art.width()));
            place(v, R.id.flip, g.art, g.d);
            v.setViewVisibility(R.id.flip, View.VISIBLE);
        } else v.setViewVisibility(R.id.flip, View.GONE);

        place(v, R.id.give, g.give, g.d);
        v.setViewVisibility(R.id.give, View.VISIBLE);
        Intent give = GiveReceiver.widgetIntent(c, GiveReceiver.ACTION_WIDGET_GIVE, wid, m.optString("id"));
        v.setOnClickPendingIntent(R.id.give, PendingIntent.getBroadcast(c, 0, give, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }

    static PendingIntent openApp(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Puts an overlay exactly over its spot in the picture (Android 12+; older launchers use the XML positions). */
    static void place(RemoteViews v, int id, Rect r, float d) {
        if (Build.VERSION.SDK_INT < 31 || r == null) return;
        int dip = TypedValue.COMPLEX_UNIT_DIP;
        v.setViewLayoutWidth(id, r.width() / d, dip);
        v.setViewLayoutHeight(id, r.height() / d, dip);
        v.setViewLayoutMargin(id, RemoteViews.MARGIN_START, r.left / d, dip);
        v.setViewLayoutMargin(id, RemoteViews.MARGIN_TOP, r.top / d, dip);
        v.setViewLayoutMargin(id, RemoteViews.MARGIN_END, 0, dip);
        v.setViewLayoutMargin(id, RemoteViews.MARGIN_BOTTOM, 0, dip);
    }

    static double lum(int[] c) { return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255; }

    /** "Right glute" → "R GLUTE". */
    static String shortSite(String site) {
        String s = site.toUpperCase(java.util.Locale.getDefault());
        return s.replaceFirst("^LEFT ", "L ").replaceFirst("^RIGHT ", "R ");
    }
}
