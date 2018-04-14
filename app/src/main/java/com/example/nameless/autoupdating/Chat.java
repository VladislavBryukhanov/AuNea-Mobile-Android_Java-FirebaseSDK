package com.example.nameless.autoupdating;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

public class Chat extends AppCompatActivity {

    private final int REQUEST_GALLERY = 100;

    private ImageButton btnSend, btnAffixFile;
    private EditText etMessage;
    private ListView lvMessages;
    private MessagesAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private ArrayList<Message> messages;
    private String toUser;

    private boolean keyboardListenerLocker = false;
    private boolean dialogFound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnSend = findViewById(R.id.btnSend);
        btnAffixFile = findViewById(R.id.btnAffixFile);
        etMessage = findViewById(R.id.etMessage);
        lvMessages = findViewById(R.id.lvMessages);

        messages = new ArrayList<>();
        Intent intent = getIntent();
        toUser = intent.getStringExtra("to");
        adapter = new MessagesAdapter(this, messages);
        lvMessages.setAdapter(adapter);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Messages");

        setTitle(toUser);
//        setListenerForScrollWhenKeyboarOpened();
        lvMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        lvMessages.setStackFromBottom(false);
//todo с помощью запроса получить название таблицы, которае содержит подстроку - логин нашего профиля, вместо поиска через листенер
//todo time to live for messages
//todo paging/cache
//todo authorise, cookie, security for dialogs
//todo оптимизация алгоритма выборки, разобраться какой расход трафика при выборке, что такое датаснэпшот, является ли он полностью готовым пришедшим с сервера пакетом данных или делает динамические запросы на выборку
//todo сохранение состояниея при повороте экрана
//todo каскадное удаление
//todo add services
//todo add message notify
// todo last online
//todo media sending


        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getChildrenCount() == 0) {
                            myRef = myRef.push();
                            myRef.child("listener1").setValue(Authentification.myAcc.getLogin());
                            myRef.child("listener2").setValue(toUser);
                            myRef = myRef.child("content");
                        } else {
                            String myLogin = Authentification.myAcc.getLogin();
                            for(DataSnapshot data : dataSnapshot.getChildren()) {
                                String listener1 = (String)data.child("listener1").getValue();
                                String listener2 = (String)data.child("listener2").getValue();
                                if (((listener1.equals(myLogin)) && (listener2.equals(toUser)))
                                        || ((listener2).equals(myLogin) && (listener1).equals(toUser))) {
                                    myRef = myRef.child(data.getKey()).child("content");
                                    dialogFound = true;
                                }
                            }
                            if (!dialogFound) {
                                myRef = myRef.push();
                                myRef.child("listener1").setValue(Authentification.myAcc.getLogin());
                                myRef.child("listener2").setValue(toUser);
                                myRef = myRef.child("content");
                            }
                        }

                        myRef.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                messages.add(dataSnapshot.getValue(Message.class));
                                adapter.notifyDataSetChanged();
//                                lvMessages.setSelection(adapter.getCount() - 1);
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

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (String.valueOf(etMessage.getText()).trim().length() > 0) {
                    btnAffixFile.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);
                } else {
                    btnAffixFile.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.GONE);
                }
            }
        });

        btnAffixFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GALLERY);
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!String.valueOf(etMessage.getText()).equals("")) {
                    Message newMsg = new Message(String.valueOf(etMessage.getText()), null,
                            new Date(), Authentification.myAcc.getLogin(), toUser);
                    myRef.push().setValue(newMsg);
                    etMessage.setText("");
                }
            }
        });
    }

    @Override
    protected void onStop() {
        startService(new Intent(getApplicationContext(), NotifyService.class));
        super.onStop();
    }

    @Override
    protected void onStart() {
        stopService(new Intent(getApplicationContext(), NotifyService.class));
        super.onStart();
    }

    @Override
    protected void onResume() {
        stopService(new Intent(getApplicationContext(), NotifyService.class));
        super.onResume();
    }

    //фокус в конец диалога при открытии клавиатуры
 /*   public void setListenerForScrollWhenKeyboarOpened() {
        final View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > 200 && !keyboardListenerLocker) { // 99% of the time the height diff will be due to a keyboard.
                    lvMessages.setSelection(adapter.getCount() - 1);
                    keyboardListenerLocker = true;
                } else if (heightDiff < 200 && keyboardListenerLocker) {
                    keyboardListenerLocker = false;
                }
            }
        });
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference gsReference = storage.getReferenceFromUrl(
                    "gs://messager-d15a0.appspot.com/");

            Uri file = data.getData();
            StorageReference riversRef = gsReference.child(Authentification.myAcc
                    .getLogin() + "/" + file.getLastPathSegment());
            UploadTask uploadTask = riversRef.putFile(file);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(Chat.this, taskSnapshot.
                            getDownloadUrl().toString(), Toast.LENGTH_SHORT).show();

                    Message newMsg = new Message(String.valueOf(etMessage.getText()), taskSnapshot.getDownloadUrl().toString(),
                            new Date(), Authentification.myAcc.getLogin(), toUser);
                    myRef.push().setValue(newMsg);
                }
            });

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(Chat.this, ":c", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
