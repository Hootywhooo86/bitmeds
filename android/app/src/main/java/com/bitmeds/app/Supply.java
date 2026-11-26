package com.bitmeds.app;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Supply left (vial / pill count), on-time streak and schedule label for a med.
 * Mirrors supplyOf / streakOf / schedShort in app/index.html.
 */
final class Supply {
    int doses; double daysLeft; boolean low; double left, size;

    /** null when the med has no supply set. Supply is kept in the dose's own unit (ml, tablets…). */
    static Supply of(JSONObject m) {
        JSONObject s = m.optJSONObject("supply");
        if (s == null || !s.has("left")) return null;
        double amount = m.optDouble("amount", 1);
        if (amount <= 0) return null;
        Supply r = new Supply();
        r.left = Math.max(0, s.optDouble("left", 0));
        r.size = s.optDouble("size", r.left);
        r.doses = (int) Math.floor(r.left / amount + 1e-9);
        r.daysLeft = r.doses * daysPerDose(m);
        r.low = r.daysLeft <= s.optDouble("lowDays", 14);
        return r;
    }

    static double daysPerDose(JSONObject m) {
        if ("days".equals(m.optString("sched"))) {
            JSONArray d = m.optJSONArray("days");
            return 7.0 / Math.max(1, d == null ? 1 : d.length());
        }
        return m.optLong("intervalMin", 480) / 1440.0;
    }

    /** Doses in a row given on time (within 6h of due). Deliberate skips don't break it; a late dose does. */
    static int streak(JSONObject m) {
        JSONArray h = m.optJSONArray("hist");
        if (h == null) return 0;
        int n = 0;
        for (int i = 0; i < h.length(); i++) {
            JSONObject e = h.optJSONObject(i);
            if (e == null) continue;
            if (e.optBoolean("skip")) continue;
            long due = e.optLong("due", 0), t = e.optLong("t");
            if (due > 0 && t > due + Sched.GRACE) break;
            n++;
        }
        return n;
    }

    static String schedLabel(JSONObject m) {
        if ("days".equals(m.optString("sched"))) {
            String[] N = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
            JSONArray d = m.optJSONArray("days");
            StringBuilder b = new StringBuilder();
            int[] order = {1, 2, 3, 4, 5, 6, 0};
            int count = d == null ? 0 : d.length();
            if (count == 7) b.append("DAILY");
            else for (int o : order) for (int i = 0; i < count; i++) if (d.optInt(i) == o) b.append(b.length() > 0 ? "+" : "").append(N[o]);
            return b + " " + m.optString("time", "");
        }
        long min = m.optLong("intervalMin", 480);
        return min % 1440 == 0 ? "EVERY " + (min / 1440) + "D" : min % 60 == 0 ? "EVERY " + (min / 60) + "H" : "EVERY " + min + "M";
    }
}
