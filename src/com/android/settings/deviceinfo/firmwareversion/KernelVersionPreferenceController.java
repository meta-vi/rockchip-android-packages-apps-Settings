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
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.DeviceInfoUtils;


public class KernelVersionPreferenceController extends BasePreferenceController {

    private static final String TAG = "KernelVersionCtrl";

    private static final int DELAY_TIMER_MILLIS = 500;
    private static final int ACTIVITY_TRIGGER_COUNT = 3;

    private final long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];

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
    public boolean handlePreferenceTreeClick(Preference preference) {

        Log.e(TAG, "handlePreferenceTreeClick");
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        Log.e(TAG, "handlePreferenceTreeClick2");
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        arrayCopy();
        Log.e(TAG, "handlePreferenceTreeClick3");
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();

        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            try {
                String value = android.os.SystemProperties.get("ro.boot.deviceid", "");
                if (TextUtils.isEmpty(value) || "none".equals(value)) {
                    Toast.makeText(mContext, R.string.attestation_device_id_failed, Toast.LENGTH_SHORT).show();
                } else if ("success".equals(value)) {
                    Toast.makeText(mContext, R.string.attestation_device_id_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, value, Toast.LENGTH_SHORT).show();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return  true;
    }
    void arrayCopy() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
    }
}
