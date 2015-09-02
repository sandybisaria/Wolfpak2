package com.wolfpakapp.wolfpak2.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.VoteStatus;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

/**
 * The ServerRestClient provides a way to interact with the Wolfpak server using GET and POST
 * requests.
 */
public class ServerRestClient extends ServiceManager {
    private final String BASE_URL = "https://ec2-52-4-176-1.compute-1.amazonaws.com/";

    private AsyncHttpClient mClient;
    private Context mContext;

    /**
     * Start the ServerRestClient service.
     *
     * @param context The context of the activity constructing the manager.
     */
    public ServerRestClient(Context context) {
        mContext = context;
        mClient = new AsyncHttpClient(true, 80, 443);

        finishInitialize();
    }

    /**
     * Make a GET request to the server.
     *
     * @param url     The relative URL.
     * @param params  The query parameters.
     * @param handler The response handler.
     */
    public void get(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        if (!checkInternetConnection()) {
            toastNoConnection();
            handler.onFailure(0, null, null, null);
            return;
        }
        mClient.get(getAbsoluteUrl(url), params, handler);
    }

    /**
     * Make a POST request to the server.
     *
     * @param url         The relative URL.
     * @param entity      The raw entity to send with the request.
     * @param contentType The content type of the entity you are sending (e.g. application/json)
     * @param handler     The response handler.
     */
    public void post(String url, HttpEntity entity, String contentType, AsyncHttpResponseHandler handler) {
        if (!checkInternetConnection()) {
            toastNoConnection();
            handler.onFailure(0, null, null, null);
            return;
        }
        mClient.post(mContext, getAbsoluteUrl(url), entity, contentType, handler);
    }

    /**
     * Make a PUT request to the server.
     *
     * @param url     The relative URL.
     * @param params  The query parameters.
     * @param handler The response handler.
     */
    public void put(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        if (!checkInternetConnection()) {
            toastNoConnection();
            handler.onFailure(0, null, null, null);
            return;
        }
        mClient.put(mContext, getAbsoluteUrl(url), params, handler);
    }

    /**
     * Update the like status of the given post.
     *
     * @param postId     The ID of the post.
     * @param voteStatus The VoteStatus to send to the server.
     * @param handler    The response handler.
     */
    public void updateLikeStatus(int postId, final VoteStatus voteStatus,
                                 AsyncHttpResponseHandler handler) {
        if (!checkInternetConnection()) {
            toastNoConnection();
            handler.onFailure(0, null, null, null);
            return;
        }
        final String likeStatusUrl = "like_status/";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("post", postId);
            UserIdManager userIdManager = (UserIdManager) WolfpakServiceProvider
                    .getServiceManager(WolfpakServiceProvider.USERIDMANAGER);
            String userId = userIdManager.getDeviceId();
            jsonObject.put("user_liked", userId);
            jsonObject.put("status", voteStatus.change);
            StringEntity entity = new StringEntity(jsonObject.toString());
            String contentType = "application/json";
            entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, contentType));

            post(likeStatusUrl, entity, contentType, handler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param relativeUrl The relative URL.
     * @return The absolute URL that will be used to access the server.
     */
    private String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

    private static boolean isAlerting = false;

    /**
     * Toast that there is no location available.
     */
    private void toastNoConnection() {
        if (!isAlerting) {
            isAlerting = true;
            Toast.makeText(mContext, "Unable to connect to the Internet", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isAlerting = false;
                }
            }, 2000);
        }
    }

    /**
     * @return True if connected to the Internet (via Wi-Fi, cellular, etc.)
     */
    public boolean checkInternetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
