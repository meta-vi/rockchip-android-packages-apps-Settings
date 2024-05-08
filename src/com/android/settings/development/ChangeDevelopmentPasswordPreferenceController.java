package com.android.settings.development;

import android.content.Context;
import android.os.UserManager;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ChangeDevelopmentPasswordPreferenceController extends DeveloperOptionsPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_CHANGEDEVELOPMENTPASSWORD = "changedevelopmentpassword";

    public ChangeDevelopmentPasswordPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CHANGEDEVELOPMENTPASSWORD;
    }
}


