package com.example.nameless.autoupdating.common;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.example.nameless.autoupdating.activities.Authentification;
import com.example.nameless.autoupdating.models.AuthComplete;
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

public class AuthGuard extends AppCompatActivity {

    private final int AUTH_SUCCESS = 789;
    private static User myAccount;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference dbUsers;

    private AuthComplete authComplete;

    public User getMyAccount() {
        return myAccount;
    }

    public void setMyAccount(User myAccount) {
        this.myAccount = myAccount;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        database = FirebaseSingleton.getFirebaseInstanse();
        dbUsers = database.getReference("Users");

        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {
                            android.Manifest.permission.INTERNET,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.CAPTURE_AUDIO_OUTPUT }, 1);
        }
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

    // Application can be started from 3 activity - UserList, Chat, VOIP, protect them
    public void checkAccess(AuthComplete authComplete) {
        this.authComplete = authComplete;

        if (myAccount != null ) {
            authComplete.onAuthSuccess();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            signIn();
        } else {
            Intent i = new Intent(this, Authentification.class);
            startActivityForResult(i, AUTH_SUCCESS);
        }
    }

    private void signIn() {
        Query getUser = dbUsers.orderByChild("uid").equalTo(mAuth.getUid());
        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    myAccount = data.getValue(User.class);
                    myAccount.setEmail(mAuth.getCurrentUser().getEmail());
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
                    if(myAccount.getAvatarUrl() != null) {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference fileReference = storage.getReferenceFromUrl(myAccount.getAvatarUrl());
//                        File path = new File(Environment.getExternalStorageDirectory()
//                                + "/AUMessanger/Users/");
//                        if (!path.exists()) {
//                            path.mkdir();
//                        }
                        File imgFile = new File(getApplicationContext().getCacheDir(), fileReference.getName());
                        if (!imgFile.exists()) {
                            fileReference.getFile(imgFile);
                        }
                        myAccount.setAvatar(imgFile.getPath());
                    }

                    //TODO |reactive| update from main app instance
                    NetworkUtil.setNetworkStatus();
                    authComplete.onAuthSuccess();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

}
