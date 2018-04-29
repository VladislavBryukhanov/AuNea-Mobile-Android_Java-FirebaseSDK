package com.example.nameless.autoupdating;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat extends AppCompatActivity {

//    private static final int REQUEST_GALLERY = 100;
    private static final int PICKFILE_RESULT_CODE = 200;
    private static final int CAMERA_REQUEST = 10;

    private ImageButton btnSend, btnAffixFile;
    private static EditText etMessage;
    private ListView lvMessages;
    private static ImageView ivEdit;
    private static Message messageForEditing;
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
        ivEdit = findViewById(R.id.ivEdit);

        messages = new ArrayList<>();
        Intent intent = getIntent();
        toUser = intent.getStringExtra("to");

//        lvMessages.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                registerForContextMenu(R.menu.message_context_menu);
//            }
//        });


        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Messages");

        setTitle(toUser);
//        setListenerForScrollWhenKeyboarOpened();
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

                        adapter = new MessagesAdapter(getApplicationContext(), etMessage, messages, myRef);
                        lvMessages.setAdapter(adapter);

                        myRef.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                messages.add(new Message(dataSnapshot.getKey(), dataSnapshot.getValue(Message.class)));
                                adapter.notifyDataSetChanged();
//                                lvMessages.setSelection(adapter.getCount() - 1);
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
//                                for (Message msg : messages) {
//                                    if(msg.getUid().equals(dataSnapshot.getKey())) {
//                                        messages.remove(msg);
//                                        adapter.notifyDataSetChanged();
//                                    }
//                                }
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

        /*etMessage.addTextChangedListener(new TextWatcher() {
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
        });*/

        btnAffixFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow(v);
            }
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
                    if (!(String.valueOf(etMessage.getText()).trim()).equals("")) {
                    Message newMsg = new Message(String.valueOf(etMessage.getText()), null,
                            new Date(), Authentification.myAcc.getLogin(), toUser, null);
                    parseMessageContent(newMsg);
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
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == PICKFILE_RESULT_CODE || requestCode == CAMERA_REQUEST) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference gsReference = storage.getReferenceFromUrl(
                        "gs://messager-d15a0.appspot.com/");
                Uri file;
                if(requestCode == CAMERA_REQUEST) {
//                    file = data.getData();
//                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//                    ((Bitmap)data.getExtras().get("data")).compress(Bitmap.CompressFormat.PNG,
//                            100, bytes);
                    String path = MediaStore.Images.Media.insertImage(getApplicationContext()
                            .getContentResolver(), (Bitmap)data.getExtras().get("data"),
                            "Title", null);
                    file = Uri.parse(path);
                } else {
                    file = data.getData();
                }
                String extension = getContentResolver().getType(file);

                final String fileType = extension.split("/")[0];
                extension = "." + extension.split("/")[1];
                Toast.makeText(this, fileType, Toast.LENGTH_SHORT).show();
                StorageReference riversRef = gsReference.child(Authentification.myAcc
                        .getLogin() + "/" + java.util.UUID.randomUUID() + extension); //file.getLastPathSegment()
                UploadTask uploadTask = riversRef.putFile(file);

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Message newMsg = new Message(String.valueOf(etMessage.getText()), taskSnapshot.getDownloadUrl().toString(),
                                new Date(), Authentification.myAcc.getLogin(), toUser, fileType);
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

//    public String getRealPathFromURI (Uri uri) {
//        Cursor cursor = getApplicationContext().getContentResolver().query(uri, {MediaStore.Images.Media.DATA}, null, null, null);
//    }

    public void showPopupWindow(View v) {
//        PopupMenu popupMenu = new PopupMenu(this, v);
//        popupMenu.inflate(R.menu.chose_file_type_menu);
//        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch(item.getItemId()) {
//                    case R.id.mGallery: {
//                        Intent intent = new Intent();
//                        intent.setType("image/*");
//                        intent.setAction(Intent.ACTION_GET_CONTENT);
//                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICKFILE_RESULT_CODE);
//                        break;
//                    }
//                    case R.id.mCamera: {
//                        Toast.makeText(Chat.this, "it is a bad idea, close it", Toast.LENGTH_LONG).show();
//                        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//
//                       /* String fileName = "IMG" +
//                                new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
//                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//
//                        File photoFile = null;
//                        try {
//                            photoFile = File.createTempFile(fileName, ".png", storageDir);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        Uri photoUri = FileProvider.getUriForFile(getApplicationContext(),
//                                getPackageName(), photoFile);
//                        i.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); // Uri.fromFile(out)*/
//                        startActivityForResult(i, CAMERA_REQUEST);
//                        break;
//                    }
//                    case R.id.mFileSystem: {
//                        Intent intent = new Intent();
//                        intent.setType("*/*");
//                        intent.setAction(Intent.ACTION_GET_CONTENT);
//                        startActivityForResult(intent, PICKFILE_RESULT_CODE);
//                        break;
//                    }
//                }
//                return true;
//            }
//        });
//        popupMenu.show();

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
                       /* String fileName = "IMG" +
                                new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                        File photoFile = null;
                        try {
                            photoFile = File.createTempFile(fileName, ".png", storageDir);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Uri photoUri = FileProvider.getUriForFile(getApplicationContext(),
                                getPackageName(), photoFile);
                        i.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); // Uri.fromFile(out)*/
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
        if(messageForEditing != null) {
            messageForEditing = null;
            etMessage.setText("");
            ivEdit.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
