package com.bitmeds.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (Alarms.ACTION_TICK.equals(intent.getAction())) { MedWidget.updateAll(c); Alarms.scheduleTick(c); return; }
        Alarms.fire(c);
    }
}
