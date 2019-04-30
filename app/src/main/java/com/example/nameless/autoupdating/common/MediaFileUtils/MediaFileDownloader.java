package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.common.AudioTrackUI;
import com.example.nameless.autoupdating.activities.Settings;
import com.example.nameless.autoupdating.adapters.MessagesAdapter;
import com.example.nameless.autoupdating.common.VideoUI;
import com.example.nameless.autoupdating.models.Message;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class MediaFileDownloader implements DownloadingFinish {

    public static final String UNDEFINED_TYPE = "undefined";
    public static final String AUDIO_TYPE = "audio";
    public static final String IMAGE_TYPE = "image";
    public static final String VIDEO_TYPE = "video";

    private File targetFile;
    private File basePath;
    private Context parentContext;
    private LinearLayout parentLayout;
    private MessagesAdapter adapterInterface;
    private Message message;
    private String fileType;
    private String fileUrl;

    private ImageView bmImage;
    private ProgressBar pbLoading;
    private LinearLayout audioUI;
    private FrameLayout videoUI;

    public MediaFileDownloader(
            Context parentContext,
            LinearLayout parentLayout,
            Message message,
            MessagesAdapter adapterInterface) {

        this.parentContext = parentContext;
        this.parentLayout = parentLayout;
        this.message = message;
        this.adapterInterface = adapterInterface;
        this.fileType = message.getFileType();
        this.fileUrl = message.getFileUrl();
    }

    private void createItemsLayout() {
        switch (fileType) {
            case IMAGE_TYPE: {
                createImageFileLayout();
                break;
            }
            case AUDIO_TYPE: {
                createAudioFileLayout();
                break;
            }
            case VIDEO_TYPE: {
                createVideoFileLayout();
                break;
            }
            default: {
                createUndefinedFileLayout();
            }
        }
    }

    //TODO undefined UI - button for downloading resource
    private void createUndefinedFileLayout() {
        bmImage = new ImageView(parentContext);
        bmImage.setVisibility(View.GONE);
        bmImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        pbLoading = new ProgressBar(parentContext);
        pbLoading.setIndeterminate(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
        bmImage.setLayoutParams(params);
        pbLoading.setLayoutParams(params);
        parentLayout.addView(bmImage);
        parentLayout.addView(pbLoading);
    }

    private void createImageFileLayout() {
        String fileMediaSides = message.getFileMediaSides();

        bmImage = new ImageView(parentContext);
        bmImage.setVisibility(View.GONE);
        bmImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        pbLoading = new ProgressBar(parentContext);
        pbLoading.setIndeterminate(true);

        setMediaItemSize(fileMediaSides, pbLoading, bmImage);
        parentLayout.addView(bmImage);
        parentLayout.addView(pbLoading);
    }

    private void createVideoFileLayout() {
        String fileMediaSides = message.getFileMediaSides();

        videoUI = new VideoUI(parentContext);
        bmImage = videoUI.findViewById(R.id.vpPreview);
        pbLoading = videoUI.findViewById(R.id.vpPreloader);

        setMediaItemSize(fileMediaSides, pbLoading, bmImage);
        parentLayout.addView(videoUI);
    }

    private void createAudioFileLayout() {
        audioUI = new AudioTrackUI(parentContext);
        parentLayout.addView(audioUI);
    }

    private void setMediaItemSize(String resolution, ProgressBar loading, ImageView img) {
        Point screenSize = new Point();
        WindowManager wm = (WindowManager) parentContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(screenSize);
        screenSize.x *= 0.5;

        int imageWidth = Integer.parseInt(resolution.split("x")[0]);
        int imageHeight = Integer.parseInt(resolution.split("x")[1]);
        double scale = (double) imageWidth / screenSize.x;
        if(imageWidth > screenSize.x) {
            imageWidth = screenSize.x;
            imageHeight /= scale;
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageWidth, imageHeight);
        loading.setLayoutParams(params);
        img.setLayoutParams(params);
    }

    public void downloadFileByUrl() {
        // TODO лейату создается 2-3 (инит, окончание загрузки-колбек 3-й смена статуса месседжа на прочитанный)
        // раза и из за этого дергается прогресс бар (из за пересоздания разный результат сетится)
        createItemsLayout();

        // if downloading in progress
        if (FilesMemoryCache.filesLoadingInProgress.indexOf(fileUrl) != -1) {
            return;
        }
        // if image cached set up in view because async task has delay before setting
        setCachedImage();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference fileReference = storage.getReferenceFromUrl(fileUrl);

        SharedPreferences settings = parentContext.getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE);
        String storage_mode = settings.getString(Settings.STORAGE_MODE, Settings.CACHE_STORAGE);

        basePath = parentContext.getCacheDir();
        if (storage_mode.equals(Settings.LOCAL_STORAGE)) {
            basePath = new File(Environment.getExternalStorageDirectory() + "/AUMessanger/");
            if (!basePath.exists()) {
                basePath.mkdirs();
            }
        }

        targetFile = new File(basePath, fileReference.getName());

        if (targetFile.exists()) {
            handleMediaFile();
        } else {
            FilesMemoryCache.filesLoadingInProgress.add(fileUrl);

            if (fileType.equals(VIDEO_TYPE)) {
//                new Thread(this::downloadVideoPreview).start();
                handleVideoFile();
                return;
            }

            fileReference.getFile(targetFile)
                .addOnSuccessListener(taskSnapshot -> finishDownloadingCallback());
        }
    }

    private void setCachedImage() {
        Bitmap image = FilesMemoryCache.memoryCache.get(fileUrl);
        if (image != null && fileType.equals(IMAGE_TYPE)) {
            bmImage.setImageBitmap(image);
            bmImage.setVisibility(View.VISIBLE);
            pbLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void finishDownloadingCallback() {
        FilesMemoryCache.filesLoadingInProgress.remove(fileUrl);
        // impossible use "callback" because adapter regularly reset all
        // layouts and after downloading old layout may will be removed
        adapterInterface.notifyDataSetChanged();
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
            case VIDEO_TYPE: {
                handleVideoFile();
                break;
            }
            default: {
                handleUndefinedFile();
            }
        }
    }

    private void handleUndefinedFile() {
        ImageComponent imageComponent = new ImageComponent(parentContext, targetFile, fileUrl, bmImage, pbLoading);
        MediaFileHandler downloadTask = new MediaFileHandler(
                imageComponent, parentContext, UNDEFINED_TYPE);
        downloadTask.execute();
    }

    private void handleImageFile() {
        ImageComponent imageComponent = new ImageComponent(parentContext, targetFile, fileUrl, bmImage, pbLoading);
        MediaFileHandler downloadTask = new MediaFileHandler(
                imageComponent, parentContext, IMAGE_TYPE);
        downloadTask.execute();
    }

    private void handleAudioFile() {
        AudioComponent audioComponent = new AudioComponent(parentContext, targetFile, fileUrl, audioUI);
        MediaFileHandler downloadTask = new MediaFileHandler(
                audioComponent, parentContext, AUDIO_TYPE);
        downloadTask.execute();
    }

    private void handleVideoFile() {
        VideoComponent videoComponent = new VideoComponent(parentContext, basePath, targetFile, fileUrl, videoUI, this);
        MediaFileHandler downloadTask = new MediaFileHandler(
                videoComponent, parentContext, VIDEO_TYPE);
        downloadTask.execute();
    }
}
