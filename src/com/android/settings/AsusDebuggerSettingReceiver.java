package com.android.settings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;


public class AsusDebuggerSettingReceiver extends BroadcastReceiver {

    public AsusDebuggerSettingReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals("com.asus.debugger.intent_action.bugreport")) {
            try {
                ActivityManager.getService().requestFullBugReport();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
