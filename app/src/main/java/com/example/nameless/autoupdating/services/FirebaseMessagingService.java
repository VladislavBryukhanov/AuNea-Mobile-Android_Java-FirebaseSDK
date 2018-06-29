package com.example.nameless.autoupdating.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.net.Uri;

import com.example.nameless.autoupdating.R;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by nameless on 06.04.18.
 */

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(getApplicationContext())
                        .setSound(Uri.parse("android.resource://" + this.getApplicationContext()
                        .getPackageName() + "/" + R.raw.notify))
                        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                        .setContentTitle(remoteMessage.getNotification().getTitle())
                        .setContentText(remoteMessage.getNotification().getBody()).build();
        notificationManager.notify(1, notification);
//
//
//        notification = new Notification.Builder(getApplicationContext())
//                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
//                .setContentTitle("From")
//                .setContentText(remoteMessage.getFrom()).build();
//        notificationManager.notify(2, notification);
    }
}
