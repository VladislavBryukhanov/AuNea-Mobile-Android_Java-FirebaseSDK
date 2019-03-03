package com.example.nameless.autoupdating.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import com.example.nameless.autoupdating.adapters.TabAdapter;
import com.example.nameless.autoupdating.common.FirebaseSingleton;
import com.example.nameless.autoupdating.common.GlobalMenu;
import com.example.nameless.autoupdating.common.NetworkUtil;
import com.example.nameless.autoupdating.fragments.dialogs.DialogListFragment;
import com.example.nameless.autoupdating.fragments.dialogs.UsersListFragment;
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

public class UserList extends GlobalMenu {

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

                    initialiseData();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void initialiseData() {

//        ConnectivityNetworkListener con = new ConnectivityNetworkListener(UserList.this);
        //TODO |reactive| update from main app instance
        NetworkUtil.setNetworkStatus();

//        Intent broadcastIntent = new Intent("android.intent.action.startServices");
//        sendBroadcast(broadcastIntent);

/*
        FragmentTabHost tabHost = findViewById(R.id.tabhost);
        tabHost.setup(getApplicationContext(), getSupportFragmentManager(), R.id.tabcontent);
        tabHost.addTab(tabHost.newTabSpec("Users").setIndicator("Users"),
                UsersListFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec("Dialogs").setIndicator("Dialogs"),
                DialogListFragment.class, null);

        tabHost.setCurrentTab(0);
*/

        ViewPager viewPager = findViewById(R.id.pager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        TabAdapter adapter = new TabAdapter(getSupportFragmentManager());
        adapter.addFragment(new DialogListFragment(), "Dialogs");
        adapter.addFragment(new UsersListFragment(), "Users");
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);

        DatabaseReference dialogsDb = database.getReference("Dialogs");
        Query getChat = dialogsDb.orderByChild("speakers/" + mAuth.getUid()).equalTo(mAuth.getUid());
        getChat.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == 0) {
                    viewPager.setCurrentItem(1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
}
