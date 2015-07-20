package com.wolfpakapp.wolfpak2;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

/**
 * The ServerRestClient provides a way to interact with the Wolfpak server using GET and POST
 * requests.
 */
public class ServerRestClient {
    private static final String BASE_URL = "https://ec2-52-4-176-1.compute-1.amazonaws.com/";

    private static AsyncHttpClient client = new AsyncHttpClient(true, 80, 443);

    /**
     * Make a GET request to the server.
     * @param url The relative URL.
     * @param params The query parameters.
     * @param handler The response handler.
     */
    public static void get(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        client.get(getAbsoluteUrl(url), params, handler);
    }

    /**
     * Make a POST request to the server.
     * @param context The Context which made the request.
     * @param url The relative URL.
     * @param entity The raw entity to send with the request.
     * @param contentType The content type of the entity you are sending (e.g. application/json)
     * @param handler The response handler.
     */
    public static void post(Context context, String url, HttpEntity entity, String contentType, AsyncHttpResponseHandler handler) {
        client.post(context, getAbsoluteUrl(url), entity, contentType, handler);
    }

    /**
     * @param relativeUrl The relative URL.
     * @return The absolute URL that will be used to access the server.
     */
    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
