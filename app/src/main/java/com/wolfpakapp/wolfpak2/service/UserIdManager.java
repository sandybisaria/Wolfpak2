package com.wolfpakapp.wolfpak2.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.UUID;

public class UserIdManager extends ServiceManager {

    private String mDeviceId;
    private static final String userIdKey = "userID";

    /**
     * Start the UserIdManager service. Retrieve the device ID from memory or generate one for a
     * first time user.
     * @param context The context of the activity constructing the manager.
     */
    public UserIdManager(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mDeviceId = sharedPreferences.getString(userIdKey, null);
        if (mDeviceId == null) {
            mDeviceId = generateDeviceId(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(userIdKey, mDeviceId);
            editor.apply();
        }

        finishInitialize();
    }

    private String generateDeviceId(Context context) {
        //TODO Update device ID algorithm?
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = manager.getDeviceId();
        String serialNumber = manager.getSimSerialNumber();
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        UUID deviceUUID = new UUID(androidId.hashCode(),
                ((long) deviceId.hashCode() << 32) | serialNumber.hashCode());
        //TODO Remove when done testing!
        return "temp_test_id"; //deviceUUID.toString();
    }

    /**
     * @return The device ID.
     */
    public String getDeviceId() {
        return mDeviceId;
    }
}
