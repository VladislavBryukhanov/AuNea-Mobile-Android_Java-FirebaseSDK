package com.example.nameless.autoupdating;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class Settings extends AppCompatActivity {

    public static final String APP_PREFERENCES = "preferences";
    public static final String IS_NOTIFY_ENABLED = "IS_NOTIFY_ENABLED";
    public static final String IS_LOCATION_ENABLED  = "IS_LOCATION_ENABLED";
    private SharedPreferences settings;

    private CircleImageView avatar;
    private EditText etLogin, etNickname, etBio;
    private Switch cbLocation, cbNotify;
    private MenuItem mSave;
    private boolean isNewAvatar = false;
    private Uri avatarImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("");

        avatar = findViewById(R.id.profile_image);
        etLogin = findViewById(R.id.etLogin);
        etNickname = findViewById(R.id.etNickname);
        etBio = findViewById(R.id.etBio);
        cbLocation = findViewById(R.id.cbLocation);
        cbNotify = findViewById(R.id.cbNotify);

        settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        /*boolean test = settings.contains(IS_NOTIFY_ENABLED);
        String tt = settings.getString(IS_NOTIFY_ENABLED, "");
        if(settings.contains(IS_NOTIFY_ENABLED)) {
            Toast.makeText(this, settings.getString(IS_NOTIFY_ENABLED, ""), Toast.LENGTH_SHORT).show();
        }*/
        cbNotify.setChecked(settings.getBoolean(IS_NOTIFY_ENABLED, false));
        cbLocation.setChecked(settings.getBoolean(IS_LOCATION_ENABLED, false));

        etLogin.setText(UserList.myAcc.getLogin());
        etNickname.setText(UserList.myAcc.getNickname());
        etBio.setText(UserList.myAcc.getBio());

        if(UserList.myAcc.getAvatar() != null) {
            avatar.setImageBitmap(BitmapFactory.decodeFile(UserList.myAcc.getAvatar()));
        }

        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), Chat.PICKFILE_RESULT_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == Chat.PICKFILE_RESULT_CODE) {
                isNewAvatar = true;
                avatarImage = data.getData();
/*                try {
                    UserList.myAcc.setAvatar(MediaStore.Images.Media.getBitmap(this.getContentResolver(), avatarImage));
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

//                UserList.myAcc.setAvatar(avatarImage);
                avatar.setImageURI(avatarImage);
            }
        }

    }

    private void setNewAvatar(final DatabaseReference myRef, final DataSnapshot data) {
        mSave.setVisible(false);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference;
        if(UserList.myAcc.getAvatarUrl() != null) {
            gsReference = storage.getReferenceFromUrl(UserList.myAcc.getAvatarUrl());
            gsReference.delete(); // Remove old avatar
        }


        gsReference = storage.getReferenceFromUrl("gs://messager-d15a0.appspot.com/");
        StorageReference riversRef = gsReference.child(UserList.myAcc
                .getUid() + "/Avatar/" + java.util.UUID.randomUUID());
        UploadTask uploadTask = riversRef.putFile(avatarImage);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                UserList.myAcc.setAvatarUrl(taskSnapshot.getDownloadUrl().toString());
                myRef.child(data.getKey()).child("avatarUrl").setValue(UserList.myAcc.getAvatarUrl());

                DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(avatar, UserList.myAcc);
                downloadTask.execute(UserList.myAcc.getAvatarUrl());
                Settings.this.finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        mSave = menu.findItem(R.id.mSave);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.mSave: {
                Pattern pattern = Pattern.compile("[!@#$%^&*()_]");
                Matcher matcher = pattern.matcher(etNickname.getText().toString());
                if(matcher.find() || etNickname.getText().toString().length() < 3) {
                    Toast.makeText(this, "Sorry, this nickname is invalid", Toast.LENGTH_SHORT).show();
                    break;
                }

                final FirebaseDatabase database = FirebaseDatabase.getInstance();
                final DatabaseReference myRef = database.getReference("Users");

                Query getNickname = database.getReference("Users").orderByChild("nickname").equalTo(etNickname.getText().toString());
                getNickname.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean isMyNickname = false;
                        if(dataSnapshot.exists()) {
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                String currentUid = (String) (data.child("uid").getValue());
                                if (currentUid != null && currentUid.equals(UserList.myAcc.getUid())) {
                                    isMyNickname = true;
                                }
                            }
                        }
                        if(!dataSnapshot.exists() || isMyNickname) {
                            Query getUser = database.getReference("Users").orderByChild("uid").equalTo(UserList.myAcc.getUid());
                            getUser.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for(DataSnapshot data : dataSnapshot.getChildren()) {
                                        UserList.myAcc.setBio(etBio.getText().toString());
                                        UserList.myAcc.setLogin(etLogin.getText().toString());
                                        UserList.myAcc.setNickname(etNickname.getText().toString());

                                        myRef.child(data.getKey()).child("login").setValue(UserList.myAcc.getLogin());
                                        myRef.child(data.getKey()).child("bio").setValue(UserList.myAcc.getBio());
                                        myRef.child(data.getKey()).child("nickname").setValue(UserList.myAcc.getNickname());

                                        SharedPreferences.Editor prefs = settings.edit();
                                        prefs.putBoolean(IS_NOTIFY_ENABLED, cbNotify.isChecked());
                                        prefs.putBoolean(IS_LOCATION_ENABLED, cbLocation.isChecked());
                                        prefs.apply();

                                        if(isNewAvatar) {
                                            setNewAvatar(myRef, data);
                                        } else {
                                            Settings.this.finish();
                                        }

                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {}
                            });
                        } else {
                            Toast.makeText(Settings.this, "This nickname already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });


                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
