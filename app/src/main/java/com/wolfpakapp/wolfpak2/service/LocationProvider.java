package com.wolfpakapp.wolfpak2.service;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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

    public void reconnect() {
        if (!mClient.isConnecting() && !mClient.isConnected()) {
            mClient.connect();
        }
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
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mClient);

            // Set up a recurring location request to update the location.
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            int fastestInterval = 1000 * 60; // 1 minute in milliseconds
            locationRequest.setFastestInterval(fastestInterval);
            int interval = 1000 * 60 * 30; // 30 minutes in milliseconds
            locationRequest.setInterval(interval);
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient, locationRequest,
                    new com.google.android.gms.location.LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            mLastLocation = location;
                        }
                    });

            finishInitialize();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    public static boolean isResolvingError = false;

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (isResolvingError) {
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                isResolvingError = true;
                connectionResult.startResolutionForResult((MainActivity) mContext,
                        MainActivity.REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mClient.connect();
            }
        } else {
            showErrorDialog(connectionResult.getErrorCode());
            isResolvingError = true;
        }

    }

    private static final String DIALOG_ERROR = "dialog_error";

    private void showErrorDialog(int errorCode) {
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(((MainActivity) mContext).getSupportFragmentManager(), "errordialog");
    }

    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, MainActivity.REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            isResolvingError = false;
        }
    }

    public Location getLastLocation() throws NoLocationException {
        if (mLastLocation != null) {
            return mLastLocation;
        }

        toastNoLocation();
        throw new NoLocationException("No location available");
    }

    private static boolean isAlerting = false;

    /**
     * Toast that there is no location available.
     */
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
    }

}
