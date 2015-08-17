package com.wolfpakapp.wolfpak2.service;

import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.wolfpakapp.wolfpak2.MainActivity;

/**
 * The LocationProvider gives access to the user's location information.
 */
public class LocationProvider extends ServiceManager
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Context mContext;
    private GoogleApiClient mClient;
    
    private static Location mLastLocation;

    private boolean canObtainLocation = false;

    public LocationProvider(Context context) {
        mContext = context;
        mClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);
        PendingResult<LocationSettingsResult> pendingResult =
                LocationServices.SettingsApi.checkLocationSettings(mClient, builder.build());
        pendingResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS: {
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        canObtainLocation = true;
                        obtainLocation();
                        break;
                    }
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            canObtainLocation = true;
                            status.startResolutionForResult((MainActivity) mContext,
                                    MainActivity.REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    }
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                    }
                }
            }
        });

    }

    /**
     * Signify that the LocationProvider can start getting the last user location. Note that this
     * method will only work under the appropriate situation.
     */
    public void obtainLocation() {
        if (canObtainLocation) {
            //TODO Set a timer to wait for the user location to be obtained (not always instant)
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mClient);
            finishInitialize();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

<<<<<<< HEAD
    public Location getLastLocation() throws NoLocationException {
        if (mLastLocation != null) {
            return mLastLocation;
        }

        toastNoLocation();
        throw new NoLocationException("No location available");
    }

    private boolean isAlerting = false;
    private void toastNoLocation() {
        if (!isAlerting) {
            isAlerting = true;
            Toast.makeText(mContext, "Unable to obtain a location", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isAlerting = false;
                }
            }, 2000);
        }
=======
    public Location getLastLocation() {
        if(mLastLocation == null)   {
            // TODO remove hardcoded values, handle null location
            Location l = new Location("Dummy Provider");
            l.setLatitude(40.3443640);
            l.setLongitude(-74.4646780);
            return l;
        } else {
            return mLastLocation;
        }
>>>>>>> 66c93d8f8ab7994dda2495310cca73d1fb520709
    }

}
