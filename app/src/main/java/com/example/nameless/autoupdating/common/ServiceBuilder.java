package com.example.nameless.autoupdating.common;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceBuilder {

    public static void startService(Context context, Class serviceType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, serviceType));
        } else {
            context.startService(new Intent(context, serviceType));
        }
    }

    public static void startService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context, Class serviceType) {
        context.stopService(new Intent(context, serviceType));
    }
}
