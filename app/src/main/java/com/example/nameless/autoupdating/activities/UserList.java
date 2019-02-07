package com.example.nameless.autoupdating.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.view.Menu;

import com.example.nameless.autoupdating.fragments.DialogListFragment;
import com.example.nameless.autoupdating.fragments.UsersListFragment;
import com.example.nameless.autoupdating.generalModules.FirebaseSingleton;
import com.example.nameless.autoupdating.generalModules.GlobalMenu;
import com.example.nameless.autoupdating.receivers.NetworkStateReceiver;
import com.example.nameless.autoupdating.services.CallService;
import com.example.nameless.autoupdating.services.NotifyService;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserList extends GlobalMenu implements NetworkStateReceiver.NetworkStateReceiverListener {

    public static User myAcc;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference dbUsers;

    private final int AUTH_SUCCESS = 789;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        database = FirebaseSingleton.getFirebaseInstanse();
        dbUsers = database.getReference("Users");

        if (currentUser != null) {
            signIn();
        } else {
            Intent i = new Intent(UserList.this, Authentification.class);
            startActivityForResult(i, AUTH_SUCCESS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AUTH_SUCCESS) {
            if(resultCode == Activity.RESULT_OK) {
                signIn();
            } else {
                finish();
            }
        }
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context c) {
        ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void setStatus() {
        final DatabaseReference connectedRef = FirebaseDatabase
                .getInstance()
                .getReference(".info/connected");
        connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {

                final Query getUser = FirebaseDatabase
                        .getInstance()
                        .getReference("Users")
                        .orderByChild("uid")
                        .equalTo(mAuth.getUid());

                getUser.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot data : dataSnapshot.getChildren()) {

                            final DatabaseReference userRef = getUser.getRef()
                                    .child(data.getKey()).child("status");
                            DateFormat dateFormat = (new SimpleDateFormat(" HH:mm dd MMM", Locale.ENGLISH));
                            final String lastSeen = "last seen at" + dateFormat.format(new Date());

                            boolean connected = snapshot.getValue(Boolean.class);
                            if (connected) {
                                userRef.onDisconnect().setValue(lastSeen);
                                userRef.setValue("online");
                            } else { //Вроде как else никогда не отрабатывает
                                userRef.setValue(lastSeen);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }



    public void signIn() {
        Query getUser = dbUsers.orderByChild("uid").equalTo(mAuth.getUid());
        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    myAcc = data.getValue(User.class);
                    myAcc.setEmail(mAuth.getCurrentUser().getEmail());
    /*                Thread loadAvatarThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InputStream in =null;
                            try {
                                in = new java.net.URL(myAcc.getAvatarUrl()).openStream();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            myAcc.setAvatar(BitmapFactory.decodeStream(in));
                        }
                    });
                    loadAvatarThread.start();*/
                    if(myAcc.getAvatarUrl() != null) {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference fileReference = storage.getReferenceFromUrl(myAcc.getAvatarUrl());
//                        File path = new File(Environment.getExternalStorageDirectory()
//                                + "/AUMessanger/Users/");
//                        if (!path.exists()) {
//                            path.mkdir();
//                        }
                        File imgFile = new File(getApplicationContext().getCacheDir(), fileReference.getName());
                        if (!imgFile.exists()) {
                            fileReference.getFile(imgFile);
                        }
                        myAcc.setAvatar(imgFile.getPath());
                    }
                    setStatus();
                    initialiseData();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void initialiseData() {
        if (!UserList.isMyServiceRunning(NotifyService.class, this)) {
            startService(new Intent(this, NotifyService.class));
        }
        if (!UserList.isMyServiceRunning(CallService.class, this)) {
            startService(new Intent(this, CallService.class));
        }

//        Intent broadcastIntent = new Intent("android.intent.action.startServices");
//        sendBroadcast(broadcastIntent);

        FragmentTabHost tabHost = findViewById(R.id.tabhost);
        tabHost.setup(getApplicationContext(), getSupportFragmentManager(), R.id.tabcontent);
        tabHost.addTab(tabHost.newTabSpec("Users").setIndicator("Users"),
                UsersListFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec("Dialogs").setIndicator("Dialogs"),
                DialogListFragment.class, null);

        tabHost.setCurrentTab(0);
    }

    @Override
    public void networkAvailable() {
        setStatus();
    }

    @Override
    public void networkUnavailable() {

    }
}
