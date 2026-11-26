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

    /** Non-med data from the app (settings, trip, labs, profiles) — used here for reminder settings + the trip alarm. */
    static String rawExtra(Context c) { return prefs(c).getString("extra", ""); }

    static synchronized void saveExtraRaw(Context c, String json) { prefs(c).edit().putString("extra", json).commit(); }

    static JSONObject extra(Context c) {
        try { return new JSONObject(prefs(c).getString("extra", "{}")); } catch (JSONException e) { return new JSONObject(); }
    }

    static JSONObject settings(Context c) {
        JSONObject s = extra(c).optJSONObject("settings");
        return s != null ? s : new JSONObject();
    }

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

    /** Widget "are you sure" state: which med's GAVE IT button was first tapped, and when. */
    static long armedAt(Context c, int widgetId, String medId) {
        String v = prefs(c).getString("arm_" + widgetId, "");
        int bar = v.lastIndexOf('|');
        if (bar < 0 || medId == null || !medId.equals(v.substring(0, bar))) return 0;
        try { return Long.parseLong(v.substring(bar + 1)); } catch (NumberFormatException e) { return 0; }
    }

    static void arm(Context c, int widgetId, String medId, long t) {
        prefs(c).edit().putString("arm_" + widgetId, medId == null ? "" : medId + "|" + t).commit();
    }

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
