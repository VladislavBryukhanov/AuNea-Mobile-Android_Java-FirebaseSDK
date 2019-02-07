package com.example.nameless.autoupdating.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Chat;
import com.example.nameless.autoupdating.adapters.DialogsAdapter;
import com.example.nameless.autoupdating.adapters.UsersAdapter;
import com.example.nameless.autoupdating.generalModules.FirebaseSingleton;
import com.example.nameless.autoupdating.models.Dialog;
import com.example.nameless.autoupdating.models.Message;
import com.example.nameless.autoupdating.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DialogListFragment  extends Fragment {

    private EditText etSearch;
    private ListView lvUsers;

    private ArrayList<User> users;
    private ArrayList<Dialog> dialogs;
    private DialogsAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference dbUsers;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        users = new ArrayList<>();
        dialogs = new ArrayList<>();
        adapter = new DialogsAdapter(getContext(), dialogs);

        database = FirebaseSingleton.getFirebaseInstanse();
        dbUsers = database.getReference("Users");
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialogs_list, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        lvUsers = view.findViewById(R.id.lvUsers);

        lvUsers.setAdapter(adapter);
        initialiseListeners();

        if (dialogs.size() == 0) {
            fetchData();
        }

        return view;
    }

    private void initialiseListeners() {

        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getContext(), Chat.class);
            intent.putExtra("to", dialogs.get(position).getSpeaker());
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


    public void getMessages() {
        FirebaseDatabase database = FirebaseSingleton.getFirebaseInstanse();
        DatabaseReference dialogsDb = database.getReference("Dialogs");

        Query getChat = dialogsDb.orderByChild("speakers/" + mAuth.getUid());
        getChat.keepSynced(true);
        getChat.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                assocDialogWithUser(dataSnapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                for (int i = 0; i < dialogs.size(); i++) {
                    if (((dialogs.get(i)).getUid()).equals(dataSnapshot.getKey())) {
                        Dialog newDialog = dialogs.get(i);
                        newDialog.setLastMessage(
                                dataSnapshot.child("lastMessage").getValue(Message.class)
                        );
                        newDialog.setUnreadCounter(
                                dataSnapshot.child("unreadCounter").getValue(Integer.class)
                        );

                        dialogs.set(i, newDialog);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void assocDialogWithUser(DataSnapshot dataSnapshot) {
        Iterable<DataSnapshot> speakers = dataSnapshot.child("speakers").getChildren();
        speakers.forEach(item -> {
            users.forEach(user -> {
                if(user.getUid().equals(item.getValue())) {

                    int unreadCounter = dataSnapshot.child("unreadCounter")
                            .getValue(Integer.class);
                    Message lastMessage = dataSnapshot.child("lastMessage")
                            .getValue(Message.class);

                    Dialog dialog = new Dialog(
                            dataSnapshot.getKey(),
                            lastMessage,
                            unreadCounter,
                            user
                    );
                    dialogs.add(dialog);
                }
            });
        });
        adapter.notifyDataSetChanged();
    }

    public void fetchData() {
        dbUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot user : dataSnapshot.getChildren()) {
                    User newUserItem = user.getValue(User.class);
                    if (!newUserItem.getUid().equals(mAuth.getUid())) {
                        users.add(newUserItem);
                    }
                }
//                adapter.notifyDataSetChanged();
                // TODO сделать нормально, а не делать выборку всех юзеров и привязывать их к месседжам
                getMessages();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}
