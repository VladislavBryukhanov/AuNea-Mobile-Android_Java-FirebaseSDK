package com.example.nameless.autoupdating;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import de.hdodenhof.circleimageview.CircleImageView;

public class AlienPage extends AppCompatActivity {

    private TextView tvLogin, tvNickname, tvStatus, tvBio;
    private CircleImageView avatar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alien_page);

        Intent intent = getIntent();
        final User user = (User)intent.getSerializableExtra("to");

        avatar = findViewById(R.id.alienAvatar);
        tvLogin = findViewById(R.id.tvLogin);
        tvNickname = findViewById(R.id.tvNickname);
        tvStatus = findViewById(R.id.tvStatus);
        tvBio = findViewById(R.id.tvBio);

        tvLogin.setText(user.getLogin());
        if(user.getNickname().length() > 0) {
            tvNickname.setText('@'+user.getNickname());
        }
        tvStatus.setText(user.getStatus());
        tvBio.setText(user.getBio());

        if(user.getAvatarUrl() != null) {
            DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(avatar, user);
            try {
                downloadTask.execute(user.getAvatarUrl()).get();

                avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent(getApplicationContext(), ImageViewer.class);
                        intent.putExtra("bitmap", Uri.parse(user.getAvatar()));
                        startActivity(intent);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }



    }
}
