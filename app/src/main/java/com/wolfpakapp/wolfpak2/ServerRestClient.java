package com.wolfpakapp.wolfpak2;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ServerRestClient {
    private static final String BASE_URL = "https://ec2-52-4-176-1.compute-1.amazonaws.com/";

    private static AsyncHttpClient client = new AsyncHttpClient(true, 80, 443);

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        client.get(getAbsoluteUrl(url), params, handler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        client.post(getAbsoluteUrl(url), params, handler);
    }

    public static void put(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        client.put(getAbsoluteUrl(url), params, handler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
