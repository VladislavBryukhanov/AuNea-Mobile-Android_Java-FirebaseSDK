package com.example.nameless.autoupdating.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toolbar;

import com.example.nameless.autoupdating.generalModules.AppCompatActivityWithInternetStatusListener;
import com.example.nameless.autoupdating.asyncTasks.DownloadAvatarByUrl;
import com.example.nameless.autoupdating.services.NotifyService;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.adapters.MessagesAdapter;
import com.example.nameless.autoupdating.models.ClientToClient;
import com.example.nameless.autoupdating.models.Message;
import com.example.nameless.autoupdating.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat extends AppCompatActivityWithInternetStatusListener {

//    private static final int REQUEST_GALLERY = 100;
    public static final int PICKFILE_RESULT_CODE = 200;
    public static final int CAMERA_REQUEST = 10;

    private ImageButton btnSend, btnAffixFile, btnStartRec, btnStopRec;
    private static EditText etMessage;
    private ListView lvMessages;
    private static ImageView ivEdit;
    private static Message messageForEditing;
    private MessagesAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private ArrayList<Message> messages;
    private User toUser;

    private boolean keyboardListenerLocker = false;
    private boolean dialogFound = false;
    private FirebaseAuth mAuth;
    private Uri imgUri;
    private MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnSend = findViewById(R.id.btnSend);
        btnStartRec = findViewById(R.id.btnStartRec);
        btnStopRec = findViewById(R.id.btnStopRec);
        btnAffixFile = findViewById(R.id.btnAffixFile);
        etMessage = findViewById(R.id.etMessage);
        lvMessages = findViewById(R.id.lvMessages);
        ivEdit = findViewById(R.id.ivEdit);

        messages = new ArrayList<>();
        Intent intent = getIntent();
        toUser = (User)intent.getSerializableExtra("to");
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Messages");

        setActionBar();
        lvMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        lvMessages.setStackFromBottom(true); // if dialog started first time need false


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
                    myRef.child("listener1").setValue(UserList.myAcc.getUid());
                    myRef.child("listener2").setValue(toUser.getUid());
                    myRef = myRef.child("content");
                } else {
                    String myLogin = mAuth.getUid();
                    for(DataSnapshot data : dataSnapshot.getChildren()) {
                        String listener1 = (String)data.child("listener1").getValue();
                        String listener2 = (String)data.child("listener2").getValue();
                        if (((listener1.equals(myLogin)) && (listener2.equals(toUser.getUid())))
                                || ((listener2).equals(myLogin) && (listener1).equals(toUser.getUid()))) {
                            myRef = myRef.child(data.getKey()).child("content");
                            dialogFound = true;
                        }
                    }
                    if (!dialogFound) {
                        myRef = myRef.push();
                        myRef.child("listener1").setValue(UserList.myAcc.getUid());
                        myRef.child("listener2").setValue(toUser.getUid());
                        myRef = myRef.child("content");
                    }
                }

                adapter = new MessagesAdapter(getApplicationContext(), etMessage, messages, myRef);
                lvMessages.setAdapter(adapter);

                myRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        messages.add(new Message(dataSnapshot.getKey(), dataSnapshot.getValue(Message.class)));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        for (int i=0; i<messages.size(); i++) {
                            if(((messages.get(i)).getUid()).equals(dataSnapshot.getKey())) {
                                messages.set(i, new Message(dataSnapshot.getKey(), dataSnapshot.getValue(Message.class)));
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Iterator<Message> itr = messages.iterator();
                        while(itr.hasNext()) {
                            Message message = itr.next();
                            if(message.getUid().equals(dataSnapshot.getKey())) {
                                itr.remove();
                                adapter.notifyDataSetChanged();
                            }
                        }
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

        btnAffixFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow(v);
            }
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(String.valueOf(etMessage.getText()).trim()).equals("")) {
                    btnSend.setVisibility(View.VISIBLE);
                    btnStartRec.setVisibility(View.GONE);
                } else {
                    btnSend.setVisibility(View.GONE);
                    btnStartRec.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(messageForEditing != null) {
                    messageForEditing.setContent(String.valueOf(etMessage.getText()));
                    parseMessageContent(messageForEditing);
                    messageForEditing = null;
                    etMessage.setText("");
                    ivEdit.setVisibility(View.GONE);
                    return;
                }
//                if (!(String.valueOf(etMessage.getText()).trim()).equals("")) {
                Message newMsg = new Message(String.valueOf(etMessage.getText()), null,
                        new Date(), mAuth.getUid(), toUser.getUid(), null, null);
                parseMessageContent(newMsg);
//                }
            }
        });

        btnStartRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecord();
                etMessage.setEnabled(false);
                btnStartRec.setVisibility(View.GONE);
                btnStopRec.setVisibility(View.VISIBLE);
            }
        });
        btnStopRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
                etMessage.setEnabled(true);
                btnStartRec.setVisibility(View.VISIBLE);
                btnStopRec.setVisibility(View.GONE);
            }
        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main_menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    protected void onStop() {
        if (!UserList.isMyServiceRunning(NotifyService.class, this)) {
            startService(new Intent(this, NotifyService.class));
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        stopService(new Intent(this, NotifyService.class));
        super.onStart();
    }

//    @Override
//    protected void onResume() {
//        stopService(new Intent(getApplicationContext(), NotifyService.class));
//        super.onResume();
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dialog_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.mCall: {
                Intent intent = new Intent(getApplicationContext(), VoiceCalling.class);
                ClientToClient ctc = new ClientToClient(mAuth.getUid(), toUser.getUid());
                intent.putExtra("dialog", ctc);
                intent.putExtra("action", "call");
                startActivity(intent);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == PICKFILE_RESULT_CODE || requestCode == CAMERA_REQUEST) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference gsReference = storage.getReferenceFromUrl(
                        "gs://messager-d15a0.appspot.com/");
                Uri file;
                if(requestCode == CAMERA_REQUEST) {
                    file = imgUri;
                } else {
                    file = data.getData();
                }
                String extension = getContentResolver().getType(file);

                final String fileType = extension.split("/")[0];
                extension = "." + extension.split("/")[1];
                String fMS = null;
                if(fileType.equals("image") || fileType.equals("video")) {
                    fMS = getImageSides(file);
                }
                final String fileMediaSides = fMS;
//                Toast.makeText(this, fileType, Toast.LENGTH_SHORT).show();


                StorageReference riversRef = gsReference.child(UserList.myAcc
                        .getUid() + "/" + java.util.UUID.randomUUID() + extension); //file.getLastPathSegment()
                UploadTask uploadTask = riversRef.putFile(file);

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Message newMsg = new Message(String.valueOf(etMessage.getText()), taskSnapshot.getDownloadUrl().toString(),
                                new Date(), mAuth.getUid(), toUser.getUid(), fileType, fileMediaSides);
                        parseMessageContent(newMsg);
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

    public void startRecord() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory()
                + "/AUMessanger/AudioCache.3gp");
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
    }

    public void stopRecord() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference = storage.getReferenceFromUrl(
                "gs://messager-d15a0.appspot.com/");

        final Uri file = Uri.fromFile(new File(Environment.getExternalStorageDirectory()
                + "/AUMessanger/AudioCache.3gp"));

//        String extension = getContentResolver().getType(file);
//        final String fileType = extension.split("/")[0];
//        extension = "." + extension.split("/")[1];

        StorageReference riversRef = gsReference.child(UserList.myAcc
                .getUid() + "/" + java.util.UUID.randomUUID() + ".3gp");
        UploadTask uploadTask = riversRef.putFile(file);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Message newMsg = new Message(String.valueOf(etMessage.getText()), taskSnapshot.getDownloadUrl().toString(),
                        new Date(), mAuth.getUid(), toUser.getUid(), "audio", null);
                parseMessageContent(newMsg);

                File fdelete = new File(file.getPath());
                if (fdelete.exists()) {
                    fdelete.delete();
                }
            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Chat.this, ":c", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String getImageSides(Uri uri) {
        Bitmap image = null;
        try {
            image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image.getWidth() + "x" + image.getHeight();
    }

//    public String getRealPathFromURI (Uri uri) {
//        Cursor cursor = getApplicationContext().getContentResolver().query(uri, {MediaStore.Images.Media.DATA}, null, null, null);
//    }

    public void setActionBar() {
        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(toUser.getUid());
        getUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    User user = data.getValue(User.class);

                    ImageView alienAvatar = findViewById(R.id.alienAvatar);
                    if(user.getAvatarUrl() != null && !user.getAvatarUrl().equals(toUser.getAvatarUrl())) {
                        DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(alienAvatar, toUser);
                        downloadTask.execute(toUser.getAvatarUrl());
                    }
                    toUser = user;
                    ((TextView)findViewById(R.id.tvStatus)).setText(toUser.getStatus());
                    ((TextView)findViewById(R.id.tvAlienLogin)).setText(toUser.getLogin());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(
                R.layout.chat_action_bar,
                null);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        ((TextView)findViewById(R.id.tvAlienLogin)).setText(toUser.getLogin());
        ((TextView)findViewById(R.id.tvStatus)).setText(toUser.getStatus());

        ImageView alienAvatar = findViewById(R.id.alienAvatar);
        if(toUser.getAvatarUrl() != null) {
            DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(alienAvatar, toUser);
            downloadTask.execute(toUser.getAvatarUrl());
        }
        alienAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AlienPage.class);
                intent.putExtra("to", toUser);
                startActivity(intent);
            }
        });
    }
    public void showPopupWindow(View v) {

        LayoutInflater layoutInflater
                = (LayoutInflater)getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.choose_file_type, null);

        final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
        popupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        Button btnCamera = popupView.findViewById(R.id.btnCamera);
        Button btnGallery = popupView.findViewById(R.id.btnGallery);
        Button btnDevice = popupView.findViewById(R.id.btnDevice);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From camera");
            imgUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            i.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
            startActivityForResult(i, CAMERA_REQUEST);
            popupWindow.dismiss();
            }
        });

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICKFILE_RESULT_CODE);

//             Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            intent.setType("image/*");
//            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICKFILE_RESULT_CODE);
            popupWindow.dismiss();
            }
        });

        btnDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, PICKFILE_RESULT_CODE);
                popupWindow.dismiss();
            }
        });
    }

    public void parseMessageContent(Message message) {
        String msg = message.getContent();
        Pattern urlPattern = Pattern.compile(
                "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d" +
                        ":#@%/;$()~_?\\+-=\\\\\\.&]*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        Matcher matcher = urlPattern.matcher(msg);
        if (matcher.find()) {
            message.setFileType("Url");
        }

        if(message.getUid() != null) {
            String uid = message.getUid();
            message.setUid(null);
            //в базу записываем объект мессадж без uid  тк это поле является название узла в бд и не требуется
            myRef.child(uid).setValue(message);
            message.setUid(uid);
            return;
        }

        myRef.push().setValue(message);
        etMessage.setText("");
    }

    public static void onEdit(Message message) {
        messageForEditing = message;
        etMessage.setText(message.getContent());
        etMessage.setSelection(etMessage.getText().length());
        etMessage.requestFocus();

        ivEdit.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
//        stopCall();
        if(messageForEditing != null) {
            messageForEditing = null;
            etMessage.setText("");
            ivEdit.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
//todo в уведомлениях закрепить уведомление о том что сейчас происходит звонок, по клику а нем звонок завершать или переходить в соответствующий диалог
//блокировать возможность звонить во врмея разговора
//при входящем звонке вызывать диалог, при клике по ок происходит то же что и при нажати пн меню call
//сделать таймаут в 10 сек для звонка, через 10 тсек если он не был принят то сбрасываем
//голосовое оповещание для звонка