package com.example.nameless.autoupdating.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.example.nameless.autoupdating.generalModules.AppCompatActivityWithInternetStatusListener;
import com.example.nameless.autoupdating.asyncTasks.DownloadAvatarByUrl;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.generalModules.GlobalMenu;
import com.example.nameless.autoupdating.models.User;

import java.util.concurrent.ExecutionException;

import de.hdodenhof.circleimageview.CircleImageView;

public class AlienPage extends GlobalMenu {

    private TextView tvLogin, tvNickname, tvStatus, tvBio;
    private CircleImageView avatar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alien_page);
        setTitle("");

        Intent intent = getIntent();
        final User user = (User)intent.getSerializableExtra("to");

        avatar = findViewById(R.id.alienAvatar);
        tvLogin = findViewById(R.id.tvLogin);
        tvNickname = findViewById(R.id.tvNickname);
        tvStatus = findViewById(R.id.tvStatus);
        tvBio = findViewById(R.id.tvBio);

        tvLogin.setText(user.getLogin());
        if(user.getNickname() != null && user.getNickname().length() > 0) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
