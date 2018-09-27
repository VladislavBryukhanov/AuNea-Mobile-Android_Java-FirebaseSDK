package com.example.nameless.autoupdating.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.nameless.autoupdating.services.CallService;
import com.example.nameless.autoupdating.services.NotifyService;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by nameless on 09.08.18.
 */

public class StartMyServiceAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (FirebaseAuth.getInstance().getUid() != null) {
            context.startService(new Intent(context, NotifyService.class));
            context.startService(new Intent(context, CallService.class));
        }
    }
}