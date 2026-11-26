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
        JSONObject set = Store.settings(c);
        long headMs = set.optLong("headsUpH", 12) * Sched.HOUR, nagMs = set.optLong("nagMin", 30) * Sched.MIN;
        long now = System.currentTimeMillis(), best = Long.MAX_VALUE;
        JSONObject trip = Store.extra(c).optJSONObject("trip");
        if (trip != null && trip.optBoolean("remind") && !trip.optBoolean("reminded") && trip.optLong("remindAt") > now) best = trip.optLong("remindAt");
        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m == null) continue;
            long next = m.optLong("nextAt");
            if (headMs > 0 && next - headMs > now && m.optLong("nHead") != next) best = Math.min(best, next - headMs);
            if (nagMs > 0 && next <= now && m.optLong("nNagFor") == next && m.optInt("nNagCount") < 12) best = Math.min(best, Math.max(now + 5000, m.optLong("nNagAt", next) + nagMs));
            if (nagMs > 0 && next <= now && m.optLong("nNagFor") != next) best = Math.min(best, Math.max(now + 5000, next + nagMs));
            if (next > now) best = Math.min(best, next);
            long soon = next - Sched.soonWindow(m);
            if (soon > now) best = Math.min(best, soon);
            long snooze = m.isNull("snoozeAt") ? 0 : m.optLong("snoozeAt", 0);
            if (snooze > now) best = Math.min(best, snooze);
            if (next - now > Sched.DAY) best = Math.min(best, next - ((next - now) / Sched.DAY) * Sched.DAY + 500); // widget day count steps down
        }
        scheduleTick(c);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, new Intent(c, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (best == Long.MAX_VALUE) { am.cancel(pi); return; }
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
    }

    static final String ACTION_TICK = "com.bitmeds.app.TICK";

    /** The widgets show minutes, so redraw them at the next minute boundary. RTC (not _WAKEUP): never wakes the phone. */
    static void scheduleTick(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 2, new Intent(c, AlarmReceiver.class).setAction(ACTION_TICK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (!MedWidget.anyWidgets(c)) { am.cancel(pi); return; }
        long next = (System.currentTimeMillis() / 60_000 + 1) * 60_000 + 200;
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExact(AlarmManager.RTC, next, pi);
        else am.set(AlarmManager.RTC, next, pi);
    }

    /** Called when the alarm fires: notify for doses that just came due or whose snooze ran out. */
    static void fire(Context c) {
        JSONArray meds = Store.load(c);
        JSONObject set = Store.settings(c);
        long headMs = set.optLong("headsUpH", 12) * Sched.HOUR, nagMs = set.optLong("nagMin", 30) * Sched.MIN;
        long now = System.currentTimeMillis();
        boolean changed = false;
        // trip: "pack your meds" the evening before
        JSONObject x = Store.extra(c), trip = x.optJSONObject("trip");
        if (trip != null && trip.optBoolean("remind") && !trip.optBoolean("reminded") && trip.optLong("remindAt") > 0 && trip.optLong("remindAt") <= now) {
            try { trip.put("reminded", true); Store.saveExtraRaw(c, x.toString()); } catch (JSONException ignored) { }
            simple(c, 999, "🧳 Pack your meds", "You leave tomorrow — open bitmeds for your packing list.");
        }
        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m == null) continue;
            try {
                long next = m.optLong("nextAt");
                // heads-up before the dose ("Testosterone at 20:00 tomorrow")
                if (headMs > 0 && now >= next - headMs && now < next - 60_000 && m.optLong("nHead") != next) {
                    m.put("nHead", next); changed = true;
                    simple(c, notifId(m.optString("id")), "🔔 " + m.optString("name") + " " + Sched.when(c, next), Sched.doseLine(m) + (Sched.nextSite(m).isEmpty() ? "" : " · site: " + Sched.nextSite(m)));
                }
                // keep nagging while overdue
                if (nagMs > 0 && next <= now && m.optLong("nAlerted") == next) {
                    if (m.optLong("nNagFor") != next) { m.put("nNagFor", next); m.put("nNagCount", 0); m.put("nNagAt", next); changed = true; }
                    if (m.optInt("nNagCount") < 12 && now >= m.optLong("nNagAt", next) + nagMs - 30_000) {
                        m.put("nNagCount", m.optInt("nNagCount") + 1); m.put("nNagAt", now); changed = true;
                        notify(c, m, true);
                    }
                }
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
                .addAction(new Notification.Action.Builder(null, "SKIP", action(c, GiveReceiver.ACTION_SKIP, id, req + 3)).build())
                .build();
        try { c.getSystemService(NotificationManager.class).notify(req, n); } catch (SecurityException ignored) { }
    }

    /** A plain notification (heads-up, trip, low supply) that opens the app. */
    static void simple(Context c, int id, String title, String text) {
        ensureChannel(c);
        PendingIntent open = PendingIntent.getActivity(c, id, new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_stat_syringe).setColor(0xFF34D399)
                .setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).build();
        try { c.getSystemService(NotificationManager.class).notify(id, n); } catch (SecurityException ignored) { }
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
