package com.wolfpakapp.wolfpak2.mainfeed;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class APIHelper {
    private static final int HTTP_PORT = 40;
    private static final int HTTPS_PORT = 443;

    private static final String BASE_URL = "https://ec2-52-4-176-1.compute-1.amazonaws.com/";
    private static final AsyncHttpClient CLIENT = new AsyncHttpClient(true, HTTP_PORT, HTTPS_PORT);

    /**
     * All valid url extensions to the server
     */
    public enum UrlExtension {
        /**
         * Valid HttpRequests: GET, POST[user_id]
         */
        users("users/"),

        /**
         * Valid HttpRequests: GET[user_id]
         */
        userDen("users/den/"), // GET

        /**
         * Valid HttpRequests: POST[post, user_liked, status]
         */
        likeStatus("like_status/"), // POST

        /**
         * Valid HttpRequests: GET[user_id, latitude, longitude, is_nsfw]
         */
        localLeaderboard("posts/local_leaderboard/"),

        /**
         * Valid HttpRequests: GET
         */
        allTimeLeaderboard("posts/all_time_leaderboard/"),

        /**
         * Valid HttpRequests:
         * GET[user_id, latitude, longitude, is_nsfw],
         * POST[media, handle, latitude, longitude, nsfw, is_image, user]
         */
        posts("posts/");

        private final String value;

        private UrlExtension(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    /**
     * Sends a HTTP GET request to the specified url extension, which then fires the input handler
     *
     * @param urlExtension url to send GET request to
     * @param requestParams parameters required for GET request to the specified url
     * @param handler block that fires after GET request succeeds or fails
     */
    public static void GET(UrlExtension urlExtension, RequestParams requestParams, AsyncHttpResponseHandler handler) {
        CLIENT.get(getFullUrl(urlExtension), requestParams, handler);
    }

    /**
     * Sends a HTTP POST request to the specified url extension, which then fires the input handler
     *
     * @param urlExtension url to send POST request to
     * @param requestParams parameters required for POST request to the specified url
     * @param handler block that fires after POST request succeeds or fails
     */
    public static void POST(UrlExtension urlExtension, RequestParams requestParams, AsyncHttpResponseHandler handler) {
        CLIENT.post(getFullUrl(urlExtension), requestParams, handler);
    }

    /**
     * @param urlExtension url extension of the BASE_URL
     *
     * @return full, valid url
     */
    private static String getFullUrl(UrlExtension urlExtension) {
        return BASE_URL + urlExtension.toString();
    }
}
