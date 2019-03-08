package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.AudioTrackUI;
import com.example.nameless.autoupdating.activities.Settings;
import com.example.nameless.autoupdating.adapters.MessagesAdapter;
import com.example.nameless.autoupdating.models.Message;
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
    private LinearLayout parentLayout;
    private MessagesAdapter adapterInterface;
    private Message message;
    private String fileType;
    private String fileUrl;

    private ImageView bmImage;
    private ProgressBar pbLoading;
    private LinearLayout audioUI;

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
/*            case VIDEO_TYPE: {
                return setVideoFile(url);
            }*/
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

    private void createAudioFileLayout() {
        parentLayout.findViewById(R.id.audioButton);

        audioUI = new AudioTrackUI(parentContext, null);
        parentLayout.addView(audioUI);

        ImageView audioButton = audioUI.findViewById(R.id.audioButton);
        ProgressBar audioPbLoading = audioUI.findViewById(R.id.pbLoading);
        TextView timeDuration = audioUI.findViewById(R.id.tvTime);

        timeDuration.setText("Loading...");
        audioPbLoading.setVisibility(View.VISIBLE);
        audioButton.setVisibility(View.GONE);
        audioUI.setVisibility(View.VISIBLE);
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
            FilesMemoryCache.filesLoadingInProgress.add(fileUrl);
            fileReference.getFile(targetFile).addOnSuccessListener(taskSnapshot -> {
                FilesMemoryCache.filesLoadingInProgress.remove(fileUrl);
                // impossible use "callback" because adapter regularly reset all
                // layouts and after downloading old layout may will be removed
                adapterInterface.notifyDataSetChanged();
            });
        }
    }

    private void setCachedImage() {
        Bitmap image = FilesMemoryCache.memoryCache.get(fileUrl);
        if (image != null) {
            bmImage.setImageBitmap(image);
            pbLoading.setVisibility(View.GONE);
            bmImage.setVisibility(View.VISIBLE);
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
