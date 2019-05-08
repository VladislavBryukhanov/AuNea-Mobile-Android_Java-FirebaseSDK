package com.example.nameless.autoupdating.common;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Authentification;
import com.example.nameless.autoupdating.activities.Settings;
import com.example.nameless.autoupdating.models.AppData;
import com.example.nameless.autoupdating.models.AuthComplete;
import com.example.nameless.autoupdating.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class AuthGuard extends AppCompatActivity {

    private final int AUTH_SUCCESS = 789;
    private static User myAccount;
    private static AppData appData;

    private FirebaseAuth mAuth;
    FirebaseDatabase database;

    private AuthComplete authComplete;
    private CountDownLatch latch;

    private Double myVersion;

    public User getMyAccount() {
        return myAccount;
    }

    public void setMyAccount(User myAccount) {
        AuthGuard.myAccount = myAccount;
    }

    public static AppData getAppData() {
        return appData;
    }

    public static void setAppData(AppData appData) {
        AuthGuard.appData = appData;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

/*
        String[] requiredPermissions = new String[] {
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAPTURE_AUDIO_OUTPUT };
        ArrayList<String> permissionRequest = new ArrayList<>();

        for (String permission: requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionRequest.add(permission);
            }
        }
        ActivityCompat.requestPermissions(this, (String[]) permissionRequest.toArray(), 1);
*/

        database = FirebaseSingleton.getFirebaseInstanse();
        mAuth = FirebaseAuth.getInstance();

        latch = new CountDownLatch(2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AUTH_SUCCESS) {
            if(resultCode == Activity.RESULT_OK) {
                manageApplicationState();
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
            manageApplicationState();
            signIn();
        } else {
            Intent i = new Intent(this, Authentification.class);
            startActivityForResult(i, AUTH_SUCCESS);
        }
    }

    private void signIn() {
        Query getUser = database
                .getReference("Users")
                .orderByChild("uid")
                .equalTo(mAuth.getUid());

        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    myAccount = data.getValue(User.class);

                    if (myAccount.isBanned()) {
                        appLockDialog("You were banned", "Your account banned by administrator");
                        return;
                    }

                    data.child("banned").getRef().addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.exists()) {
                                return;
                            }
                            boolean isBanned = dataSnapshot.getValue(Boolean.class);
                            if (isBanned) {
                                appLockDialog("You were banned", "Your account banned by administrator");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });

                    completeAppUpdating(data);

                    myAccount.setEmail(mAuth.getCurrentUser().getEmail());
                    if(myAccount.getAvatarUrl() != null) {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference fileReference = storage.getReferenceFromUrl(myAccount.getAvatarUrl());
                        File imgFile = new File(getApplicationContext().getCacheDir(), fileReference.getName());
                        if (!imgFile.exists()) {
                            fileReference.getFile(imgFile);
                        }
                        myAccount.setAvatar(imgFile.getPath());
                    }

                    latch.countDown();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void manageApplicationState() {
        new Thread(() -> {
            try {
                // Waiting for completing app state checking
                latch.await();
                NetworkUtil.setNetworkStatus();
                runOnUiThread(() -> authComplete.onAuthSuccess());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            PackageInfo pInfo = AuthGuard.this.getPackageManager().getPackageInfo(getPackageName(), 0);
            myVersion = Double.parseDouble(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Query getAppVersion = database.getReference("AppData");
        getAppVersion.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                appData =  dataSnapshot.getValue(AppData.class);
                PackageInfo pInfo = null;

                try {
                    pInfo = AuthGuard.this.getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                myVersion = Double.parseDouble(pInfo.versionName);
                Double lastVersion = appData.getCurrentVersion();
                Double LastSupportedVersion = appData.getLastSupportedVersion();

                //Server locked by developer
                if (!appData.isServerAvailable()) {
                    appLockDialog("Engineering works", "Server temporarily unavailable, please try again later");
                    return;
                }

                if (lastVersion > myVersion) {
                    if (LastSupportedVersion > myVersion) {
                        // App version no longer supported (db structure or other fundamental features was changed)
                        AlertDialog.Builder builder = new AlertDialog.Builder(AuthGuard.this);
                        builder.setTitle("Please update app");
                        builder.setMessage("Your application version is out of date and no longer supported. " +
                                "\n\nCurrent version: " + myVersion +
                                "\n\nActual version: " + LastSupportedVersion);
                        builder.setPositiveButton("Update", (dialogInterface, i) -> {
                            dialogInterface.cancel();
                            Updating update = new Updating(getApplicationContext());
                            update.startUpdating();
                        });
                        builder.setNegativeButton("Close", ((dialogInterface, i) ->
                                android.os.Process.killProcess(android.os.Process.myPid())));
                        AlertDialog dialog = builder.create();
                        dialog.setCancelable(false);
                        dialog.show();
                        return;
                    } else {
                        // App has new version but still can works
                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        Notification notification = new Notification.Builder(getApplicationContext())
                                .setSmallIcon(R.mipmap.logo)
                                .setContentTitle("New version " + lastVersion + " already available")
                                .setContentText("Please update your app to latest version for using more features ").build();
                        notificationManager.notify(1, notification);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void completeAppUpdating(DataSnapshot snap) {
        SharedPreferences appPref = getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE);

        if (appPref.getBoolean(Updating.APP_UPDATING_PROCESS, true)) {
            snap.child("AppVersion").getRef().setValue(myVersion);
            FCMManager.subscribeToNotificationService();
            SharedPreferences.Editor prefs = appPref.edit();
            prefs.putBoolean(Updating.APP_UPDATING_PROCESS, false);
            prefs.apply();
        }
    }

    private void appLockDialog(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AuthGuard.this);
        builder.setTitle(title);
        builder.setMessage(content);
        builder.setPositiveButton("Close", (dialogInterface, i) ->
                android.os.Process.killProcess(android.os.Process.myPid()));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();

        //TODO dialog to class field if appStatus changed when dialog opened -> dialog.isShowing ? dialog.hide()
    }
}
