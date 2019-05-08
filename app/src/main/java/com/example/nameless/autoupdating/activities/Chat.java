package com.example.nameless.autoupdating.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.DocumentsContract;
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

import com.example.nameless.autoupdating.asyncTasks.DownloadAvatarByUrl;
import com.example.nameless.autoupdating.common.AuthGuard;
import com.example.nameless.autoupdating.models.AuthComplete;
import com.example.nameless.autoupdating.models.ChatActions;
import com.example.nameless.autoupdating.common.FirebaseSingleton;
import com.example.nameless.autoupdating.models.Dialog;
import com.example.nameless.autoupdating.common.FCMManager;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.adapters.MessagesAdapter;
import com.example.nameless.autoupdating.models.ClientToClient;
import com.example.nameless.autoupdating.models.Message;
import com.example.nameless.autoupdating.models.User;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat extends AuthGuard implements ChatActions, AuthComplete {

//    private static final int REQUEST_GALLERY = 100;
    public static final int PICKFILE_RESULT_CODE = 200;
    public static final int CAMERA_REQUEST = 10;

    public static final int MAX_UNDEFINED_FILE_SIZE = 8 * 1024 * 1024;
    public static final int MAX_IMAGE_FILE_SIZE = 8 * 1024 * 1024;
    public static final int MAX_AUDIO_FILE_SIZE = 22 * 1024 * 1024;
    public static final int MAX_VIDEO_FILE_SIZE = 40 * 1024 * 1024;


    private ImageButton btnSend, btnAffixFile, btnStartRec, btnStopRec;
    private ListView lvMessages;
    private EditText etMessage;
    private ImageView ivEdit;
    private Message messageForEditing;
    private MessagesAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference messagesDb, dialogsDb, dialogsRef, messagesRef;
    private String dialogId;
    private int firstUnreadMessageIndex = -1;
    
    private ArrayList<Message> messages;
    private User toUser;

    private boolean keyboardListenerLocker = false;
    private boolean dialogFound;
    private Uri resourceUri;
    private MediaRecorder mediaRecorder;
    private String myUid, myEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_light);

        Intent intent = getIntent();
        toUser = (User)intent.getSerializableExtra("to");

        super.checkAccess(this);
    }

    @Override
    public void onAuthSuccess() {
        setContentView(R.layout.activity_chat);

        btnSend = findViewById(R.id.btnSend);
        btnStartRec = findViewById(R.id.btnStartRec);
        btnStopRec = findViewById(R.id.btnStopRec);
        btnAffixFile = findViewById(R.id.btnAffixFile);
        etMessage = findViewById(R.id.etMessage);
        lvMessages = findViewById(R.id.lvMessages);
        ivEdit = findViewById(R.id.ivEdit);

        myUid = getMyAccount().getUid();
        myEmail = getMyAccount().getEmail();
        
        dialogFound = false;
        btnSend.setEnabled(false);
        btnStartRec.setEnabled(false);
        messages = new ArrayList<>();

        database = FirebaseSingleton.getFirebaseInstanse();
        dialogsDb = database.getReference("Dialogs");
        messagesDb = database.getReference("Messages");

        lvMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        lvMessages.setStackFromBottom(true); // if dialog started first time need false
        setActionBar();


//todo с помощью запроса получить название таблицы, которае содержит подстроку - логин нашего профиля, вместо поиска через листенер
//todo time to live for messages
//todo paging/cache
//todo authorise, cookie, security for dialogs
//todo оптимизация алгоритма выборки, разобраться какой расход трафика при выборке, что такое датаснэпшот, является ли он полностью готовым пришедшим с сервера пакетом данных или делает динамические запросы на выборку
//todo каскадное удаление

        setChatListeners();

        btnAffixFile.setOnClickListener(v -> showPopupWindow(v));

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(String.valueOf(etMessage.getText()).trim()).equals("")) {
                    btnStartRec.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);
                } else {
                    btnSend.setVisibility(View.GONE);
                    btnStartRec.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> {
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
                    timestampNow(), myUid, toUser.getUid(), null, null, false);
            parseMessageContent(newMsg);
//                }
        });

        btnStartRec.setOnClickListener(v -> {
            startRecord();
            etMessage.setEnabled(false);
            btnStartRec.setVisibility(View.GONE);
            btnStopRec.setVisibility(View.VISIBLE);
        });
        btnStopRec.setOnClickListener(v -> {
            cancelRecord();
            etMessage.setEnabled(true);
            btnStopRec.setVisibility(View.GONE);
            btnStartRec.setVisibility(View.VISIBLE);
        });
        btnStopRec.setOnLongClickListener(view -> {
            stopRecord();
            etMessage.setEnabled(true);
            btnStopRec.setVisibility(View.GONE);
            btnStartRec.setVisibility(View.VISIBLE);
            return true;
        });
    }

    @Override
    protected void onStop() {
        FCMManager.setInterlocutor("");
        super.onStop();
    }

    @Override
    protected void onStart() {
        FCMManager.setInterlocutor(toUser.getUid());
        super.onStart();
    }

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
                ClientToClient ctc = new ClientToClient(myUid, toUser.getUid());
                intent.putExtra("dialog", ctc);
                intent.putExtra("action", VoiceCalling.OUTGOING_CALL_ACTION);
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
                        "gs://messager-d15a0.appspot.com");

                Uri file = requestCode == CAMERA_REQUEST ? resourceUri : data.getData();
                String extension = getContentResolver().getType(file);
                final String fileType = extension.split("/")[0];

                if (!validateFileSize(file, fileType)) return;

                extension = "." + extension.split("/")[1];
                String fMS = null;
                if (fileType.equals("image")) {
                    fMS = getImageSides(file);
                } else if (fileType.equals("video")) {
                    fMS = getVideoSides(file);
                }
                final String fileMediaSides = fMS;

                StorageReference riversRef = gsReference.child(
                        myEmail + "/" + fileType + "s/" + java.util.UUID.randomUUID() + extension); //file.getLastPathSegment()
                UploadTask uploadTask = riversRef.putFile(file);

                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    Message newMsg = new Message(
                        String.valueOf(etMessage.getText()),
                        taskSnapshot.getDownloadUrl().toString(),
                        timestampNow(),
                        myUid,
                        toUser.getUid(),
                        fileType,
                        fileMediaSides,
                        false
                    );
                    parseMessageContent(newMsg);
                });

                uploadTask.addOnFailureListener(e -> Toast.makeText(Chat.this, ":c", Toast.LENGTH_SHORT).show());
            }
        }
    }


    private void setChatListeners() {

        Query getChat = dialogsDb.orderByChild("speakers/" + myUid).equalTo(myUid);
        getChat.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Триггери или один раз или на добавление, плодишь листенеры при каждом изменении - т к ласт месседж меняется
                // но в дальнейщем появятся чаты и в них будут динамически появляться юзера и это нужно учитывать
                // сделаю для этого отдельную реализацию и отдельную привязку, так что игнор
                if(dialogFound) {
                    getChat.removeEventListener(this);
                    return;
                }
                // Keep calm dialogs can cached and then will not sync with messages
                dialogsDb.keepSynced(true);

                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    for(DataSnapshot speaker : data.child("speakers").getChildren()) {
                        if(speaker.getValue().equals(toUser.getUid())) {
                            dialogId = data.getKey();
                            dialogsRef = dialogsDb.child(dialogId);
                            messagesRef = messagesDb.child(dialogId);
                            dialogFound = true;
                            break;
                        }
                    }
                }

                // TODO animation and disabling
                btnSend.setEnabled(true);
                btnStartRec.setEnabled(true);

                if (!dialogFound) {
                    return;
                }

                adapter = new MessagesAdapter(Chat.this, messages, messagesRef);
                lvMessages.setAdapter(adapter);

                messagesRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot data : dataSnapshot.getChildren()) {
                            dialogsRef.child("lastMessage").setValue(data.getValue());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
                //TODO replace
                Query unreadMessages = messagesRef.orderByChild("read").equalTo(false);
                unreadMessages.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        dialogsRef.child("unreadCounter").setValue(dataSnapshot.getChildrenCount());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
                //
                messagesRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Message message = new Message(dataSnapshot.getKey(), dataSnapshot.getValue(Message.class));
                        messages.add(message);
                        //TODO invalid for infinite scroll
                        if (firstUnreadMessageIndex == -1 && !message.isRead() && !message.getWho().equals(myUid)) {
                            firstUnreadMessageIndex = messages.size() - 1;
                            lvMessages.setSelection(firstUnreadMessageIndex);
                        }
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
    }

    private void startRecord() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(getApplicationContext().getCacheDir() + "AudioCache.3gp");
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
    }

    private void stopRecord() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference = storage.getReferenceFromUrl(
                "gs://messager-d15a0.appspot.com/");

        final Uri file = Uri.fromFile(new File(getApplicationContext().getCacheDir()
                + "AudioCache.3gp"));

        StorageReference riversRef = gsReference.child(myEmail + "/audioRecords/" + java.util.UUID.randomUUID() + ".3gp");
        UploadTask uploadTask = riversRef.putFile(file);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Message newMsg = new Message(String.valueOf(etMessage.getText()), taskSnapshot.getDownloadUrl().toString(),
                    timestampNow(), myUid, toUser.getUid(), "audio", null, false);
            parseMessageContent(newMsg);

            File fdelete = new File(file.getPath());
            if (fdelete.exists()) {
                fdelete.delete();
            }
        });

        uploadTask.addOnFailureListener(e -> Toast.makeText(Chat.this, ":c", Toast.LENGTH_SHORT).show());
    }

    private void cancelRecord() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        File audioCache = new File(getApplicationContext().getCacheDir()
                + "AudioCache.3gp");
        audioCache.delete();
    }

    private String getImageSides(Uri uri) {
        Bitmap image = null;
        try {
            image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image.getWidth() + "x" + image.getHeight();
    }

    private String getVideoSides(Uri uri) {
        String picturePath = getPath(uri);

        Bitmap image = ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MINI_KIND);
        return image.getWidth() + "x" + image.getHeight();
    }

    public String getPath(Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(this, uri)) {

            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String selection = "_id=?";
            final String[] selectionArgs = new String[] {
                split[1]
            };

            Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;


            return getDataColumn(contentUri, selection, selectionArgs);
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public String getDataColumn(Uri uri, String selection, String[] selectionArgs) {

        String[] projection = { MediaStore.Images.Media.DATA };
        try (Cursor cursor = this.getContentResolver()
                .query(uri, projection, selection, selectionArgs, null)) {
            if (cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(projection[0]);
                return cursor.getString(column_index);
            }
        }
        Toast.makeText(this, "Can't send video", Toast.LENGTH_LONG).show();
        return null;
    }

//    public String getRealPathFromURI (Uri uri) {
//        Cursor cursor = getApplicationContext().getContentResolver().query(uri, {MediaStore.Images.Media.DATA}, null, null, null);
//    }

    private void setActionBar() {
        Query getUser = database.getReference("Users").orderByChild("uid").equalTo(toUser.getUid());
        getUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    User user = data.getValue(User.class);

                    ImageView alienAvatar = findViewById(R.id.alienAvatar);
                    if(user.getAvatarUrl() != null && !user.getAvatarUrl().equals(toUser.getAvatarUrl())) {
                        DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(alienAvatar, toUser, getApplication());
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

        ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater()
                .inflate(R.layout.chat_action_bar, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        ((TextView)findViewById(R.id.tvAlienLogin)).setText(toUser.getLogin());
        ((TextView)findViewById(R.id.tvStatus)).setText(toUser.getStatus());

        ImageView alienAvatar = findViewById(R.id.alienAvatar);
        if(toUser.getAvatarUrl() != null) {
            DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(alienAvatar, toUser, getApplication());
            downloadTask.execute(toUser.getAvatarUrl());
        }
        alienAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AlienPage.class);
            intent.putExtra("to", toUser);
            startActivity(intent);
        });
    }

    private void showPopupWindow(View view) {
        LayoutInflater layoutInflater
                = (LayoutInflater)getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.choose_file_type, null);

        final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        popupView.setOnClickListener(v -> popupWindow.dismiss());
        Button btnCamera = popupView.findViewById(R.id.btnCamera);
        Button btnVideo = popupView.findViewById(R.id.btnVideo);
        Button btnGallery = popupView.findViewById(R.id.btnGallery);
        Button btnDevice = popupView.findViewById(R.id.btnDevice);

        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From camera");

            resourceUri = getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, resourceUri);
            startActivityForResult(intent, CAMERA_REQUEST);
            popupWindow.dismiss();
        });

        btnVideo.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Video");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From camera");

            resourceUri = getContentResolver()
                    .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, resourceUri);
            startActivityForResult(intent, CAMERA_REQUEST);
            popupWindow.dismiss();
        });

        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/* video/*");

//            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(
                Intent.createChooser(intent, "Select Resource"),
                PICKFILE_RESULT_CODE
            );
            popupWindow.dismiss();
        });

        btnDevice.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("*/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICKFILE_RESULT_CODE);
            popupWindow.dismiss();
        });
    }

    private boolean validateFileSize(Uri file, String fileType) {
        int maxSize;

        switch(fileType) {
            case "image": {
                maxSize = MAX_IMAGE_FILE_SIZE;
                break;
            }
            case "audio": {
                maxSize = MAX_AUDIO_FILE_SIZE;
                break;
            }
            case "video": {
                maxSize = MAX_VIDEO_FILE_SIZE;
                break;
            }
            default: {
                maxSize = MAX_UNDEFINED_FILE_SIZE;
            }
        }

        try {
            InputStream fis = getContentResolver().openInputStream(file);
            if (fis.available() > maxSize) {
                String message = MessageFormat.format("Maximum {0} size is {1}MB. You have exceeded this restriction.",
                        fileType, maxSize / (1024 *1024)
                );
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle("Maximal file size exceeded");
                alertBuilder.setMessage(message);
                alertBuilder.setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.cancel());
                AlertDialog alertDialog = alertBuilder.create();
                alertDialog.setCancelable(true);
                alertDialog.show();
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void parseMessageContent(Message message) {
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
            messagesRef.child(uid).setValue(message);
            message.setUid(uid);
            return;
        }

        if (!dialogFound) {
            dialogId = dialogsDb.push().getKey();
            dialogsRef = dialogsDb.child(dialogId);
            messagesRef = messagesDb.child(dialogId);

            HashMap<String, String> speakers = new HashMap<>();
            speakers.put(toUser.getUid(), toUser.getUid());
            speakers.put(myUid, myUid);
            Dialog dialog = new Dialog(message, 1, speakers, true);
            dialogsRef.setValue(dialog);
//            dialogFound = true;
        }

        messagesRef.push().setValue(message);
        dialogsRef.child("notify").setValue(true);
        etMessage.setText("");
    }

    @Override
    public void onEdit(Message message) {
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

    private long timestampNow() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime();
    }
}
//todo в уведомлениях закрепить уведомление о том что сейчас происходит звонок, по клику а нем звонок завершать или переходить в соответствующий диалог
//блокировать возможность звонить во врмея разговора
//при входящем звонке вызывать диалог, при клике по ок происходит то же что и при нажати пн меню call
//сделать таймаут в 10 сек для звонка, через 10 тсек если он не был принят то сбрасываем
//голосовое оповещание для звонка