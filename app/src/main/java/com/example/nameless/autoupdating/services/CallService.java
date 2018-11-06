package com.example.nameless.autoupdating.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.example.nameless.autoupdating.activities.VoiceCalling;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nameless on 11.04.18.
 */

public class CallService extends Service {

    private FirebaseDatabase database;
    private DatabaseReference refToListener;
    private FirebaseAuth mAuth;

    private HashMap<Query, ChildEventListener> refToListeners;
    private ChildEventListener newCallListener;

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

        voiceNotifyListening();
    }

    private void voiceNotifyListening() {
        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(mAuth.getUid());
        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    refToListener = database.getReference("Users");
                    refToListener.child(data.getKey()).addChildEventListener(newCallListener = new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            if(dataSnapshot.getKey().equals("voiceCall")) {
                                String dialogChannel = (String)dataSnapshot.getValue();
                                if(!dialogChannel.equals(VoiceCalling.CALLING_STATE)) {
                                    Intent intent = new Intent(getApplicationContext(), VoiceCalling.class);
                                    intent.putExtra("action", VoiceCalling.INCOMING_CALL_ACTION);
                                    intent.putExtra("privateRoomPort", dialogChannel);
                                    startActivity(intent);
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
                    refToListeners.put(refToListener, newCallListener);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        for(Map.Entry<Query, ChildEventListener> entry : refToListeners.entrySet()) {
            (entry.getKey()).removeEventListener(entry.getValue());
        }
        // not working
//        Intent broadcastIntent = new Intent("android.intent.action.RestartCallNotificationService");
//        sendBroadcast(broadcastIntent);
    }

}
