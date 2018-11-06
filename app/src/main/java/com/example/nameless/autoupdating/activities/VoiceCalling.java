package com.example.nameless.autoupdating.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.voip.ListenVoiceStream;
import com.example.nameless.autoupdating.voip.UDPClient;
import com.example.nameless.autoupdating.voip.WriteVoiceStream;
import com.example.nameless.autoupdating.models.ClientToClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


public class VoiceCalling extends AppCompatActivity {

    public static final String CALLING_STATE = "calling...";
    public static final String INCOMING_CALL_ACTION = "incoming_call";
    public static final String OUTGOING_CALL_ACTION = "outgoing_call";

    private DatabaseReference myRef, toRef;
    private ValueEventListener connectionListener;

    private ListenVoiceStream voiceStreamListener;
    private WriteVoiceStream voiceWriter;
    private UDPClient client;

    private FloatingActionButton btnAccept, btnReject;
    private String action;

    private Ringtone ringtone;
    private MediaPlayer beep;

    private int privateRoomPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_calling);


        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);

        final Intent intent = getIntent();
        action = intent.getStringExtra("action");

        Query getUser = FirebaseDatabase.getInstance()
                .getReference("Users")
                .orderByChild("uid")
                .equalTo(FirebaseAuth.getInstance().getUid());
        getUser.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                myRef = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(dataSnapshot.getKey())
                        .child("voiceCall");

                if(action.equals(VoiceCalling.OUTGOING_CALL_ACTION)) {
                    ClientToClient ctc = (ClientToClient)intent.getSerializableExtra("dialog");
                    CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) btnReject.getLayoutParams();
                    lp.anchorGravity = Gravity.CENTER;
                    btnReject.setLayoutParams(lp);
                    btnAccept.setVisibility(View.GONE);

                    initConnection(ctc.getSecondUser());

                    beep = MediaPlayer.create(getBaseContext(), R.raw.beep);
                    beep.setLooping(true);
                    beep.start();
                } else if (action.equals(VoiceCalling.INCOMING_CALL_ACTION)) {
                    privateRoomPort = Integer.parseInt(intent.getStringExtra("privateRoomPort"));
                    client = new UDPClient(UserList.voiceStreamServerIpAddress, privateRoomPort, () -> onReject());
                    client.createPrivateStream("secondCon");
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    ringtone.play();
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(!dataSnapshot.child("voiceCall").exists()) {
                    closeConnection();
                }
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        btnAccept.setOnClickListener(view -> {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) btnReject.getLayoutParams();
            lp.anchorGravity = Gravity.CENTER;
            btnReject.setLayoutParams(lp);
            btnAccept.setVisibility(View.GONE);
            onAccept();
        });
        btnReject.setOnClickListener(view -> onReject());
    }

    private void onAccept() {
        myRef.setValue(CALLING_STATE).addOnCompleteListener(task -> {
            ringtone.stop();
            createConnection();
        });
    }

    private void onReject() {
        if(connectionListener != null) {
            toRef.removeEventListener(connectionListener);
        }
        if(toRef != null) {
            toRef.removeValue();
        }
        if(myRef != null) {
            myRef.removeValue();
        }
        closeConnection();
    }

    private void initConnection(final String who) {
        Query getUser = FirebaseDatabase.getInstance()
                .getReference("Users")
                .orderByChild("uid")
                .equalTo(who);
        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    toRef = FirebaseDatabase.getInstance()
                            .getReference("Users")
                            .child(data.getKey())
                            .child("voiceCall");
                    if(action.equals(VoiceCalling.OUTGOING_CALL_ACTION)) {
                        client = new UDPClient(
                                UserList.voiceStreamServerIpAddress,
                                UserList.voiceStreamServerPort,
                                () -> onReject());
                        privateRoomPort = client.createPrivateStream("firstCon");

                        if (privateRoomPort < 0) {
                            Toast.makeText(VoiceCalling.this, "Server is not available", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Handler rejectHandler = new Handler();
                        rejectHandler.postDelayed(() -> onReject(), 30000); // calling timeout

                        myRef.setValue(CALLING_STATE);
                        myRef.onDisconnect().removeValue();
                        toRef.setValue(String.valueOf(privateRoomPort));
                        toRef.addValueEventListener(connectionListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String key = dataSnapshot.getKey();
                                String value = (String)(dataSnapshot.getValue());
                                if (key == null || value == null) {
                                    onReject();
                                } else  if(key.equals("voiceCall") && value.equals(CALLING_STATE)) {
                                    beep.stop();
                                    rejectHandler.removeCallbacksAndMessages(null);
                                    createConnection();
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                        toRef.onDisconnect().removeValue();
/*                        toRef.onDisconnect().removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(connectionListener != null) {
                                    toRef.removeEventListener(connectionListener);
                                }
                            }
                        });*/

                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void createConnection() {
        Thread streamThread = new Thread(() -> {
            Thread writerThread = new Thread(() -> {
                voiceWriter = new WriteVoiceStream(privateRoomPort);
                voiceWriter.start();
                onReject();
            });
            writerThread.start();

            Thread listenerThread = new Thread(() -> {
                voiceStreamListener = new ListenVoiceStream(client);
                voiceStreamListener.start();
                onReject();
            });
            listenerThread.start();
        });
        streamThread.start();
    }

    private void closeConnection() {
/*        if(streamThread != null && streamThread.isAlive()) {
            streamThread.interrupt();
        }*/
        if(voiceStreamListener != null) {
            voiceStreamListener.stop();
        }
        if(voiceWriter != null) {
            voiceWriter.stop();
        }
        if(ringtone != null) {
            ringtone.stop();
        }
        if(beep != null) { // && beep.isPlaying()) {
            beep.stop();
//            beep.release();
//            beep = null;
        }
        if(client != null) {
            client.closeStream();
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        onReject();
        super.onDestroy();
    }
}


