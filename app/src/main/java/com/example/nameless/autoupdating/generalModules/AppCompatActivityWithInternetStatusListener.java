package com.example.nameless.autoupdating.generalModules;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.nameless.autoupdating.receivers.NetworkStateReceiver;

/**
 * Created by nameless on 23.06.18.
 */

public class AppCompatActivityWithInternetStatusListener extends AppCompatActivity  implements NetworkStateReceiver.NetworkStateReceiverListener {
    private NetworkStateReceiver networkStateReceiver;
    private boolean isDisconnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_auth);

        isDisconnected = false;
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void networkAvailable() {
        if(isDisconnected) {
            isDisconnected = false;
            recreate();
        }
    }

    @Override
    public void networkUnavailable() {
        isDisconnected = true;
    }

    @Override
    protected void onDestroy() {
        isDisconnected = false;
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
        super.onDestroy();
    }
}
