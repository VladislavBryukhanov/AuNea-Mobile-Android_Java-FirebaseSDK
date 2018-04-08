package com.example.nameless.autoupdating;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class Chat extends AppCompatActivity {

    private Button btnSend;
    private EditText etMessage;
    private ListView lvMessages;
    private MessagesAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private ArrayList<Message> messages;
    private User to;
    private String tableName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnSend = findViewById(R.id.btnSend);
        etMessage = findViewById(R.id.etMessage);
        lvMessages = findViewById(R.id.lvMessages);

        messages = new ArrayList<>();
        Intent intent = getIntent();
        to = (User)intent.getSerializableExtra("to");
        adapter = new MessagesAdapter(this, messages);
        lvMessages.setAdapter(adapter);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Messages");

//todo с помощью запроса получить название таблицы, которае содержит подстроку - логин нашего профиля, вместо поиска через листенер
//todo time to live for messages
//todo paging
//todo authorise, cookie, security for dialogs
//todo оптимизация алгоритма выборки, разобраться какой расход трафика при выборке, что такое датаснэпшот, является ли он полностью готовым пришедшим с сервера пакетом данных или делает динамические запросы на выборку
//todo сохранение состояниея при повороте экрана
//todo каскадное удаление
//        Query dialogName = database.getReference("Messages")//.child(MainActivity.myAcc.getLogin() +  "+" + to.getLogin())
//                .orderByKey()
//                .startAt(MainActivity.myAcc.getLogin())
//                .endAt(MainActivity.myAcc.getLogin() +  "\uf8ff");
//        myRef = dialogName.getRef();
//        Toast.makeText(this, myRef.getKey(), Toast.LENGTH_SHORT).show();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                tableName = to.getLogin() +  "+" + MainActivity.myAcc.getLogin();
                if (dataSnapshot.hasChild(tableName)) {
                    myRef = database.getReference("Messages").child(tableName);
                } else {
                    tableName = MainActivity.myAcc.getLogin() +  "+" + to.getLogin();
                    myRef = database.getReference("Messages").child(tableName);
                }

                myRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        messages.add(dataSnapshot.getValue(Message.class));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


//        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                for(DataSnapshot user : dataSnapshot.getChildren()) {
//                    messages.add(user.getValue(Message.class));
//                }
//                adapter.notifyDataSetChanged();
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//            }
//        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!String.valueOf(etMessage.getText()).equals("")) {
                    Message newMsg = new Message(String.valueOf(etMessage.getText()),
                            new Date(), MainActivity.myAcc.getLogin(), to.getLogin());
                    etMessage.setText("");
                    myRef.push().setValue(newMsg);
                }
            }
        });
    }
}
