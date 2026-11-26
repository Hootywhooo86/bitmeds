package com.bitmeds.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** The med list, stored as the same JSON array the web app uses. */
final class Store {
    private static final String PREFS = "bitmeds", KEY = "meds";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String raw(Context c) { return prefs(c).getString(KEY, ""); }

    static synchronized void saveRaw(Context c, String json) { prefs(c).edit().putString(KEY, json).commit(); }

    static synchronized JSONArray load(Context c) {
        try { return new JSONArray(prefs(c).getString(KEY, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    static synchronized void save(Context c, JSONArray meds) { saveRaw(c, meds.toString()); }

    static JSONObject find(JSONArray meds, String id) {
        if (id == null) return null;
        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m != null && id.equals(m.optString("id"))) return m;
        }
        return null;
    }

    static void setWidgetMed(Context c, int widgetId, String medId) {
        prefs(c).edit().putString("w_" + widgetId, medId).commit();
    }

    static String widgetMed(Context c, int widgetId) { return prefs(c).getString("w_" + widgetId, null); }

    static void forgetWidget(Context c, int widgetId) {
        prefs(c).edit().remove("w_" + widgetId).remove("arm_" + widgetId).commit();
    }

    /** Widget "are you sure" state: the time the GAVE IT button was first tapped. */
    static long armedAt(Context c, int widgetId) { return prefs(c).getLong("arm_" + widgetId, 0); }

    static void arm(Context c, int widgetId, long t) { prefs(c).edit().putLong("arm_" + widgetId, t).commit(); }

    /** The med a widget shows: the one it was set to, else the pinned "main" med, else the next one due. */
    static JSONObject forWidget(Context c, JSONArray meds, int widgetId) {
        JSONObject m = find(meds, widgetMed(c, widgetId));
        if (m != null) return m;
        JSONObject best = null;
        for (int i = 0; i < meds.length(); i++) {
            JSONObject x = meds.optJSONObject(i);
            if (x == null) continue;
            if (x.optBoolean("pinned")) return x;
            if (best == null || x.optLong("nextAt") < best.optLong("nextAt")) best = x;
        }
        return best;
    }
}
