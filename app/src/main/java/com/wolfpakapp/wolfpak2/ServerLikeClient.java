package com.wolfpakapp.wolfpak2;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wolfpakapp.wolfpak2.leaderboard.LeaderboardTabAdapter;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

/**
 * The ServerLikeClient provides a way to update the like status of Wolfpak posts.
 */
public class ServerLikeClient {
    private static final String RELATIVE_URL = "like_status/";

    /**
     * Update the like status of the given post. If successful, the view count will update its color
     * and text to reflect the changed status. Otherwise, it will remain unchanged.
     * @param adapter The adapter that contains the post.
     * @param postId The ID of the post.
     * @param voteStatus The VoteStatus to send to the server.
     * @param handler The response handler.
     */
    public static void updateLikeStatus(LeaderboardTabAdapter adapter, int postId,
                                        final Post.VoteStatus voteStatus,
                                        AsyncHttpResponseHandler handler) {
        try {
            Context context = adapter.getParentManager().getParentActivity();

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

            ServerRestClient.post(context, RELATIVE_URL, entity, contentType, handler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
