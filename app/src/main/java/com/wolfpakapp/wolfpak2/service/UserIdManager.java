package com.wolfpakapp.wolfpak2.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.util.UUID;

public class UserIdManager extends ServiceManager {

    private String mDeviceId;
    private static final String userIdKey = "userID";
    private static boolean isNewUser = false;

    /**
     * Start the UserIdManager service. Retrieve the device ID from memory or generate one for a
     * first time user.
     * @param context The context of the activity constructing the manager.
     */
    public UserIdManager(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mDeviceId = sharedPreferences.getString(userIdKey, null);
        if (mDeviceId == null) {
            isNewUser = true;
            mDeviceId = generateDeviceId(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(userIdKey, mDeviceId);
            editor.apply();
        } else {
            isNewUser = false;
        }
        Log.d(userIdKey, mDeviceId);

        finishInitialize();
    }

    private String generateDeviceId(Context context) {
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = manager.getDeviceId();
        String serialNumber = manager.getSimSerialNumber();
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        UUID deviceUUID = new UUID(androidId.hashCode(),
                ((long) deviceId.hashCode() << 32) | serialNumber.hashCode());

        return deviceUUID.toString();
    }

    /**
     * @return The device ID.
     */
    public String getDeviceId() {
        if (isNewUser) {
            try {
                ServerRestClient client = (ServerRestClient) WolfpakServiceProvider
                        .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("user_id", mDeviceId);
                StringEntity entity = new StringEntity(jsonObject.toString());
                String contentType = "application/json";
                entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, contentType));
                client.post("users/", entity, contentType, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        isNewUser = false;
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        // Uh-oh
                    }
                });
            } catch (Exception e) {
                // Uh-oh again
                e.printStackTrace();
            }
        }
        return mDeviceId;
    }
}
