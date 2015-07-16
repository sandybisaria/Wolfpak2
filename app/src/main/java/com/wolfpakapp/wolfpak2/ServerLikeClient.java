package com.wolfpakapp.wolfpak2;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

public class ServerLikeClient {
    private static final String RELATIVE_URL = "like_status/";

    public static void updateLikeStatus(LeaderboardTabAdapter adapter,
                                        final LeaderboardTabAdapter.ViewHolder viewHolder,
                                        int postId,
                                        final LeaderboardListItem.VoteStatus voteStatus) {
        try {
            Context context = adapter.getParentManager().getParentActivity();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("post", postId);
            //TODO Get actual user ID!
            jsonObject.put("user_liked", "temp_test_id");
            jsonObject.put("status", voteStatus.change);
            StringEntity entity = new StringEntity(jsonObject.toString());
            String contentType = "application/json";
            entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, contentType));

            ServerRestClient.post(context, RELATIVE_URL, entity, contentType, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    viewHolder.getListItem().setVoteStatus(voteStatus);
                    viewHolder.updateViewCountBackground(voteStatus);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    viewHolder.updateViewCountBackground(viewHolder.getListItem().getVoteStatus());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
