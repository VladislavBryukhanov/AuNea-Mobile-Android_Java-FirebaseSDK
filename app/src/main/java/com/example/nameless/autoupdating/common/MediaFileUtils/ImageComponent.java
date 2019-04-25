package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.nameless.autoupdating.activities.ImageViewer;

import java.io.File;

public class ImageComponent {
    private Context parentContext;
    private File mediaFile;
    private ImageView bmImage;
    private ProgressBar pbLoading;
    private String fileUrl;

    ImageComponent(Context parentContext, File mediaFile, String fileUrl, ImageView bmImage, ProgressBar pbLoading) {
        this.parentContext = parentContext;
        this.mediaFile = mediaFile;
        this.bmImage = bmImage;
        this.pbLoading = pbLoading;
        this.fileUrl = fileUrl;
    }

    Bitmap setImageProperties() {
        Bitmap image = FilesMemoryCache.memoryCache.get(fileUrl);

        if (image == null) {
            image = BitmapFactory.decodeFile(mediaFile.getPath());
            FilesMemoryCache.memoryCache.put(fileUrl, image);
        }
        return image;
    }

    void setImageUi(Bitmap bmp) {
        setImageOnClickListener();
        bmImage.setImageBitmap(bmp);
        pbLoading.setVisibility(View.GONE);
        bmImage.setVisibility(View.VISIBLE);
        bmImage.setAdjustViewBounds(true);
    }

    private void setImageOnClickListener() {
        bmImage.setOnClickListener(v -> {
            Intent intent = new Intent(parentContext, ImageViewer.class);
            intent.putExtra("bitmap", Uri.parse(mediaFile.getPath()));
            parentContext.startActivity(intent);
        });
    }
}
