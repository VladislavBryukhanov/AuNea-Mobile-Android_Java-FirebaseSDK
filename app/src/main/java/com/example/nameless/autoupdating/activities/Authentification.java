package com.example.nameless.autoupdating.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.nameless.autoupdating.adapters.TabAdapter;
import com.example.nameless.autoupdating.common.AuthGuard;
import com.example.nameless.autoupdating.fragments.auth.SignInFragment;
import com.example.nameless.autoupdating.fragments.auth.SignUpFragment;
import com.example.nameless.autoupdating.models.AuthActions;
import com.example.nameless.autoupdating.common.FirebaseSingleton;
import com.example.nameless.autoupdating.common.GlobalMenu;
import com.example.nameless.autoupdating.common.FCMManager;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Authentification extends GlobalMenu implements AuthActions {

    private FirebaseDatabase database;
    private DatabaseReference usersRef;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    public static final int RC_PERMISSION = 1;
    public static final int RC_GOOGLE_AUTH = 2;
    public static final int RC_SIGN_UP = 3;
    public static final int RC_SIGN_IN = 4;

    private int currentAction;
    private User newUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

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
        ActivityCompat.requestPermissions(this, permissionRequest.toArray(new String[0]), 1);
        // TODO open Dialog and close app if user denied perms


        database = FirebaseSingleton.getFirebaseInstanse();
        usersRef = database.getReference("Users");
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        TabAdapter adapter = new TabAdapter(getSupportFragmentManager());
        adapter.addFragment(new SignUpFragment(), "Sign up");
        adapter.addFragment(new SignInFragment(), "Sign in");
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem hideItem = menu.findItem(R.id.mLogOut);
        hideItem.setVisible(false);
        hideItem = menu.findItem(R.id.mSettings);
        hideItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_AUTH) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(Authentification.this, "Authentication intent error", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }


    @Override
    public void onAuthenticateAction(int requestCode, User user) {
        currentAction = requestCode;
        newUser = user;

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Authentification.RC_GOOGLE_AUTH);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    if (currentAction == RC_SIGN_UP) {
                        newUser.setUid(mAuth.getUid());
                        signUp(newUser);
                    } else {
                        signIn();
                    }
                } else {
                    Toast.makeText(Authentification.this, "Google auth error", Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void signUp(User user) {
        if (user.getLogin().trim().length() > 0) {
            Query getUser = database.getReference("Users").orderByChild("uid").equalTo(mAuth.getUid());

            getUser.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getChildrenCount() == 0) {
                        usersRef.push().setValue(user);
                        authenticate(user);
                    } else {
                        Toast.makeText(Authentification.this, "Such user already exists, please Sign in", Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    public void signIn() {
        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(mAuth.getUid());

        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount() == 0) {
                    Toast.makeText(Authentification.this, "Such user is not exists, please Sign up", Toast.LENGTH_LONG).show();
                } else {
                    for(DataSnapshot data : dataSnapshot.getChildren()) {
                        authenticate(data.getValue(User.class));
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void authenticate(User user) {
        setMyAccount(user);
        FCMManager.subscribeToNotificationService();
        setResult(Activity.RESULT_OK);
        finish();
    }

/*    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {

        }
        return "02:00:00:00:00:00";
    }*/

}
