package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.example.nameless.autoupdating.R;

import java.io.File;

public class MediaFileHandler extends AsyncTask<File, Void, Bitmap> {

    private Context parentContext;
    private String fileType;
    private String fileUrl;

    private AudioComponent audioComponent;
    private ImageComponent imageComponent;

    MediaFileHandler(
            ImageComponent imageComponent,
            Context parentContext,
            String fileType,
            String fileUrl) {

        this.imageComponent = imageComponent;
        this.parentContext = parentContext;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
    }

    MediaFileHandler(
            AudioComponent audioComponent,
            Context parentContext,
            String fileType,
            String fileUrl) {

        this.audioComponent = audioComponent;
        this.parentContext = parentContext;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
    }

    @Override
    protected Bitmap doInBackground(File... file) {
        switch (fileType) {
            case MediaFileDownloader.IMAGE_TYPE: {
                return imageComponent.setImageProperties();
            }
            case MediaFileDownloader.AUDIO_TYPE: {
                return audioComponent.setAudioProperties();
            }
/*            case VIDEO_TYPE: {
                return setVideoFile(url);
            }*/
            default: {
                return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                        parentContext.getResources(),
                        R.drawable.file), 160, 160, true);
            }
        }
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        if(bmp != null) {
            switch (fileType) {
                case MediaFileDownloader.IMAGE_TYPE: {
                    imageComponent.setImageUi(bmp);
                    break;
                }
                case MediaFileDownloader.AUDIO_TYPE: {
                    audioComponent.setAudioUI(bmp);
                    break;
                }
        /*        case VIDEO_TYPE: {
                    return setVideoFile(url);
                }
                default: {

                }*/
            }
            ImagesMemoryCache.filesLoadingInProgress.remove(fileUrl);
        }
    }
}
