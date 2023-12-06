package com.tag.tag.configure;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.tag.tag.interfaces.TagSuggestionHandler;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;

import cz.msebera.android.httpclient.Header;

public class NetworkHelper {
    public final String MAIN_API = "https://api.imagga.com";
    private final String MAIN_API_URL = MAIN_API + "/v2/";
    public NetworkHelper() throws GeneralSecurityException {
    }

    public boolean getConnectivityStatusString(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            } else return activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            return false;
        }
    }

    public void tagSuggestionRequest(TagSuggestionHandler requestHandler, File attach) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        try {
            params.put("image", attach);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        params.put("limit", 20);
        String credentialsToEncode = "acc_69d07f037ac30c0" + ":" + "91735189f271a6a94dd48598aa36784a";
        client.addHeader("Authorization", "Basic " + android.util.Base64.encodeToString(credentialsToEncode.getBytes(), android.util.Base64.DEFAULT));
        client.post(MAIN_API_URL + "tags", params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                requestHandler.onStartRequest();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                requestHandler.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                requestHandler.onFailure(statusCode, headers, errorResponse, e);
            }

            @Override
            public void onFinish() {
                requestHandler.onFinishRequest();
            }
        });
    }
}
