/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.support.annotation.VisibleForTesting;

import android.util.Log;
import android.view.View;
import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.Toast;
import android.text.TextUtils;

public class KernelVersionDialogController implements View.OnClickListener {

    private static final String TAG = "KernelVersionDialogController";
    private static final int DELAY_TIMER_MILLIS = 500;
    private static final int ACTIVITY_TRIGGER_COUNT = 2;
    private static final int TAPS_TO_START_DEBUGGER = 10;
    private int mDeubggerHitCountdown;
    private Toast mDeubggerDevHitToast;

    static final String ACTION_DEBUGGER = "com.asus.debugger.SETTING_DEBUGGER";

    private final Context mContext;
    private final long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];

    @VisibleForTesting
    static int KERNEL_VERSION_VALUE_ID = R.id.kernel_version_value;

    private final FirmwareVersionDialogFragment mDialog;

    public KernelVersionDialogController(FirmwareVersionDialogFragment dialog) {
        mDialog = dialog;
        mContext = dialog.getContext();
    }

    @Override
    public void onClick(View v) {
        arrayCopy();
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            mDeubggerHitCountdown--;
        } else {
            mDeubggerHitCountdown = TAPS_TO_START_DEBUGGER;
            mDeubggerDevHitToast = null;
        }

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

    /**
     * Updates kernel version to the dialog.
     */
    public void initialize() {
        registerClickListeners();
        mDialog.setText(KERNEL_VERSION_VALUE_ID,
                DeviceInfoUtils.getFormattedKernelVersion(mDialog.getContext()));
        mDeubggerHitCountdown = TAPS_TO_START_DEBUGGER;
        mDeubggerDevHitToast = null;
    }

    private void registerClickListeners() {
        mDialog.registerClickListener(KERNEL_VERSION_VALUE_ID, this /* listener */);
    }

    @VisibleForTesting
    void arrayCopy() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
    }
}
