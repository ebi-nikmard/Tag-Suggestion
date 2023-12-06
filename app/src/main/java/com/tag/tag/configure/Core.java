package com.tag.tag.configure;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;

import com.tag.tag.R;
import com.tag.tag.project.tag.model.TagModel;

import java.security.GeneralSecurityException;
import java.util.ArrayList;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;

public class Core extends Application {
    public final String TAG = "ebrahim";
    public static Context mContext;
    private static Core instance;
    public boolean userHasInternet = false;
    public ArrayList<TagModel> imageTags = new ArrayList<>();
    public BroadcastReceiver networkStatusReceiver = null;
    private NetworkHelper networkHelper;

    public static Core getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        mContext = getApplicationContext();

        try {
            this.networkHelper = new NetworkHelper();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        Core.getInstance().networkStatusReceiver = new CheckInternetReceiver();

        setFont();
    }

    public void setFont() {
        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/Shabnam.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());
    }

    public NetworkHelper getNetworkHelper() {
        return networkHelper;
    }
}
