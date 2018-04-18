package com.example.nameless.autoupdating;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageViewer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView imgViewer = findViewById(R.id.imgViewer);
        Intent intent = getIntent();
        String path = (intent.getParcelableExtra("bitmap")).toString();

        imgViewer.setImageBitmap(BitmapFactory.decodeFile(path));

    }
}
