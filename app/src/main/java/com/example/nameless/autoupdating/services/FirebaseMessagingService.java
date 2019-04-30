package com.example.nameless.autoupdating.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Chat;
import com.example.nameless.autoupdating.activities.Settings;
import com.example.nameless.autoupdating.activities.VoiceCalling;
import com.example.nameless.autoupdating.models.Message;
import com.example.nameless.autoupdating.models.User;
import com.example.nameless.autoupdating.common.FCMManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by nameless on 06.04.18.
 */

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private final String NOTIFY_CHANNEL_NAME = "NOTIFY_CHANNEL_ID";
    private final String DEFAULT_SOUND_CHANNEL = "DEFAULT_SOUND_CHANNEL";
    private final String CUSTOM_SOUND_CHANNEL = "CUSTOM_SOUND_CHANNEL";

    private final String NOTIFICATION_TAG = "NOTIFICATION_TAG";
    private final String VOICE_CALLING_TAG = "VOICE_CALLING_TAG";

    private Uri notifySoundUri;
    private HashMap<String, Integer> usersId;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        notifySoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getApplicationContext().getPackageName() + File.separator + R.raw.notify);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        usersId = new HashMap<>();

        if (remoteMessage.getData().size() > 0) {
            switch(remoteMessage.getData().get("tag")) {

                case NOTIFICATION_TAG: {
                    Message msg = new Message();
                    msg.setContent(remoteMessage.getData().get("content"));

                    Gson gson = new GsonBuilder().create();
                    String json = remoteMessage.getData().get("sender");
                    User sender = gson.fromJson(json, User.class);

                    if (!sender.getUid().equals(mAuth.getUid()) &&
                            !sender.getUid().equals(FCMManager.getInterlocutor())) {
                        sendNotify(msg, sender);
                    }
                    break;
                }

                case VOICE_CALLING_TAG: {
                    openCallingActivity(remoteMessage.getData().get("privateRoomPort"));
                    break;
                }
            }
        } else {
            sendBroadcastNotification(remoteMessage);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        FCMManager.sendTokenToServer(token);
    }


    private void sendBroadcastNotification(RemoteMessage remoteMessage) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setContentText(remoteMessage.getNotification().getBody()).build();
        notificationManager.notify(1, notification);
    }

    private void openCallingActivity(String privateRoomPort) {
        Intent intent = new Intent(getApplicationContext(), VoiceCalling.class);
        intent.putExtra("action", VoiceCalling.INCOMING_CALL_ACTION);
        intent.putExtra("privateRoomPort", privateRoomPort);
        startActivity(intent);
    }

    private void sendNotify(Message msg, User user) {
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
        if(user.getAvatarUrl() != null) {
            try {
                largeIconBitmap = Glide
                    .with(getApplicationContext())
                    .asBitmap()
                    .load(user.getAvatarUrl())
                    .into(96, 96)
                    .get();

                buildNotify(user, msg, largeIconBitmap);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        buildNotify(user, msg, largeIconBitmap);
    }

    private void buildNotify(User sender, Message msg, Bitmap senderAvatar) {
        String messageContent = msg.getContent();
        if (msg.getFileType() != null && messageContent.length() == 0) {
            messageContent = msg.getFileType();
        }

        if(usersId.get(sender.getUid()) == null) {
            usersId.put(sender.getUid(), usersId.size() + 1);
        }
        SharedPreferences settings = getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean isSoundEnabled = settings.getBoolean(Settings.IS_NOTIFY_ENABLED, false);
        String channelId = isSoundEnabled ? CUSTOM_SOUND_CHANNEL : DEFAULT_SOUND_CHANNEL;

        Intent notificationIntent = new Intent(getApplicationContext(), Chat.class);
        notificationIntent.putExtra("to", sender);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        // Impossible to change exists channel (can not change channel sound) and then i create second channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, NOTIFY_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            if(isSoundEnabled) {
                mChannel.setSound(notifySoundUri, audioAttributes);
            }
            notificationManager.createNotificationChannel(mChannel);
            builder.setChannelId(channelId);
        }

        builder .setContentTitle(sender.getLogin())
                .setContentText(messageContent)
                .setStyle(new Notification.BigTextStyle().bigText(messageContent))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setLargeIcon(senderAvatar)
                .setShowWhen(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setWhen(Calendar.getInstance().getTimeInMillis())
                .setSmallIcon(R.drawable.send2)
                .setPriority(Notification.PRIORITY_HIGH);

        if(isSoundEnabled) {
            builder.setSound(notifySoundUri);
        } else {
            builder.setDefaults(Notification.DEFAULT_SOUND);
        }

//        builder.setFullScreenIntent(intent, true);
        Notification notification = builder.build();
//        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_INSISTENT;
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(usersId.get(sender.getUid()) , notification);
    }
}
