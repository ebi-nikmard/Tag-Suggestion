package com.tag.tag.interfaces;

import cz.msebera.android.httpclient.Header;

public interface TagSuggestionHandler {
    void onStartRequest();
    void onSuccess(int statusCode, Header[] headers, byte[] responseBody);
    void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error);
    void onFinishRequest();

}