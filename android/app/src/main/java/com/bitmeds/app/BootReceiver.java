package com.bitmeds.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms are wiped on reboot / app update / clock changes: set them again and redraw widgets. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        Alarms.reschedule(c);
        MedWidget.updateAll(c);
    }
}
