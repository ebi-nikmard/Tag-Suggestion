/*
 * The `ParentActivity` class serves as the base class for activities within the Tag application.
 * It extends AppCompatActivity and provides common functionality for handling internet connectivity status.
 * Subclasses are expected to implement the abstract methods `setView()` and `onClick()` to initialize views and handle click events, respectively.
 */

package com.tag.tag.project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.tag.tag.configure.Core;

import androidx.appcompat.app.AppCompatActivity;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public abstract class ParentActivity extends AppCompatActivity {

    // Abstract methods to be implemented by subclasses
    public abstract void setView();

    public abstract void onClick();

    @Override
    protected void onResume() {
        // Register the network status receiver when the activity is resumed
        registerInternetStatusBroadcast(Core.getInstance().networkStatusReceiver);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Unregister the network status receiver when the activity is paused
        unregisterInternetStatusBroadcast(Core.getInstance().networkStatusReceiver);
        super.onPause();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Wrap the base context with ViewPumpContextWrapper for custom font support
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    // Method to register the network status receiver
    public void registerInternetStatusBroadcast(BroadcastReceiver receiver) {
        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    // Method to unregister the network status receiver
    public void unregisterInternetStatusBroadcast(BroadcastReceiver receiver) {
        unregisterReceiver(receiver);
    }
}
