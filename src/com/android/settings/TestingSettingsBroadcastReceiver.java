package com.android.settings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.os.RemoteException;

import com.android.settings.Settings.TestingSettingsActivity;


public class TestingSettingsBroadcastReceiver extends BroadcastReceiver {

    public TestingSettingsBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_SECRET_CODE)) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClass(context, TestingSettingsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if(intent.getAction().equals("com.asus.debugger.intent_action.bugreport")) {
            try {
                ActivityManager.getService().requestFullBugReport();
            } catch (RemoteException e) {
                e.printStackTrace();
                //Log.e(TAG, "error taking bugreport (bugreportType=Full)", e);
            }
        }
    }
}
