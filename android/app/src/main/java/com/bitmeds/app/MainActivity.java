package com.bitmeds.app;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;

/** Shows the bitmeds web app (bundled in assets/www) and bridges it to widgets, alarms and haptics. */
public class MainActivity extends Activity {
    private static WeakReference<MainActivity> live = new WeakReference<>(null);
    private WebView web;
    private boolean resumed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#0b0b16"));
        getWindow().setNavigationBarColor(Color.parseColor("#0b0b16"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0b0b16"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "BitmedsAndroid");
        // keep content clear of the status / navigation bars (edge-to-edge is forced on Android 15)
        web.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom, l, r;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = i.top; bottom = i.bottom; l = i.left; r = i.right;
            } else {
                top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
                l = insets.getSystemWindowInsetLeft(); r = insets.getSystemWindowInsetRight();
            }
            v.setPadding(l, top, r, bottom);
            return insets;
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/www/index.html");
        live = new WeakReference<>(this);

        Alarms.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        web.onResume();
        web.resumeTimers();
        reload();
    }

    @Override protected void onPause() {
        resumed = false;
        web.onPause();
        web.pauseTimers();
        super.onPause();
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    private void reload() { web.evaluateJavascript("window.bitmedsReload && window.bitmedsReload()", null); }

    /** After a widget / notification button changed the data, refresh the open app so it can't save stale data over it. */
    static void reloadIfOpen() {
        MainActivity a = live.get();
        if (a != null && !a.isFinishing()) a.runOnUiThread(a::reload);
    }

    private class Bridge {
        @JavascriptInterface public String load() { return Store.raw(MainActivity.this); }

        @JavascriptInterface public void save(String json) {
            Store.saveRaw(MainActivity.this, json);
            runOnUiThread(() -> {
                MedWidget.updateAll(MainActivity.this);
                Alarms.reschedule(MainActivity.this);
            });
        }

        @JavascriptInterface public void haptic() { Alarms.buzz(MainActivity.this, 40); }

        @JavascriptInterface public boolean pinWidget(String medId) {
            AppWidgetManager mgr = getSystemService(AppWidgetManager.class);
            if (mgr == null || !mgr.isRequestPinAppWidgetSupported()) return false;
            Intent cb = new Intent(MainActivity.this, GiveReceiver.class).setAction(GiveReceiver.ACTION_PINNED).putExtra(GiveReceiver.EXTRA_MED, medId);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 7, cb, flags);
            return mgr.requestPinAppWidget(new ComponentName(MainActivity.this, MedWidget.class), null, pi);
        }

        /** Pin the multi-med widget ("bitmeds · all meds"). */
        @JavascriptInterface public boolean pinListWidget() {
            AppWidgetManager mgr = getSystemService(AppWidgetManager.class);
            if (mgr == null || !mgr.isRequestPinAppWidgetSupported()) return false;
            return mgr.requestPinAppWidget(new ComponentName(MainActivity.this, MedListWidget.class), null, null);
        }
    }
}
