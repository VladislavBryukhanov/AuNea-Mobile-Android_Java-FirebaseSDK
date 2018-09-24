package com.example.nameless.autoupdating.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nameless on 11.04.18.
 */

public class NotifyService extends Service implements NetworkStateReceiver.NetworkStateReceiverListener {

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private HashMap<String, Boolean> timeOut; //первым childAdded всегда будет последнее отправленное нам сообщение во время диалога, пропускаем его
    private boolean isServiceStopped; // т к он старт является асинхронным может произойти так, что он отработает после дестроя, тоесть обработчики будут навешаны после попытки их удаления => при дестрое ни 1 обработчик не удалится

    private ChildEventListener newMsgListener;
    private Query refToListener;
    private HashMap<Query, ChildEventListener> refToListeners;
    private FirebaseAuth mAuth;

    private NetworkStateReceiver networkStateReceiver;
    private boolean isDisconnected;
    private String interlocutor; //собеседник

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        refToListeners = new HashMap<>();
        timeOut = new HashMap<>();
        isServiceStopped = false;

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

                    if (!isServiceStopped && (myListener.equals(mAuth.getUid())
                            || foreignListener.equals(mAuth.getUid()))) {

                        if (!myListener.equals(mAuth.getUid()) ) {
                            foreignListener = myListener;
                        }

                        final String foreignListenerTmp = foreignListener;

//                        if(!foreignListenerTmp.equals(interlocutor)) {
                            refToListener = myRef.child(dlg.getKey()).child("content").limitToLast(1);
                            refToListener.addChildEventListener(newMsgListener = new ChildEventListener() {
                                @Override
                                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                                    Message newMsg = dataSnapshot.getValue(Message.class);
                                    if (!(newMsg.getWho()).equals(interlocutor)
                                            && (newMsg.getTo()).equals(mAuth.getUid())) {
                                        if(!timeOut.get(foreignListenerTmp)) {
                                            timeOut.put(foreignListenerTmp, true);
                                        } else {
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
//                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            interlocutor = intent.getStringExtra("dialog");
        }
        if(isServiceStopped) {
            createListeners();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isServiceStopped = true;
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
        for(Map.Entry<Query, ChildEventListener> entry : refToListeners.entrySet()) {
            (entry.getKey()).removeEventListener(entry.getValue());
        }
        Intent broadcastIntent = new Intent("android.intent.action.RestartNotificationService");
        sendBroadcast(broadcastIntent);
    }


    public void sendNotify(final Message msg) {

        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(msg.getWho());
        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    final User user = data.getValue(User.class);

                    if(user.getAvatarUrl() != null) {
                        Target target = new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                buildNotify(user, msg, bitmap);
                            }
                            @Override
                            public void onBitmapFailed(Drawable errorDrawable) {
                            }
                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {
                            }
                        };
                        Picasso.with(getApplicationContext()).load(user.getAvatarUrl()).into(target);
                    } else {
                        BitmapDrawable bitmapDrawable = (BitmapDrawable)getDrawable(R.drawable.avatar);
                        Bitmap largeIconBitmap = bitmapDrawable.getBitmap();
                        buildNotify(user, msg, largeIconBitmap);
                    }

//                        buildNotify(user, msg);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void buildNotify(User to, Message msg, Bitmap largeIconBitmap) {
        Intent notificationIntent = new Intent(getApplicationContext(), Chat.class);
        notificationIntent.putExtra("to", to);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder .setContentTitle(to.getLogin())
                .setContentText(msg.getContent())
                .setContentIntent(intent)
                .setVibrate(new long[] { 400, 400 })
                .setAutoCancel(true)
                .setLargeIcon(largeIconBitmap)
                .setShowWhen(true)
                .setWhen(Calendar.getInstance().getTimeInMillis())
                .setSmallIcon(R.drawable.send2);

        builder.setDefaults(Notification.DEFAULT_ALL);

        SharedPreferences settings = getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE);
        if(settings.getBoolean(Settings.IS_NOTIFY_ENABLED, false)) {
            builder.setSound(Uri.parse("android.resource://" + this.getApplicationContext()
                    .getPackageName() + File.separator + R.raw.notify));
        }

        Notification.Builder fullScreenNotify = builder;
//        fullScreenNotify.setFullScreenIntent(intent, true);
        fullScreenNotify.setPriority(Notification.PRIORITY_HIGH);

        Notification notification = fullScreenNotify.build();
//        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_INSISTENT;
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(1, notification);
    }

    @Override
    public void networkAvailable() {
        if(isDisconnected) {
            isDisconnected = false;
            stopSelf();
        }
    }

    @Override
    public void networkUnavailable() {
        isDisconnected = true;
    }
}
