package com.example.nameless.autoupdating.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.widget.EditText;
import android.widget.ListView;

import com.example.nameless.autoupdating.common.GlobalMenu;
import com.example.nameless.autoupdating.receivers.NetworkStateReceiver;
import com.example.nameless.autoupdating.services.CallService;
import com.example.nameless.autoupdating.services.NotifyService;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.adapters.UsersAdapter;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class UserList extends GlobalMenu implements NetworkStateReceiver.NetworkStateReceiverListener {

    public static User myAcc;

    private EditText etSearch;
    private ListView lvUsers;
    private ArrayList<User> users;
    private UsersAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;

    private final int AUTH_SUCCESS = 789;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        etSearch = findViewById(R.id.etSearch);
        lvUsers = findViewById(R.id.lvUsers);

        users = new ArrayList<>();
        adapter = new UsersAdapter(this, users);
        lvUsers.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

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

    private void initialiseData() {

        database = FirebaseDatabase.getInstance();

        myRef = database.getReference("Users");

        if (!isMyServiceRunning(NotifyService.class, getApplicationContext())) {
            startService(new Intent(getApplicationContext(), NotifyService.class));
        }
        if (!isMyServiceRunning(CallService.class, getApplicationContext())) {
            startService(new Intent(getApplicationContext(), CallService.class));
        }

//        Intent broadcastIntent = new Intent("android.intent.action.startServices");
//        sendBroadcast(broadcastIntent);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot user : dataSnapshot.getChildren()) {
                    User newUserItem = user.getValue(User.class);
                    if (!newUserItem.getUid().equals(myAcc.getUid())) {
                        users.add(newUserItem);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getApplicationContext(), Chat.class);
            intent.putExtra("to", users.get(position));
            startActivity(intent);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void signIn() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(mAuth.getUid());
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

    @Override
    public void networkAvailable() {
        setStatus();
    }

    @Override
    public void networkUnavailable() {

    }
}
