package com.example.nameless.autoupdating.common;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Settings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by nameless on 29.04.18.
 */

public class GlobalMenu extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private String lastVersion;
    private String myVersion;

    private FirebaseAuth mAuth;

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
                            android.Manifest.permission.CAPTURE_AUDIO_OUTPUT}, 1);
        }

        PackageInfo pInfo = null;
        try {
            pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        myVersion = pInfo.versionName;
        database = FirebaseSingleton.getFirebaseInstanse();
        mAuth = FirebaseAuth.getInstance();

    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.mUpdate: {
                myRef = database.getReference("LastVersion");

                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        lastVersion =  dataSnapshot.child("Ver").getValue().toString();

                        if (Double.parseDouble(lastVersion) > Double.parseDouble(myVersion)) {
                            Updating update = new Updating(getApplicationContext());
                            update.startUpdating();
                        } else {
                            Toast.makeText(getApplicationContext(), "You have latest version", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
                break;
            }
            case R.id.mGetVer: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Current version");
                builder.setMessage("Application's version: " + myVersion);
                builder.setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.cancel());
                AlertDialog dialog = builder.create();
                dialog.setCancelable(true);
                dialog.show();
                break;
            }
            case R.id.mSettings: {
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                break;
            }
            case R.id.mLogOut: {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

                mAuth.signOut();
                mGoogleSignInClient.signOut();

                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}