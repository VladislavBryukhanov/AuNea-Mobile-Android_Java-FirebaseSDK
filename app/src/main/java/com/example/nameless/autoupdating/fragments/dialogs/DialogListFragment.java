package com.example.nameless.autoupdating.fragments.dialogs;

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
import com.example.nameless.autoupdating.common.FirebaseSingleton;
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
import java.util.Optional;

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


    public void fetchData() {
        // Todo Зарегался новый юзер и создал диалог - в диалог листе не отобразится
        dbUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot user : dataSnapshot.getChildren()) {
                    User newUserItem = user.getValue(User.class);
                    if (!newUserItem.getUid().equals(mAuth.getUid())) {
                        users.add(newUserItem);
                    }
                }
                // TODO сделать нормально, а не делать выборку всех юзеров и привязывать их к месседжам
                getMessages();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        dbUsers.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User newUserItem = dataSnapshot.getValue(User.class);

                for (Dialog dialog: dialogs) {
                    if (dialog.getSpeaker().getUid().equals(newUserItem.getUid())) {
                        int index = dialogs.indexOf(dialog);
                        dialog.setSpeaker(newUserItem);

                        dialogs.set(index, dialog);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void getMessages() {
        FirebaseDatabase database = FirebaseSingleton.getFirebaseInstanse();
        DatabaseReference dialogsDb = database.getReference("Dialogs");

        Query getChat = dialogsDb.orderByChild("speakers/" + mAuth.getUid()).equalTo(mAuth.getUid());
        getChat.keepSynced(true);
        getChat.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                assocDialogWithUser(dataSnapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    Optional<Dialog> dialogSearch = dialogs
                            .stream()
                            .filter(d -> d.getUid().equals(dataSnapshot.getKey()))
                            .findFirst();

                    if (dialogSearch.isPresent()) {

                        Dialog newDialog = dialogSearch.get();
                        int index = dialogs.indexOf(newDialog);

                        newDialog.setLastMessage(
                                dataSnapshot.child("lastMessage").getValue(Message.class));
                        newDialog.setUnreadCounter(
                                dataSnapshot.child("unreadCounter").getValue(Integer.class));

                        dialogs.set(index, newDialog);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    for (Dialog dialog : dialogs) {
                        if (dialog.getUid().equals(dataSnapshot.getKey())) {
                            int index = dialogs.indexOf(dialog);

                            dialog.setLastMessage(
                                    dataSnapshot.child("lastMessage").getValue(Message.class));
                            dialog.setUnreadCounter(
                                    dataSnapshot.child("unreadCounter").getValue(Integer.class));

                            dialogs.set(index, dialog);
                            adapter.notifyDataSetChanged();
                            return;
                        }
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {

            Iterable<DataSnapshot> speakers = dataSnapshot.child("speakers").getChildren();

            speakers.forEach(item -> {
                Optional<User> userSearch = users
                        .stream()
                        .filter(u -> u.getUid().equals(item.getValue()))
                        .findFirst();

                if (userSearch.isPresent()) {
                    User user = userSearch.get();

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
        } else {
            for (DataSnapshot speaker : dataSnapshot.child("speakers").getChildren()) {

                for (User user: users) {

                    if (user.getUid().equals(speaker.getValue())) {
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
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
