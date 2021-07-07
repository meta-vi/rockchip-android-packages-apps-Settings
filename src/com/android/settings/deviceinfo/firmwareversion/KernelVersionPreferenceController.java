/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.app.ActivityManager;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.DeviceInfoUtils;

import android.os.SystemClock;
import android.os.RemoteException;
import android.text.TextUtils;
import androidx.preference.Preference;
import android.widget.Toast;
import android.content.BroadcastReceiver;


public class KernelVersionPreferenceController extends BasePreferenceController implements LifecycleObserver, OnResume {

    static final int TAPS_TO_START_DEBUGGER = 10;
    static final String ACTION_DEBUGGER = "com.asus.debugger.SETTING_DEBUGGER";

    private long mDebuggerHitTime;
    private int mDeubggerHitCountdown;
    private Toast mDeubggerDevHitToast;

    public KernelVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return DeviceInfoUtils.getFormattedKernelVersion(mContext);
    }

    @Override
    public void onResume() {
        mDeubggerHitCountdown = TAPS_TO_START_DEBUGGER;
        mDeubggerDevHitToast = null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        if(mDeubggerHitCountdown == TAPS_TO_START_DEBUGGER) {
            mDebuggerHitTime = SystemClock.uptimeMillis();
        } else if(SystemClock.uptimeMillis() - mDebuggerHitTime > 500) {
            mDeubggerHitCountdown = TAPS_TO_START_DEBUGGER;
            return true;
        } else {
            mDebuggerHitTime = SystemClock.uptimeMillis();
        }

        if (mDeubggerHitCountdown > 0) {
            mDeubggerHitCountdown--;
            if (mDeubggerHitCountdown == 0) {
                try {
                    Intent intent = new Intent(ACTION_DEBUGGER);
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                if (mDeubggerDevHitToast != null) {
                    mDeubggerDevHitToast.cancel();
                }
                mDeubggerHitCountdown = TAPS_TO_START_DEBUGGER;

            } else if (mDeubggerHitCountdown > 0
                    && mDeubggerHitCountdown < (TAPS_TO_START_DEBUGGER-2)) {
                if (mDeubggerDevHitToast != null) {
                    mDeubggerDevHitToast.cancel();
                }
                mDeubggerDevHitToast = Toast.makeText(mContext, "Remaining " + mDeubggerHitCountdown,Toast.LENGTH_SHORT);
                mDeubggerDevHitToast.show();
            }
        }
        return true;
    }
}
