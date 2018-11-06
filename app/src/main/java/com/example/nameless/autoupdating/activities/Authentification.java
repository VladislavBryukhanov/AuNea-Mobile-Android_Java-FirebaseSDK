package com.example.nameless.autoupdating.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.nameless.autoupdating.generalModules.GlobalMenu;
import com.example.nameless.autoupdating.services.CallService;
import com.example.nameless.autoupdating.services.NotifyService;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class Authentification extends GlobalMenu {

    private EditText etLogin;
    private Button btnSignIn;

    private FirebaseDatabase database;
    private DatabaseReference myRef;


    private FirebaseAuth mAuth;
    public GoogleSignInClient mGoogleSignInClient;
    private final int RC_SIGN_IN = 565;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.INTERNET,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
                            Manifest.permission.CAPTURE_AUDIO_OUTPUT}, 1);
        }

        etLogin = findViewById(R.id.etLogin);
        btnSignIn = findViewById(R.id.btnSignIn);

        database = FirebaseDatabase.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();

        btnSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
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

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                UserList.myAcc = new User(mAuth.getUid(), String.valueOf(etLogin.getText()));
                signUp();
            } else {
                Toast.makeText(Authentification.this, ":(9(9((", Toast.LENGTH_SHORT).show();
            }
            });
    }

    public void signUp() {
        myRef = database.getReference("Users");

        if (UserList.myAcc.getLogin().trim().length() > 0) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            Query getUser = database.getReference("Users").orderByChild("uid").equalTo(UserList.myAcc.getUid());

            getUser.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getChildrenCount() == 0) {
                        myRef.push().setValue(UserList.myAcc);
                    }

                    for(DataSnapshot data : dataSnapshot.getChildren()) {
//                        myRef.child(data.getKey()).setValue(UserList.myAcc);
                        myRef.child(data.getKey()).child("login").setValue(UserList.myAcc.getLogin());
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
        setResult(Activity.RESULT_OK);
        finish();
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

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(Authentification.this, ":(9(9((2", Toast.LENGTH_SHORT).show();
            }
        }
    }

   @Override
    protected void onStart() {
        stopService(new Intent(this, NotifyService.class));
        stopService(new Intent(this, CallService.class));
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
