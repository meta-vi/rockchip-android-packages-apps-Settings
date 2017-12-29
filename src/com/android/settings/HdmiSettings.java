package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import android.util.Log;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import javax.xml.transform.Result;

import com.android.settings.widget.SwitchBar;

import android.os.AsyncTask;
import android.os.SystemProperties;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;
import android.content.ContentResolver;
import android.os.Handler;
import android.database.ContentObserver;
import android.os.RemoteException;
import android.os.DisplayOutputManager;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.os.SystemProperties;

import com.android.settings.display.*;

import android.app.DialogFragment;

import com.android.settings.data.ConstData;

import android.hardware.display.DisplayManager;
import android.view.Display;

import android.view.IWindowManager;
import android.view.Surface;
import android.os.ServiceManager;

public class HdmiSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceClickListener {
    /**
     * Called when the activity is first created.
     */
    private static final String TAG = "HdmiSettings";
    private static final String KEY_HDMI_RESOLUTION = "hdmi_resolution";
    private static final String KEY_HDMI_SCALE = "hdmi_screen_zoom";
    private static final String KEY_HDMI_ROTATION="hdmi_rotation";
    private static final String KEY_HDMI_DUAL_SCREEN = "dual_screen_setting";
    private static final String KEY_HDMI_DUAL_SCREEN_VH = "dual_screen_vh";
    private static final String KEY_HDMI_DUAL_SCREEN_LIST = "dual_screen_vhlist";
    private static final String DOUBLE_SCREEN_CONFIG = android.provider.Settings.DUAL_SCREEN_MODE;
    private static final String DOUBLE_SCREEN_STATE = android.provider.Settings.DUAL_SCREEN_ICON_USED;
    // for identify the HdmiFile state
    private boolean IsHdmiConnect = false;
    // for identify the Hdmi connection state
    private boolean IsHdmiPlug = false;
    private boolean IsHdmiDisplayOn = false;

    private CheckBoxPreference mHdmiDualScreen;
    private CheckBoxPreference mHdmiDualScreenVH;
    private ListPreference mHdmiDualScreenList;
    private ListPreference mHdmiResolution;
    private ListPreference mHdmiRotation;
    private Preference mHdmiScale;
    private DisplayOutputManager mDisplayManagement = null;
    private Context context;
    private static final int DEF_HDMI_LCD_TIMEOUT_VALUE = 10;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private SwitchBar mSwitchBar;
    private int mDisplay;
    private String mOldResolution;
    protected String mStrPlatform;
    protected DisplayInfo mDisplayInfo;
    protected boolean mIsUseDisplayd;
    private DisplayManager mDisplayManager;
    private DisplayListener mDisplayListener;
    private IWindowManager wm;

    private final BroadcastReceiver HdmiListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent receivedIt) {
            String action = receivedIt.getAction();
            String HDMIINTENT = "android.intent.action.HDMI_PLUGGED";
            if (action.equals(HDMIINTENT)) {
                boolean state = receivedIt.getBooleanExtra("state", false);
                if (state) {
                    Log.d(TAG, "BroadcastReceiver.onReceive() : Connected HDMI-TV");

                } else {
                    Log.d(TAG, "BroadcastReceiver.onReceive() : Disconnected HDMI-TV");
                }
            }
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter("android.intent.action.HDMI_PLUGGED");
        getContext().registerReceiver(HdmiListener, filter);
        context = getActivity();
        mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        mDisplayListener = new DisplayListener();
        mStrPlatform = SystemProperties.get("ro.board.platform");
        mDisplayInfo = getDisplayInfo();
        addPreferencesFromResource(R.xml.hdmi_settings_timeout);
        mHdmiResolution = (ListPreference) findPreference(KEY_HDMI_RESOLUTION);
        mHdmiResolution.setOnPreferenceChangeListener(this);
        mHdmiResolution.setOnPreferenceClickListener(this);
        if (mDisplayInfo != null) {
            mHdmiResolution.setEntries(DrmDisplaySetting.getDisplayModes(mDisplayInfo).toArray(new String[0]));
            mHdmiResolution.setEntryValues(DrmDisplaySetting.getDisplayModes(mDisplayInfo).toArray(new String[0]));
        }
        mHdmiScale = findPreference(KEY_HDMI_SCALE);
        mHdmiScale.setOnPreferenceClickListener(this);
        mHdmiRotation = (ListPreference) findPreference(KEY_HDMI_ROTATION);
        mHdmiRotation.setOnPreferenceChangeListener(this);
        init();
        boolean enable = android.provider.Settings.System.getInt(getActivity().getContentResolver(),DOUBLE_SCREEN_CONFIG,0) == 1;
        mHdmiDualScreen = (CheckBoxPreference)findPreference(KEY_HDMI_DUAL_SCREEN);
        mHdmiDualScreen.setChecked(enable);
        mHdmiDualScreen.setOnPreferenceChangeListener(this);
 
        mHdmiDualScreenVH = (CheckBoxPreference)findPreference(KEY_HDMI_DUAL_SCREEN_VH);
        mHdmiDualScreenVH.setEnabled(enable);
        if(enable) {
            mHdmiDualScreenVH.setChecked(SystemProperties.getBoolean("persist.orientation.vhshow", false));
        }
        mHdmiDualScreenVH.setOnPreferenceChangeListener(this);
        mHdmiDualScreenList = (ListPreference)findPreference(KEY_HDMI_DUAL_SCREEN_LIST);
        mHdmiDualScreenList.setOnPreferenceChangeListener(this);
        mHdmiDualScreenList.setOnPreferenceClickListener(this);
        mHdmiDualScreenList.setEnabled(SystemProperties.getBoolean("persist.orientation.vhshow", false));
        Log.d(TAG, "onCreate---------------------");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView----------------------------------------");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.show();
        mSwitchBar.addOnSwitchChangeListener(this);
        //restore hdmi switch value
        String switchValue = SystemProperties.get("sys.hdmi_status.aux", "on");
        if (switchValue.equals("on")) {
            mSwitchBar.setChecked(true);
        } else {
            mSwitchBar.setChecked(false);
        }
        //mSwitchBar.setChecked(sharedPreferences.getString("enable", "1").equals("1"));
        //String resolutionValue=sharedPreferences.getString("resolution", "1280x720p-60");
        //Log.d(TAG,"onActivityCreated resolutionValue="+resolutionValue);
        // context.registerReceiver(hdmiReceiver, new IntentFilter("android.intent.action.HDMI_PLUG"));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHDMIState();
        mDisplayManager.registerDisplayListener(mDisplayListener, null);
        if (android.provider.Settings.System.getInt(getActivity().getContentResolver(),DOUBLE_SCREEN_STATE,0) == 0) {
            mHdmiDualScreen.setEnabled(true);
            if(android.provider.Settings.System.getInt(getActivity().getContentResolver(),DOUBLE_SCREEN_CONFIG,0) == 1) {
                mHdmiDualScreenVH.setEnabled(true);
            } else {
                mHdmiDualScreenVH.setEnabled(false);
            }
            mHdmiDualScreenList.setEnabled(SystemProperties.getBoolean("persist.orientation.vhshow", false));
        } else {
            mHdmiDualScreen.setEnabled(false);
            mHdmiDualScreenVH.setEnabled(false);
            mHdmiDualScreenList.setEnabled(false);
        }
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause----------------");
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
    }

    public void onDestroy() {
        mSwitchBar.removeOnSwitchChangeListener(this);
        super.onDestroy();
    }

    private void init() {
        mIsUseDisplayd = SystemProperties.getBoolean("ro.rk.displayd.enable", false);

        //init hdmi rotation
        try {
             wm = IWindowManager.Stub.asInterface(
                  ServiceManager.getService(Context.WINDOW_SERVICE));
             int rotation = wm.getRotation();
             switch (rotation) {
                  case Surface.ROTATION_0:
                       mHdmiRotation.setValue("0");
                       break;
                  case Surface.ROTATION_90:
                       mHdmiRotation.setValue("90");
                       break;
                  case Surface.ROTATION_180:
                       mHdmiRotation.setValue("180");
                       break;
                  case Surface.ROTATION_270:
                       mHdmiRotation.setValue("270");
                       break;
                  default:
                       mHdmiRotation.setValue("0");
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
    }

    protected DisplayInfo getDisplayInfo() {
        DrmDisplaySetting.updateDisplayInfos();
        DisplayInfo mdisplayInfo = DrmDisplaySetting.getHdmiDisplayInfo();

        return mdisplayInfo;
    }

    /**
     * 还原分辨率值
     */
    public void updateResolutionValue() {
        String resolutionValue = null;
        mDisplayInfo = getDisplayInfo();
        if (mDisplayInfo != null)
            resolutionValue = DrmDisplaySetting.getCurDisplayMode(mDisplayInfo);
        Log.i(TAG, "resolutionValue:" + resolutionValue);
        mOldResolution = resolutionValue;
        if (resolutionValue != null)
            mHdmiResolution.setValue(resolutionValue);
    }

    private void updateHDMIState() {
        Display[] allDisplays = mDisplayManager.getDisplays();
        String switchValue = SystemProperties.get("sys.hdmi_status.aux", "on");
        if (allDisplays == null || allDisplays.length < 2 || switchValue.equals("off")) {
            mHdmiResolution.setEnabled(false);
            mHdmiScale.setEnabled(false);
            mHdmiRotation.setEnabled(false);
	        mHdmiDualScreen.setEnabled(false);
	        mHdmiDualScreenVH.setEnabled(false);
	        mHdmiDualScreenList.setEnabled(false);
            SystemProperties.set("persist.orientation.vhshow", "false");
            SystemProperties.set("persist.orientation.vhinit", "0");
            mHdmiDualScreenVH.setChecked(false);
        } else {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    mDisplayInfo = getDisplayInfo();
                    //增加延迟，保证数据能够拿到
                    if (mDisplayInfo != null) {
                        mHdmiResolution.setEntries(DrmDisplaySetting.getDisplayModes(mDisplayInfo).toArray(new String[0]));
                        mHdmiResolution.setEntryValues(DrmDisplaySetting.getDisplayModes(mDisplayInfo).toArray(new String[0]));
                        updateResolutionValue();
                        mHdmiResolution.setEnabled(true);
                        mHdmiScale.setEnabled(true);
                        mHdmiRotation.setEnabled(true);
                        if(getActivity() != null && getActivity().getContentResolver() != null) {
			                if (android.provider.Settings.System.getInt(getActivity().getContentResolver(),DOUBLE_SCREEN_STATE,0) == 0) {
                                mHdmiDualScreen.setEnabled(true);
                                mHdmiDualScreenVH.setEnabled(android.provider.Settings.System.getInt(getActivity().getContentResolver(),DOUBLE_SCREEN_CONFIG,0) == 1);
                                mHdmiDualScreenList.setEnabled(SystemProperties.getBoolean("persist.orientation.vhshow", false));
                    	    }
                        }
                    }

                }
            }, 1000);

        }
    }

    protected void showConfirmSetModeDialog() {
        mDisplayInfo = getDisplayInfo();
        if (mDisplayInfo != null) {
            DialogFragment df = ConfirmSetModeDialogFragment.newInstance(mDisplayInfo, new ConfirmSetModeDialogFragment.OnDialogDismissListener() {
                @Override
                public void onDismiss(boolean isok) {
                    Log.i(TAG, "showConfirmSetModeDialog->onDismiss->isok:" + isok);
                    Log.i(TAG, "showConfirmSetModeDialog->onDismiss->mOldResolution:" + mOldResolution);
                    updateResolutionValue();
                }
            });
            df.show(getFragmentManager(), "ConfirmDialog");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mHdmiScale) {
            Intent screenScaleIntent = new Intent(getActivity(), ScreenScaleActivity.class);
            screenScaleIntent.putExtra(ConstData.IntentKey.PLATFORM, mStrPlatform);
            mDisplayInfo = getDisplayInfo();
            if (mDisplayInfo != null) {
                screenScaleIntent.putExtra(ConstData.IntentKey.DISPLAY_INFO, mDisplayInfo);
                startActivity(screenScaleIntent);
            }
        } else if (preference == mHdmiResolution) {
            updateHDMIState();
        } else if (preference == mHdmiDualScreenList) {
            String value = SystemProperties.get("persist.orientation.vhinit","0");
            Log.v("DualScreenLog"," click  value =  "+value); 
            mHdmiDualScreenList.setValue(value);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Log.i(TAG, "onPreferenceChange:" + obj);
        String key = preference.getKey();
        Log.d(TAG, key);
        if(preference == mHdmiResolution) {
            if (KEY_HDMI_RESOLUTION.equals(key)) {
                if (obj.equals(mOldResolution))
                    return true;
                int index = mHdmiResolution.findIndexOfValue((String) obj);
                Log.i(TAG, "onPreferenceChange: index= " + index);
                mDisplayInfo = getDisplayInfo();
                if (mDisplayInfo != null) {
                    DrmDisplaySetting.setDisplayModeTemp(mDisplayInfo, index);
                    showConfirmSetModeDialog();
                }
            }
         } else if (preference == mHdmiRotation) {
            if (KEY_HDMI_ROTATION.equals(key)) {
                try {
                    int value = Integer.parseInt((String) obj);
                    android.os.SystemProperties.set("persist.sys.orientation", (String) obj);
                    Log.d(TAG,"freezeRotation~~~value:"+(String) obj);
                    if(value == 0)
                        wm.freezeRotation(Surface.ROTATION_0);
                    else if(value == 90)
                        wm.freezeRotation(Surface.ROTATION_90);
                    else if(value == 180)
                        wm.freezeRotation(Surface.ROTATION_180);
                    else if(value == 270)
                        wm.freezeRotation(Surface.ROTATION_270);
                    else
                         return true;
                //android.os.SystemProperties.set("sys.boot_completed", "1");
                } catch (Exception e) {
                      Log.e(TAG, "freezeRotation error");
                }
            }
	    } else if (preference == mHdmiDualScreen) {
            Log.i("DualScreenLog","Hdmi Dual Screen config = "+ (Boolean)obj);
            android.provider.Settings.System.putInt(getActivity().getContentResolver(),DOUBLE_SCREEN_CONFIG,(Boolean)obj?1:0);
            SystemProperties.set("persist.orientation.vhinit", "0");
            SystemProperties.set("persist.orientation.vhshow", "false");
            mHdmiDualScreenVH.setEnabled((Boolean)obj);
            mHdmiDualScreenVH.setChecked(false);
            mHdmiDualScreenList.setEnabled(false);
            this.finish();
	    } else if (preference == mHdmiDualScreenVH) {
           Log.i("DualScreenLog","Hdmi Dual Screen VH config = "+ (Boolean)obj);
            if((Boolean)obj) {
                SystemProperties.set("persist.orientation.vhshow", "true");   
                mHdmiDualScreenList.setEnabled(true);
            } else {
                SystemProperties.set("persist.orientation.vhshow", "false");   
                mHdmiDualScreenList.setEnabled(false);
                SystemProperties.set("persist.orientation.vhinit", "0");
            }
            SystemProperties.set("persist.orientation.vhinit", "0");
	    } else if (preference == mHdmiDualScreenList) {
            if("0".equals(obj.toString())) {
                Log.v("DualScreenLog"," change  obj =  "+obj.toString()+ "     Horizontal"); 
                SystemProperties.set("persist.orientation.vhinit", "0");
            } else if ("1".equals(obj.toString())) {
                Log.v("DualScreenLog"," change  obj =  "+obj.toString()+ "     Verticali"); 
                SystemProperties.set("persist.orientation.vhinit", "1");
            }
        }
        return true;
    }


    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.i(TAG, "onSwitchChanged->isChecked:" + isChecked);
        if (isChecked) {
            //Settings HDMI on
            SystemProperties.set("sys.hdmi_status.aux", "on");
            updateHDMIState();
        } else {
            //Settings HDMI off
            SystemProperties.set("sys.hdmi_status.aux", "off");
            updateHDMIState();
        }
    }

    class DisplayListener implements DisplayManager.DisplayListener {
        @Override
        public void onDisplayAdded(int displayId) {
            updateHDMIState();
        }

        @Override
        public void onDisplayChanged(int displayId) {
            updateHDMIState();
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            updateHDMIState();
        }
    }
}
