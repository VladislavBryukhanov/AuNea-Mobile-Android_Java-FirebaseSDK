package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.VideoPlayer;

public class VideoComponent {

    private Context parentContext;
    private File mediaFile;
    private File basePath;
    private String fileUrl;
    private ImageView vpPreview;
    private ProgressBar vpPreloader;
    private RelativeLayout rlController;
    private ImageView vpPlay;
    private boolean downloadingFinished;
    private DownloadingFinish finish;

    VideoComponent(
            Context parentContext,
            File basePath,
            File mediaFile,
            String fileUrl,
            FrameLayout videoUi,
            DownloadingFinish finish) {

        this.parentContext = parentContext;
        this.mediaFile = mediaFile;
        this.basePath = basePath;
        this.fileUrl = fileUrl;
        this.finish = finish;

        this.rlController = videoUi.findViewById(R.id.rlController);
        this.vpPreview = videoUi.findViewById(R.id.vpPreview);
        this.vpPreloader = videoUi.findViewById(R.id.vpPreloader);
        this.vpPlay = videoUi.findViewById(R.id.vpPlay);
    }

    Bitmap setVideoProperties() {
        Bitmap image = FilesMemoryCache.memoryCache.get(fileUrl);

        if (image == null) {
            image = BitmapFactory.decodeFile(mediaFile.getPath());
        }

        if (image == null) {
            image = downloadVideoPreview();
        }

//        image = ImageFilters.blureBitmap(parentContext, mediaFile, 12);

        FilesMemoryCache.memoryCache.put(fileUrl, image);
        return image;
    }

    private Bitmap downloadVideoPreview() {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(fileUrl, new HashMap<>());

        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST);
        mediaMetadataRetriever.release();

        try (FileOutputStream os = new FileOutputStream(mediaFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            downloadingFinished = true;
        }
        return bitmap;
    }

    void setVideoUi(Bitmap bmp) {
        if (downloadingFinished) {
            finish.finishDownloadingCallback();
        }

        setVideoOnClickListener();
        vpPreview.setImageBitmap(bmp);
        vpPreloader.setVisibility(View.GONE);
        vpPreview.setVisibility(View.VISIBLE);
        vpPreview.setAdjustViewBounds(true);
        vpPlay.setVisibility(View.VISIBLE);
        rlController.bringToFront();
    }

    private void setVideoOnClickListener() {
        vpPreview.setOnClickListener(v -> {
            Intent intent = new Intent(parentContext, VideoPlayer.class);
            intent.putExtra("videoUrl", fileUrl);
            intent.putExtra("basePath", basePath.getAbsolutePath());
            parentContext.startActivity(intent);
        });
    }
}
