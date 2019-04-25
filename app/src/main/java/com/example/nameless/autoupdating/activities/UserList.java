package com.example.nameless.autoupdating.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import com.example.nameless.autoupdating.adapters.TabAdapter;
import com.example.nameless.autoupdating.common.AuthGuard;
import com.example.nameless.autoupdating.common.FirebaseSingleton;
import com.example.nameless.autoupdating.common.GlobalMenu;
import com.example.nameless.autoupdating.fragments.dialogs.DialogListFragment;
import com.example.nameless.autoupdating.fragments.dialogs.UsersListFragment;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.models.AuthComplete;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class UserList extends GlobalMenu implements AuthComplete {

    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_light);
        super.checkAccess(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onAuthSuccess() {
        setContentView(R.layout.activity_user_list);

        database = FirebaseSingleton.getFirebaseInstanse();
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
        String myUid = getMyAccount().getUid();
        Query getChat = dialogsDb.orderByChild("speakers/" + myUid).equalTo(myUid);
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
