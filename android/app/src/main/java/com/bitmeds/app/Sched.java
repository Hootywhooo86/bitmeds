package com.bitmeds.app;

import android.content.Context;
import android.text.format.DateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Schedule math. Mirrors nextSlotAfter / nextAfterGiven / giveDose in app/index.html. */
final class Sched {
    static final long MIN = 60_000L, HOUR = 3_600_000L, DAY = 86_400_000L;
    /** A dose given within 6h of a set-day slot counts for that slot. */
    static final long GRACE = 6 * HOUR;

    static long nextSlotAfter(JSONObject m, long t) {
        String[] hm = m.optString("time", "20:00").split(":");
        int hh = parse(hm, 0, 20), mm = parse(hm, 1, 0);
        JSONArray days = m.optJSONArray("days");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        for (int i = 0; i < 15; i++) {
            c.set(Calendar.HOUR_OF_DAY, hh);
            c.set(Calendar.MINUTE, mm);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            int dow = c.get(Calendar.DAY_OF_WEEK) - 1; // Sunday = 0, as in JS
            if (has(days, dow) && c.getTimeInMillis() > t) return c.getTimeInMillis();
            c.add(Calendar.DATE, 1);
        }
        return t + 7 * DAY;
    }

    static long nextAfterGiven(JSONObject m, long t) {
        if ("days".equals(m.optString("sched"))) return nextSlotAfter(m, Math.max(t + GRACE, m.optLong("nextAt", 0)));
        return t + m.optLong("intervalMin", 480) * MIN;
    }

    /** Log a dose now: same fields as giveDose() in the web app. */
    static void give(JSONObject m, long now) {
        try {
            JSONArray old = m.optJSONArray("log"), log = new JSONArray();
            log.put(now);
            if (old != null) for (int i = 0; i < old.length() && log.length() < 60; i++) log.put(old.optLong(i));
            m.put("log", log);
            // history entry (when given vs when it was due, which site) — newest first, kept to 300
            JSONObject e = new JSONObject().put("t", now).put("due", m.optLong("nextAt"));
            JSONArray sites = m.optJSONArray("sites");
            if (sites != null && sites.length() > 0) e.put("site", sites.optString(Math.floorMod(m.optInt("siteIdx", 0), sites.length())));
            JSONArray oh = m.optJSONArray("hist"), hist = new JSONArray().put(e);
            if (oh != null) for (int i = 0; i < oh.length() && hist.length() < 300; i++) hist.put(oh.opt(i));
            m.put("hist", hist);
            JSONObject sup = m.optJSONObject("supply");
            if (sup != null && sup.has("left")) sup.put("left", Math.max(0, Math.round((sup.optDouble("left") - m.optDouble("amount", 1)) * 1000) / 1000.0));
            m.put("u", now);
            if (sites != null && sites.length() > 0) {
                int idx = Math.floorMod(m.optInt("siteIdx", 0), sites.length());
                m.put("lastSite", sites.optString(idx));
                m.put("siteIdx", (idx + 1) % sites.length());
            }
            m.put("nextAt", nextAfterGiven(m, now));
            m.put("periodStart", now);
            m.put("snoozeAt", JSONObject.NULL);
        } catch (JSONException ignored) { }
    }

    /** Skip this dose on purpose: logged in history, timer moves on, supply and site rotation unchanged. */
    static void skip(JSONObject m, long now) {
        try {
            JSONObject e = new JSONObject().put("t", now).put("due", m.optLong("nextAt")).put("skip", true);
            JSONArray oh = m.optJSONArray("hist"), hist = new JSONArray().put(e);
            if (oh != null) for (int i = 0; i < oh.length() && hist.length() < 300; i++) hist.put(oh.opt(i));
            m.put("hist", hist);
            m.put("nextAt", nextAfterGiven(m, now));
            m.put("periodStart", now);
            m.put("snoozeAt", JSONObject.NULL);
            m.put("u", now);
        } catch (JSONException ignored) { }
    }

    static String nextSite(JSONObject m) {
        JSONArray s = m.optJSONArray("sites");
        if (s == null || s.length() == 0) return "";
        return s.optString(Math.floorMod(m.optInt("siteIdx", 0), s.length()));
    }

    static long periodStart(JSONObject m) {
        long next = m.optLong("nextAt");
        return m.has("periodStart") && !m.isNull("periodStart") ? m.optLong("periodStart") : next - m.optLong("intervalMin", 480) * MIN;
    }

    static float frac(JSONObject m, long now) {
        long next = m.optLong("nextAt"), span = Math.max(MIN, next - periodStart(m)), left = next - now;
        if (left <= 0) return 1f;
        return Math.max(0f, Math.min(1f, 1f - (float) left / span));
    }

    static long soonWindow(JSONObject m) {
        return Math.min(30 * MIN, (long) ((m.optLong("nextAt") - periodStart(m)) * 0.1));
    }

    static boolean isSoon(JSONObject m, long left) { return left > 0 && left <= soonWindow(m); }

    static String doseLine(JSONObject m) {
        double a = m.optDouble("amount", 1);
        String amt = a == Math.rint(a) ? String.valueOf((long) a) : String.valueOf(Math.round(a * 1000) / 1000.0);
        String s = amt + " " + m.optString("unit", "");
        String str = m.optString("strength", "");
        return str.isEmpty() ? s : s + " · " + str;
    }

    static String when(Context c, long ts) {
        Calendar now = Calendar.getInstance(), d = Calendar.getInstance();
        d.setTimeInMillis(ts);
        String time = DateFormat.getTimeFormat(c).format(new Date(ts));
        int dd = dayDiff(now, d);
        if (dd == 0) return "today " + time;
        if (dd == 1) return "tomorrow " + time;
        if (dd == -1) return "yesterday " + time;
        String day = d.getDisplayName(Calendar.DAY_OF_WEEK, Math.abs(dd) < 6 ? Calendar.LONG : Calendar.SHORT, Locale.getDefault());
        return day + " " + time;
    }

    private static int dayDiff(Calendar a, Calendar b) {
        Calendar x = (Calendar) a.clone(), y = (Calendar) b.clone();
        for (Calendar k : new Calendar[]{x, y}) {
            k.set(Calendar.HOUR_OF_DAY, 12); k.set(Calendar.MINUTE, 0); k.set(Calendar.SECOND, 0); k.set(Calendar.MILLISECOND, 0);
        }
        return (int) Math.round((y.getTimeInMillis() - x.getTimeInMillis()) / (double) DAY);
    }

    private static boolean has(JSONArray a, int v) {
        if (a == null) return false;
        for (int i = 0; i < a.length(); i++) if (a.optInt(i, -1) == v) return true;
        return false;
    }

    private static int parse(String[] p, int i, int def) {
        try { return Integer.parseInt(p[i].trim()); } catch (Exception e) { return def; }
    }
}
