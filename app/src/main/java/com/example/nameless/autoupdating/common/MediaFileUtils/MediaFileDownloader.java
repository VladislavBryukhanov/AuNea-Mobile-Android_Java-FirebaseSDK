package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.nameless.autoupdating.activities.Settings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class MediaFileDownloader {

    public static final String UNDEFINED_TYPE = "undefined";
    public static final String AUDIO_TYPE = "audio";
    public static final String IMAGE_TYPE = "image";
    public static final String VIDEO_TYPE = "video";

    private File targetFile;
    private Context parentContext;
    private String fileType;
    private ImageView bmImage;
    private ProgressBar pbLoading;

    private LinearLayout audioUI;
    private String fileUrl;

    public MediaFileDownloader(
            ImageView bmImage,
            ProgressBar pbLoading,
            Context parentContext,
            String fileType,
            String fileUrl) {

        this.bmImage = bmImage;
        this.pbLoading = pbLoading;
        this.parentContext = parentContext;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
    }

    public MediaFileDownloader(
            LinearLayout audioUI,
            Context parentContext,
            String fileType,
            String fileUrl) {

        this.audioUI = audioUI;
        this.parentContext = parentContext;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
    }

    public void downloadFileByUrl() {
        // if downloading in progress
        if (ImagesMemoryCache.filesLoadingInProgress.indexOf(fileUrl) != -1) {
            return;
        }
        // if image cached set up in view because async task has delay before setting
        setChachedImage();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference fileReference = storage.getReferenceFromUrl(fileUrl);

        File path = parentContext.getCacheDir();

        SharedPreferences settings = parentContext.getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (settings.getString(Settings.STORAGE_MODE, Settings.CACHE_STORAGE).equals(Settings.LOCAL_STORAGE)) {
            path = new File(Environment.getExternalStorageDirectory()
                    + "/AUMessanger/");
            if (!path.exists()) {
                path.mkdirs();
            }
        }

        targetFile = new File(path, fileReference.getName());

        if (targetFile.exists()) {
            handleMediaFile();
        } else {
            ImagesMemoryCache.filesLoadingInProgress.add(fileUrl);
            fileReference.getFile(targetFile).addOnSuccessListener(taskSnapshot -> handleMediaFile());
        }
    }

    private void handleMediaFile() {
        switch (fileType) {
            case IMAGE_TYPE: {
                handleImageFile();
                break;
            }
            case AUDIO_TYPE: {
                handleAudioFile();
                break;
            }
/*            case VIDEO_TYPE: {
                return setVideoFile(url);
            }*/
            default: {
                handleUndefinedFile();
            }
        }
    }

    private void setChachedImage() {
        Bitmap image = ImagesMemoryCache.memoryCache.get(fileUrl);
        if (image != null) {
            bmImage.setImageBitmap(image);
            pbLoading.setVisibility(View.GONE);
            bmImage.setVisibility(View.VISIBLE);
        }
    }

    private void handleUndefinedFile() {
        ImageComponent imageComponent = new ImageComponent(parentContext, targetFile, bmImage, pbLoading, fileUrl);
        MediaFileHandler downloadTask = new MediaFileHandler(
                imageComponent, parentContext, UNDEFINED_TYPE, fileUrl);
        downloadTask.execute();
    }

    private void handleImageFile() {
        ImageComponent imageComponent = new ImageComponent(parentContext, targetFile, bmImage, pbLoading, fileUrl);
        MediaFileHandler downloadTask = new MediaFileHandler(
                imageComponent, parentContext, IMAGE_TYPE, fileUrl);
        downloadTask.execute();
    }

    private void handleAudioFile() {
        AudioComponent audioComponent = new AudioComponent(parentContext, audioUI, fileUrl, targetFile);
        MediaFileHandler downloadTask = new MediaFileHandler(
                audioComponent, parentContext, AUDIO_TYPE, fileUrl);
        downloadTask.execute();
    }

/*    private Bitmap setVideoFile(final String url) {
        bmImage.setOnClickListener(v -> {
            Intent intent = new Intent(parentContext, VideoPlayer.class);
            intent.putExtra("videoUrl", url);
            parentContext.startActivity(intent);
        });
//        Bitmap img = ThumbnailUtils.createVideoThumbnail(url, MediaStore.Video.Thumbnails.MINI_KIND);

        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(parentContext.getResources(),
                R.drawable.file), 100, 100, true);
    }*/
}
