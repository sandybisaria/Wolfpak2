package com.wolfpakapp.wolfpak2.service;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.UUID;

public class UserIdManager extends ServiceManager {

    private String mDeviceId;

    /**
     * Start the UserIdManager service. Retrieve the device ID from memory or generate one for a
     * first time user.
     * @param context The context of the activity constructing the manager.
     */
    public UserIdManager(Context context) {
        //TODO Store on the phone so you don't have to generate it every time!
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = manager.getDeviceId();
        String serialNumber = manager.getSimSerialNumber();
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        UUID deviceUUID = new UUID(androidId.hashCode(),
                ((long) deviceId.hashCode() << 32) | serialNumber.hashCode());
        mDeviceId = deviceUUID.toString();

        //TODO Remove eventually...
        mDeviceId = "temp_test_id";

        isInitialized = true;
    }

    /**
     * @return The device ID.
     */
    public String getDeviceId() {
        return mDeviceId;
    }
}
