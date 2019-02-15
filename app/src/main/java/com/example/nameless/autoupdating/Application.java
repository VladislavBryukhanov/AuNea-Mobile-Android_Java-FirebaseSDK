package com.example.nameless.autoupdating;

import android.app.Activity;
import android.os.Bundle;

import com.example.nameless.autoupdating.common.NetworkUtil;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new AppLifecycleTracker());
    }
}

class AppLifecycleTracker implements android.app.Application.ActivityLifecycleCallbacks {

    private int created = 0;
    private int numStarted = 0;

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        created++;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // App in foreground
        if (numStarted == 0) {
            NetworkUtil.setOnlineStatus();
        }
        numStarted++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // App in background
        numStarted--;
        if (numStarted == 0) {
            NetworkUtil.setAfkStatus();
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // App closed
        created--;
        if (created == 0) {
            NetworkUtil.setOfflineStatus();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

}