package com.example.nameless.autoupdating.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.nameless.autoupdating.receivers.NetworkStateReceiver;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Chat;
import com.example.nameless.autoupdating.activities.Settings;
import com.example.nameless.autoupdating.models.Message;
import com.example.nameless.autoupdating.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nameless on 11.04.18.
 */

public class NotifyService extends Service implements NetworkStateReceiver.NetworkStateReceiverListener {

    private final String NOTIFY_CHANNEL_NAME = "NOTIFY_CHANNEL_ID";
    private final String DEFAULT_SOUND_CHANNEL = "DEFAULT_SOUND_CHANNEL";
    private final String CUSTOM_SOUND_CHANNEL = "CUSTOM_SOUND_CHANNEL";

    private Uri notifySoundUri;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private HashMap<String, Boolean> timeOut; //первым childAdded всегда будет последнее отправленное нам сообщение во время диалога, пропускаем его

    private ChildEventListener newMsgListener;
    private Query refToListener;
    private HashMap<Query, ChildEventListener> refToListeners;
    private FirebaseAuth mAuth;

    private NetworkStateReceiver networkStateReceiver;
    private boolean isDisconnected;
    private String interlocutor; //собеседник
    private Date disconnectTime;

    private HashMap<String, Integer> usersId;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        notifySoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getApplicationContext().getPackageName() + File.separator + R.raw.notify);

        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        refToListeners = new HashMap<>();
        timeOut = new HashMap<>();
        usersId = new HashMap<>();

        interlocutor = "";
        isDisconnected = false;
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        createListeners();
    }

    private void createListeners() {
        myRef = database.getReference("Messages");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                timeOut = new HashMap<>();
                refToListeners = new HashMap<>();

                for (DataSnapshot dlg : dataSnapshot.getChildren()) {
                    String myListener = String.valueOf(dlg.child("listener1").getValue());
                    String foreignListener = String.valueOf(dlg.child("listener2").getValue());

                    if (myListener.equals(mAuth.getUid())
                            || foreignListener.equals(mAuth.getUid())) {

                        if (!myListener.equals(mAuth.getUid()) ) {
                            foreignListener = myListener;
                        }

                        final String foreignListenerTmp = foreignListener;

                        refToListener = myRef.child(dlg.getKey()).child("content");
                        refToListener.limitToLast(1).addChildEventListener(newMsgListener = new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                                Message newMsg = dataSnapshot.getValue(Message.class);
                                if(!timeOut.get(foreignListenerTmp)) {
                                    timeOut.put(foreignListenerTmp, true);
                                } else {
                                    if (!(newMsg.getWho()).equals(interlocutor)
                                            && (newMsg.getTo()).equals(mAuth.getUid())) {
                                        sendNotify(newMsg);
                                    }
                                }
                            }
                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {}
                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                        refToListeners.put(refToListener, newMsgListener);
                        timeOut.put(foreignListener, false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getMissedNotifications() {
        for(Map.Entry<Query, ChildEventListener> entry : refToListeners.entrySet()) {
            entry.getKey().orderByChild("read").equalTo(false).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Message newMsg = data.getValue(Message.class);
                        if((newMsg.getDateOfSend()).compareTo(disconnectTime) > 0) {
                            if (!(newMsg.getWho()).equals(interlocutor)
                                    && (newMsg.getTo()).equals(mAuth.getUid())) {
                                sendNotify(newMsg);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
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
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
        for(Map.Entry<Query, ChildEventListener> entry : refToListeners.entrySet()) {
            (entry.getKey()).removeEventListener(entry.getValue());
        }
//        disconnectTime = new Date();

//        Intent broadcastIntent = new Intent("android.intent.action.RestartNotificationService");
//        sendBroadcast(broadcastIntent);
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

    @Override
    public void networkAvailable() {
        if(isDisconnected) {
            isDisconnected = false;
//            stopSelf();
            getMissedNotifications();
        }
    }

    @Override
    public void networkUnavailable() {
        isDisconnected = true;
        disconnectTime = new Date();
    }
}
