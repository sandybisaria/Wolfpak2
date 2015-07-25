package com.wolfpakapp.wolfpak2.service;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.leaderboard.LeaderboardTabAdapter;
import com.wolfpakapp.wolfpak2.service.ServiceManager;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

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

    public ServerRestClient(Context context) {
        mClient = new AsyncHttpClient(true, 80, 443);
        mContext = context;

        finishInitialize();
    }

    /**
     * Make a GET request to the server.
     * @param url The relative URL.
     * @param params The query parameters.
     * @param handler The response handler.
     */
    public void get(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        mClient.get(getAbsoluteUrl(url), params, handler);
    }

    /**
     * Make a POST request to the server.
     * @param url The relative URL.
     * @param entity The raw entity to send with the request.
     * @param contentType The content type of the entity you are sending (e.g. application/json)
     * @param handler The response handler.
     */
    public void post(String url, HttpEntity entity, String contentType, AsyncHttpResponseHandler handler) {
        mClient.post(mContext, getAbsoluteUrl(url), entity, contentType, handler);
    }

    /**
     * Update the like status of the given post.
     * @param postId The ID of the post.
     * @param voteStatus The VoteStatus to send to the server.
     * @param handler The response handler.
     */
    public void updateLikeStatus(int postId, final Post.VoteStatus voteStatus,
                                 AsyncHttpResponseHandler handler) {
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
}
