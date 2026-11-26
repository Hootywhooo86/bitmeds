package com.bitmeds.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Alarms (so doses ring when the app is closed) and the dose notifications. */
final class Alarms {
    static final String CHANNEL = "doses";

    /** Set one alarm for the next moment anything changes: a dose comes due, turns "soon", or a snooze ends. */
    static void reschedule(Context c) {
        JSONArray meds = Store.load(c);
        long now = System.currentTimeMillis(), best = Long.MAX_VALUE;
        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m == null) continue;
            long next = m.optLong("nextAt");
            if (next > now) best = Math.min(best, next);
            long soon = next - Sched.soonWindow(m);
            if (soon > now) best = Math.min(best, soon);
            long snooze = m.isNull("snoozeAt") ? 0 : m.optLong("snoozeAt", 0);
            if (snooze > now) best = Math.min(best, snooze);
            if (next - now > Sched.DAY) best = Math.min(best, next - ((next - now) / Sched.DAY) * Sched.DAY + 500); // widget day count steps down
        }
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, new Intent(c, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (best == Long.MAX_VALUE) { am.cancel(pi); return; }
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
    }

    /** Called when the alarm fires: notify for doses that just came due or whose snooze ran out. */
    static void fire(Context c) {
        JSONArray meds = Store.load(c);
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m == null) continue;
            try {
                long next = m.optLong("nextAt");
                if (next <= now && m.optLong("nAlerted", 0) != next) {
                    m.put("nAlerted", next); notify(c, m, false); changed = true;
                }
                long snooze = m.isNull("snoozeAt") ? 0 : m.optLong("snoozeAt", 0);
                if (snooze > 0 && snooze <= now) {
                    m.put("snoozeAt", JSONObject.NULL); notify(c, m, true); changed = true;
                }
            } catch (JSONException ignored) { }
        }
        if (changed) Store.save(c, meds);
        MedWidget.updateAll(c);
        reschedule(c);
        MainActivity.reloadIfOpen();
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Dose reminders", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Rings when a dose is due");
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 120, 80, 120, 80, 300});
        ch.enableLights(true);
        ch.setLightColor(0xFF34D399);
        nm.createNotificationChannel(ch);
    }

    static int notifId(String medId) { return 1000 + (medId.hashCode() & 0x7ffffff); }

    static void notify(Context c, JSONObject m, boolean again) {
        ensureChannel(c);
        String id = m.optString("id"), site = Sched.nextSite(m), desc = m.optString("desc", "");
        String text = Sched.doseLine(m) + (site.isEmpty() ? "" : " · site: " + site);
        String big = text + (desc.isEmpty() ? "" : "\n" + desc);
        int req = notifId(id);
        PendingIntent open = PendingIntent.getActivity(c, req, new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_syringe)
                .setColor(0xFF34D399)
                .setContentTitle((again ? "⏰ Reminder: " : "⏰ ") + m.optString("name") + " is due")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(big))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setWhen(m.optLong("nextAt"))
                .setShowWhen(true)
                .addAction(new Notification.Action.Builder(null, "↺ GAVE IT", action(c, GiveReceiver.ACTION_GIVE, id, req + 1)).build())
                .addAction(new Notification.Action.Builder(null, "SNOOZE 15 MIN", action(c, GiveReceiver.ACTION_SNOOZE, id, req + 2)).build())
                .build();
        try { c.getSystemService(NotificationManager.class).notify(req, n); } catch (SecurityException ignored) { }
    }

    static void cancel(Context c, String medId) {
        c.getSystemService(NotificationManager.class).cancel(notifId(medId));
    }

    static PendingIntent action(Context c, String action, String medId, int req) {
        Intent i = new Intent(c, GiveReceiver.class).setAction(action).putExtra(GiveReceiver.EXTRA_MED, medId);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void buzz(Context c, long ms) {
        Vibrator v = c.getSystemService(Vibrator.class);
        if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }
}
