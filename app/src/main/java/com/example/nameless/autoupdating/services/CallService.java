package com.example.nameless.autoupdating.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.example.nameless.autoupdating.activities.VoiceCalling;
import com.example.nameless.autoupdating.generalModules.NetworkStateReceiver;
import com.example.nameless.autoupdating.models.ClientToClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by nameless on 11.04.18.
 */

public class CallService extends Service implements NetworkStateReceiver.NetworkStateReceiverListener {

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;

    private NetworkStateReceiver networkStateReceiver;
    private boolean isDisconnected;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        isDisconnected = false;
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        voiceNotifyListening();
    }

    private void voiceNotifyListening() {
        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(mAuth.getUid());
        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    myRef = database.getReference("Users");
                    myRef.child(data.getKey()).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            if(dataSnapshot.getKey().equals("voiceCall")) {
                                String dialogChannel = (String)dataSnapshot.getValue();
                                if(!dialogChannel.equals(VoiceCalling.CALLING_STATE)) {
                                    Intent intent = new Intent(getApplicationContext(), VoiceCalling.class);
                                    ClientToClient ctc = new ClientToClient((String)dataSnapshot.getValue(), mAuth.getUid());
                                    intent.putExtra("dialog", ctc);
                                    intent.putExtra("action", dataSnapshot.getKey());
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
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        voiceNotifyListening();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
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
