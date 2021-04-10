package com.chibox.wellness.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenOffAdminReceiver extends DeviceAdminReceiver {

    public void onDisabled(Context paramContext, Intent paramIntent) {
        super.onEnabled(paramContext, paramIntent);
    }

    public void onEnabled(Context paramContext, Intent paramIntent) {
        super.onDisabled(paramContext, paramIntent);
    }
}