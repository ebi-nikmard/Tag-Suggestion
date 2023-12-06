package com.tag.tag.configure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class CheckInternetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Core.getInstance().userHasInternet = Core.getInstance().getNetworkHelper().getConnectivityStatusString(context);
        if (!Core.getInstance().userHasInternet) {
            Toast.makeText(context, "You must turn on your Internet!", Toast.LENGTH_SHORT).show();
        }
    }
}