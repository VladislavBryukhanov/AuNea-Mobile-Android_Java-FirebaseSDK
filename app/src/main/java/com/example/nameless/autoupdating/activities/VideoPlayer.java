package com.example.nameless.autoupdating.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.common.ImageFilters;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class VideoPlayer extends AppCompatActivity {

    private VideoView videoView;
    private ImageView bmPreview;
    private ProgressBar pbLoading;
    private RelativeLayout rlPreloader;
    private String videoUrl;
    private File targetFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getSupportActionBar().hide();

        videoView = findViewById(R.id.videoView);
        rlPreloader = findViewById(R.id.rlPreloader);
        bmPreview = findViewById(R.id.bmPreview);
        pbLoading = findViewById(R.id.pbLoading);

        bmPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pbLoading.setIndeterminate(true);
        rlPreloader.bringToFront();

        Intent intent = getIntent();
        String basePath = intent.getStringExtra("basePath");
        videoUrl = intent.getStringExtra("videoUrl");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference fileReference = storage.getReferenceFromUrl(videoUrl);

        File thumbnail = new File(basePath,  fileReference.getName());
        targetFile = new File(basePath,  "video." + fileReference.getName());

        Bitmap thumbnailBitmap = ImageFilters.blureBitmap(this, thumbnail, 22);
        bmPreview.setImageBitmap(thumbnailBitmap);

        if (targetFile.exists()) {
            initVideoPlayer();
        } else {
            fileReference.getFile(targetFile)
                .addOnSuccessListener(taskSnapshot -> initVideoPlayer());
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.abc_fade_out);
    }

    private void initVideoPlayer() {
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoPath(targetFile.getAbsolutePath());

        bmPreview.setVisibility(View.GONE);
        pbLoading.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
//        videoView.setZOrderOnTop(true);
        videoView.start();
    }
}
