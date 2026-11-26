package com.bitmeds.app;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Buttons on the widget and on notifications. */
public class GiveReceiver extends BroadcastReceiver {
    static final String ACTION_GIVE = "com.bitmeds.app.GIVE";
    static final String ACTION_WIDGET_GIVE = "com.bitmeds.app.WIDGET_GIVE";
    static final String ACTION_SNOOZE = "com.bitmeds.app.SNOOZE";
    static final String ACTION_SKIP = "com.bitmeds.app.SKIP";
    static final String ACTION_CYCLE = "com.bitmeds.app.CYCLE";
    static final String ACTION_PINNED = "com.bitmeds.app.PINNED";
    static final String EXTRA_MED = "med";

    /** Widget button intent. The data URI makes each (widget, med, action) a distinct PendingIntent, so a tap can never log the wrong med. */
    static Intent widgetIntent(Context c, String action, int wid, String medId) {
        return new Intent(c, GiveReceiver.class).setAction(action)
                .setData(android.net.Uri.parse("bitmeds://widget/" + wid + "/" + action + "/" + (medId == null ? "" : android.net.Uri.encode(medId))))
                .putExtra(EXTRA_MED, medId).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
    }
    /** The widget's GAVE IT button needs a second tap within this time, so a stray tap can't log a dose. */
    static final long CONFIRM_MS = 5000;

    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        String medId = intent.getStringExtra(EXTRA_MED);
        int wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (action == null) return;
        switch (action) {
            case ACTION_WIDGET_GIVE: {
                long now = System.currentTimeMillis();
                if (now - Store.armedAt(c, wid, medId) > CONFIRM_MS) {
                    Store.arm(c, wid, medId, now);
                    Alarms.buzz(c, 15);
                    MedWidget.updateAll(c);
                    // put the button back if the second tap never comes
                    PendingResult pr = goAsync();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        MedWidget.updateAll(c);
                        pr.finish();
                    }, CONFIRM_MS + 200);
                    return;
                }
                Store.arm(c, wid, null, 0);
                give(c, medId);
                return;
            }
            case ACTION_GIVE: give(c, medId); return;
            case ACTION_SNOOZE: snooze(c, medId); return;
            case ACTION_SKIP: {
                JSONArray meds = Store.load(c);
                JSONObject m = Store.find(meds, medId);
                if (m == null) return;
                Sched.skip(m, System.currentTimeMillis());
                Store.save(c, meds);
                Alarms.cancel(c, medId);
                Toast.makeText(c, m.optString("name") + " skipped. Next " + Sched.when(c, m.optLong("nextAt")), Toast.LENGTH_LONG).show();
                after(c);
                return;
            }
            case ACTION_CYCLE: cycle(c, wid); return;
            case ACTION_PINNED:
                if (wid != AppWidgetManager.INVALID_APPWIDGET_ID && medId != null) {
                    Store.setWidgetMed(c, wid, medId);
                    MedWidget.update(c, AppWidgetManager.getInstance(c), wid);
                }
                return;
            default:
        }
    }

    static void give(Context c, String medId) {
        JSONArray meds = Store.load(c);
        JSONObject m = Store.find(meds, medId);
        if (m == null) return;
        Supply before = Supply.of(m);
        Sched.give(m, System.currentTimeMillis());
        Store.save(c, meds);
        Supply sup = Supply.of(m);
        if (sup != null && sup.low && (before == null || !before.low))
            Alarms.simple(c, Alarms.notifId(medId) + 1, "⚠ " + m.optString("name") + ": refill soon",
                    sup.doses + (sup.doses == 1 ? " dose" : " doses") + " left (about " + Math.round(sup.daysLeft) + " days).");
        Alarms.cancel(c, medId);
        Alarms.buzz(c, 60);
        String site = Sched.nextSite(m);
        Toast.makeText(c, m.optString("name") + " logged ✓  Next " + Sched.when(c, m.optLong("nextAt"))
                + (site.isEmpty() ? "" : " · " + site), Toast.LENGTH_LONG).show();
        after(c);
    }

    private static void snooze(Context c, String medId) {
        JSONArray meds = Store.load(c);
        JSONObject m = Store.find(meds, medId);
        if (m == null) return;
        try { m.put("snoozeAt", System.currentTimeMillis() + 15 * Sched.MIN); } catch (JSONException ignored) { }
        Store.save(c, meds);
        Alarms.cancel(c, medId);
        Toast.makeText(c, "Snoozed 15 min", Toast.LENGTH_SHORT).show();
        after(c);
    }

    private static void cycle(Context c, int wid) {
        JSONArray meds = Store.load(c);
        if (meds.length() < 2) return;
        JSONObject cur = Store.forWidget(c, meds, wid);
        int idx = 0;
        for (int i = 0; i < meds.length(); i++) if (cur != null && cur.optString("id").equals(meds.optJSONObject(i).optString("id"))) idx = i;
        Store.setWidgetMed(c, wid, meds.optJSONObject((idx + 1) % meds.length()).optString("id"));
        Store.arm(c, wid, null, 0);
        MedWidget.update(c, AppWidgetManager.getInstance(c), wid);
    }

    private static void after(Context c) {
        MedWidget.updateAll(c);
        Alarms.reschedule(c);
        MainActivity.reloadIfOpen();
    }
}
