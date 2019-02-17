package com.example.nameless.autoupdating.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.nameless.autoupdating.common.FirebaseSingleton;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Chat;
import com.example.nameless.autoupdating.activities.Settings;
import com.example.nameless.autoupdating.models.Message;
import com.example.nameless.autoupdating.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by nameless on 11.04.18.
 */

public class NotifyService extends Service {

    private final String NOTIFY_CHANNEL_NAME = "NOTIFY_CHANNEL_ID";
    private final String DEFAULT_SOUND_CHANNEL = "DEFAULT_SOUND_CHANNEL";
    private final String CUSTOM_SOUND_CHANNEL = "CUSTOM_SOUND_CHANNEL";

    private Uri notifySoundUri;

    private FirebaseDatabase database;
    private Query dbMyDialogs;
    private ValueEventListener dialogsListener;
    private FirebaseAuth mAuth;

    private HashMap<String, Integer> usersId;

    private String interlocutor; //собеседник


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        notifySoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getApplicationContext().getPackageName() + File.separator + R.raw.notify);

        database = FirebaseSingleton.getFirebaseInstanse();
        mAuth = FirebaseAuth.getInstance();
        usersId = new HashMap<>();

        interlocutor = "";
        createListeners();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            interlocutor = intent.getStringExtra("dialog");
        }
//        createListeners();

//        getMissedNotifications();
//        disconnectTime = new Date();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        dbMyDialogs.removeEventListener(dialogsListener);
//        disconnectTime = new Date();

//        Intent broadcastIntent = new Intent("android.intent.action.RestartNotificationService");
//        sendBroadcast(broadcastIntent);
    }

    private void createListeners() {
        dbMyDialogs = database.getReference("Dialogs").orderByChild("speakers/" + mAuth.getUid()).equalTo(mAuth.getUid());
        dbMyDialogs.addValueEventListener(dialogsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot dlg : dataSnapshot.getChildren()) {
                    Message lastMessage = dlg.child("lastMessage").getValue(Message.class);
                    String sender = lastMessage.getWho();
                    if (!lastMessage.isRead() && dlg.child("notify").getValue(Boolean.class)) {
                        if (!sender.equals(interlocutor) && (lastMessage.getTo()).equals(mAuth.getUid())) {
                            sendNotify(lastMessage);
                            dlg.child("notify").getRef().setValue(false);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void sendNotify(final Message msg) {
        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(msg.getWho());
        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    final User user = data.getValue(User.class);
                    if(user.getAvatarUrl() != null) {
                        Glide
                            .with(getApplicationContext())
                            .asBitmap()
                            .load(user.getAvatarUrl())
                            .into(new SimpleTarget<Bitmap>(96, 96) {
                                @Override
                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                    buildNotify(user, msg, resource);
                                }
                            });

                    } else {
                        BitmapDrawable bitmapDrawable = (BitmapDrawable)getDrawable(R.drawable.avatar);
                        Bitmap largeIconBitmap = bitmapDrawable.getBitmap();
                        buildNotify(user, msg, largeIconBitmap);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void buildNotify(User who, Message msg, Bitmap largeIconBitmap) {
        if(usersId.get(who.getUid()) == null) {
            usersId.put(who.getUid(), usersId.size() + 1);
        }
        SharedPreferences settings = getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean isSoundEnabled = settings.getBoolean(Settings.IS_NOTIFY_ENABLED, false);
        String channelId = isSoundEnabled ? CUSTOM_SOUND_CHANNEL : DEFAULT_SOUND_CHANNEL;

        Intent notificationIntent = new Intent(getApplicationContext(), Chat.class);
        notificationIntent.putExtra("to", who);
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

        builder .setContentTitle(who.getLogin())
                .setContentText(msg.getContent())
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setLargeIcon(largeIconBitmap)
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
        notificationManager.notify(usersId.get(who.getUid()) , notification);
    }
}
