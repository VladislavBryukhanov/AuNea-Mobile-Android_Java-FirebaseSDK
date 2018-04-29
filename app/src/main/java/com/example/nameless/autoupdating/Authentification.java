package com.example.nameless.autoupdating;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class Authentification extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnSignIn;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    public static User myAcc;
//    private SQLiteDatabase db; // храним "куки" авторизации


    private String lastVersion;
    private String myVersion;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.INTERNET,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        PackageInfo pInfo = null;
        try {
            pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        myVersion = pInfo.versionName;

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);

        database = FirebaseDatabase.getInstance();


        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isAccessGranted = true;

                myAcc = new User(String.valueOf(etLogin.getText()),
                        String.valueOf(etPassword.getText()));
                myRef = database.getReference("Users");

                if (myAcc.getLogin().trim().length() > 0) {
                    myRef.child(myAcc.getLogin()).setValue(myAcc);
                } else {
                    isAccessGranted = false;
                }


                if (isAccessGranted) {
                    Intent intent = new Intent(Authentification.this, UserList.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

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
                            Updating update = new Updating(Authentification.this);
                            update.startUpdating();
                        } else {
                            Toast.makeText(Authentification.this, "You have latest version", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
                break;
            }
            case R.id.mGetVer: {
                AlertDialog.Builder builder = new AlertDialog.Builder(Authentification.this);
                builder.setTitle("Current version");
                builder.setMessage("Application's version: " + myVersion);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setCancelable(true);
                dialog.show();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }



    public static String getMacAddr() {
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
    }
    @Override
    protected void onStart() {
        stopService(new Intent(getApplicationContext(), NotifyService.class));
        super.onStart();
    }

    @Override
    protected void onDestroy() { //пока нет логаута и автоматического входа по "кукам"
        stopService(new Intent(getApplicationContext(), NotifyService.class));
        super.onDestroy();
    }
}
