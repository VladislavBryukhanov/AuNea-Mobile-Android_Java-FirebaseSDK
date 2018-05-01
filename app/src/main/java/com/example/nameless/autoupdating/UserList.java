package com.example.nameless.autoupdating;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserList extends GlobalMenu {

    public static User myAcc;

    private EditText etSearch;
    private ListView lvUsers;
    private ArrayList<User> users;
    private UsersAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;

    private final int AUTH_SUCESS = 789;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        etSearch = findViewById(R.id.etSearch);
        lvUsers = findViewById(R.id.lvUsers);

        users = new ArrayList<>();
        adapter = new UsersAdapter(this, users);
        lvUsers.setAdapter(adapter);


//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            signIn();
        } else {
            Intent i = new Intent(UserList.this, Authentification.class);
            startActivityForResult(i, AUTH_SUCESS);
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
        if(requestCode == AUTH_SUCESS) {
            if(resultCode == Activity.RESULT_OK) {
                initialiseData();
            } else {
                finish();
            }
        }
    }

    private void initialiseData() {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Users");

        startService(new Intent(getApplicationContext(), NotifyService.class));

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

        lvUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), Chat.class);
//                String toUser = users.get(position).getEmail();
                intent.putExtra("to", users.get(position));
                startActivity(intent);
            }
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
                    initialiseData();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}
