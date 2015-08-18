package com.wolfpakapp.wolfpak2.mainfeed;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.service.LocationProvider;
import com.wolfpakapp.wolfpak2.service.NoLocationException;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Vishaal on 7/20/15.
 */
public class Networking_MainFeed{

    public int length;
    private MainFeedFragment mainFeed;
    private CustomView_MainFeed customView;

    /** Arrays for JSON Object String **/
    public String[] HowlsURL;
    public String[] HowlsIsImage;
    private String[] HowlsUserID;
    private String[] HowlsPostID;
    private String[] HowlsHandle;
    public String[] HowlsThumbnail;


    /** Location **/
//    public LocationManager lm;
    public Location location;
    private double longitude;
    private double latitude;
    public String deviceId;

    /** Random Number **/
    private String random_string;
    private String random_input;

    public int count;

    public Networking_MainFeed(MainFeedFragment mainFeed){
        this.mainFeed = mainFeed;
        this.customView = new CustomView_MainFeed(mainFeed, this);

        length = 10;
        count = 0;

        HowlsURL = new String[length];
        HowlsIsImage = new String[length];
        HowlsUserID = new String[length];
        HowlsPostID = new String[length];
        HowlsHandle = new String[length];
        HowlsThumbnail = new String[length];
        random_input = "";
    }

    public void initializeQueryString() throws NoLocationException {
        deviceId = ((UserIdManager) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.USERIDMANAGER)).getDeviceId();

        try {
            /** Setting Location for get() query string **/
            location = ((LocationProvider) WolfpakServiceProvider
                    .getServiceManager(WolfpakServiceProvider.LOCATIONPROVIDER)).getLastLocation();
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        } catch (NoLocationException e) {
            // If a location was never set, throw the exception.
            // Otherwise, just use the previously set location
            if (location == null) {
                throw e;
            }
        }

//        if (location == null) {
//            locationCheck();
//        } else {
//            longitude = location.getLongitude();
//            latitude = location.getLatitude();
//        }

//        /** Location Update Detector **/
//        final LocationListener locationListener = new LocationListener() {
//            @Override
//            public void onStatusChanged(String provider, int status, Bundle extras) {
//            }
//
//            @Override
//            public void onProviderEnabled(String provider) {
//            }
//
//            @Override
//            public void onProviderDisabled(String provider) {
//            }
//
//            public void onLocationChanged(Location location) {
//                longitude = location.getLongitude();
//                latitude = location.getLatitude();
//            }
//        };
//        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
    }

    /** Asynchronous HTTP Client - Pull Image/Video from Server **/
    public void getHowls(){
        try {
            // Refresh the query string with the latest location.
            initializeQueryString();
            AsyncHttpClient client = new AsyncHttpClient(true, 80, 443);
            if(location==null){
//            locationCheck();
            }
            else {
                client.get("https://ec2-52-4-176-1.compute-1.amazonaws.com/posts/?user_id=" + "temp_test_id"
                                + "&latitude=" + latitude + "&longitude=" + longitude + "&isNSFW=true&limit=5/",
                        new AsyncHttpResponseHandler() {

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                final JSONArray arr;
                                try {
                                    arr = new JSONArray(new String(response));

                                    mainFeed.getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (int x = 0; x < 5 || x < arr.length(); x++) {
                                                try {
                                                    HowlsURL[x] = arr.getJSONObject(x).optString("media_url");
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                try {
                                                    HowlsIsImage[x] = arr.getJSONObject(x).optString("is_image");
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                try {
                                                    HowlsUserID[x] = arr.getJSONObject(x).optString("user_id");
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                try {
                                                    HowlsPostID[x] = arr.getJSONObject(x).optString("id");
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                try {
                                                    HowlsHandle[x] = arr.getJSONObject(x).optString("handle");
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                try {
                                                    HowlsThumbnail[x] = arr.getJSONObject(x).optString("thumbnail_url");
                                                    Log.d("CHECK",HowlsThumbnail[x]);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            customView.num = arr.length() - 1;
                                            int size = arr.length() - 1;
                                            count = arr.length();

                                            for (int x = size; x > -1; x--) {
                                                customView.loadViews(HowlsIsImage[x], HowlsHandle[x], HowlsURL[x],HowlsThumbnail[x]);
                                            }

                                            if (Objects.equals(HowlsIsImage[0], "false")) {
                                                customView.views[0].mediaVideoView.start();
                                            }
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            }
                        });
            }
        } catch (NoLocationException e) {
            e.printStackTrace();
        }
    }

    /** Asynchronous HTTP Client - Incr/Decr Image/Video in Server **/
    public void incrHowls(int status) {
        AsyncHttpClient client1 = new AsyncHttpClient(true, 80, 443);
        RequestParams params = new RequestParams();
        params.put("post",HowlsPostID[mainFeed.number]);
        params.put("user_liked","temp_test_id");
        params.put("status",status);
        client1.post("https://ec2-52-4-176-1.compute-1.amazonaws.com/like_status/",params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            }
        });
    }

    /** Asynchronous HTTP Client - Reports Image/Video in Server **/
    public void reportHowl(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainFeed.getActivity());
        final EditText random = new EditText(mainFeed.getActivity());
        random.setInputType(InputType.TYPE_CLASS_TEXT);

        // set title
        alertDialogBuilder.setTitle("FLAG!!");
        final EditText input = new EditText(mainFeed.getActivity());

        // set dialog message
        alertDialogBuilder
                .setMessage("Do you really want to report this howl?")
                .setCancelable(false)
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Networking_MainFeed.this.randomstring();
                        AlertDialog.Builder alertDialogBuilder1 = new AlertDialog.Builder(mainFeed.getActivity());
                        // set title
                        alertDialogBuilder1.setTitle("Type Captcha in order to report!");
                        // set dialog message

                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        alertDialogBuilder1.setView(input);

                        final AsyncHttpClient reportput = new AsyncHttpClient(true, 80, 443);

                        alertDialogBuilder1
                                .setMessage("CAPTCHA = " + random_string)
                                .setCancelable(false)
                                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog1, int id1) {
                                        random_input = input.getText().toString();
                                        if (Objects.equals(random_string, random_input)) {
                                            reportput.put("https://ec2-52-4-176-1.compute-1.amazonaws.com/posts/flag/" + HowlsPostID[mainFeed.number] + "/", new AsyncHttpResponseHandler() {
                                                @Override
                                                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                                }

                                                @Override
                                                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                                }
                                            });
                                        } else {
                                            dialog1.cancel();
                                        }

                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog1, int id1) {
                                        // if this button is clicked, just close
                                        // the dialog box and do nothing
                                        dialog1.cancel();
                                    }
                                });

                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder1.create();

                        // show it
                        alertDialog.show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    /** Random Number Generator for reportHowl() **/
    public void randomstring(){
        char[] chars1 = "ABCDEF012GHIJKL345MNOPQR678STUVWXYZ9".toCharArray();
        StringBuilder sb1 = new StringBuilder();
        Random random1 = new Random();
        for (int i = 0; i < 8; i++) {
            char c1 = chars1[random1.nextInt(chars1.length)];
            sb1.append(c1);
        }
        random_string = sb1.toString();
    }

    @Deprecated
    public void locationCheck(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainFeed.getActivity());
        alertDialogBuilder.setTitle("Please turn on your location services!");
        alertDialogBuilder
                .setMessage("CAPTCHA = " + random_string)
                .setCancelable(false)
                .setNeutralButton("OK!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
}
