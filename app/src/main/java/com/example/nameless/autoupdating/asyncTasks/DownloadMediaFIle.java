package com.example.nameless.autoupdating.asyncTasks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.ImageViewer;
import com.example.nameless.autoupdating.activities.VideoPlayer;
import com.example.nameless.autoupdating.adapters.MessagesAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by nameless on 13.06.18.
 */

public class DownloadMediaFIle extends AsyncTask<String, Void, Bitmap> {

    public static final String MUSIC_TYPE = "audio";
    public static final String IMAGE_TYPE = "image";
    public static final String VIDEO_TYPE = "video";

    private ImageView bmImage;
    private ProgressBar pbLoading;
    private Context parentContext;
    private String fileType;

    public DownloadMediaFIle(ImageView bmImage, ProgressBar pbLoading, Context parentContext, String fileType) {
        this.bmImage = bmImage;
        this.parentContext = parentContext;
        this.pbLoading = pbLoading;
        this.fileType = fileType;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        return setFileProperties(strings[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        if(bmp != null) {
            bmImage.setImageBitmap(bmp);
            pbLoading.setVisibility(View.GONE);
            bmImage.setVisibility(View.VISIBLE);
        }
    }

    private Bitmap setFileProperties(String url) {
        switch (fileType) {
            case IMAGE_TYPE: {
                return  downloadFileByUrl(url, IMAGE_TYPE);
            }
            case MUSIC_TYPE: {
                return  downloadFileByUrl(url, MUSIC_TYPE);
//                return setAudioFile(url);
            }
            case VIDEO_TYPE: {
                return setVideoFile(url);
            }
            default: {
                return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(parentContext.getResources(),
                    R.drawable.file), 160, 160, true);
            }
        }
    }

    private Bitmap downloadFileByUrl(final String url, String type) {
/*        if (imageCollection.get(audioFile.png) != null) {
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
*//*                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uriForIntentCollection.get(audioFile.png), "image*//**//*");
                    ma.startActivity(intent);*//*
                    Intent intent = new Intent(ma, ImageViewer.class);
                    intent.putExtra("bitmap", uriForIntentCollection.get(audioFile.png));
                    ma.startActivity(intent);
                }
            });
            return imageCollection.get(audioFile.png);
        }*/

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference fileReference = storage.getReferenceFromUrl(url);

//        File file = null;
//        try {
        //.createTempFile("images", "jpg");

//            file = File.createTempFile(fileReference.getName(), null,
//                    getContext().getCacheDir());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        fileReference.getFile(file);
//        String path = getContext().getCacheDir().getAbsolutePath() + file.getName();

        final File path = new File(Environment.getExternalStorageDirectory()
                + "/AUMessanger/");
        if(!path.exists()) {
            path.mkdir();
        }

        final File file = new File(path, fileReference.getName());

//        if(imageCollection.containsKey(url)) {
//            return  setImageOnClickListener(file.getPath(), url);
//        }

        if (!file.exists()) {
            fileReference.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    DownloadMediaFIle downloadTask = new DownloadMediaFIle(
                            bmImage, pbLoading, parentContext, fileType);
                    downloadTask.execute(url);
                }
            });
            return null;
        } else {
            if(type.equals(IMAGE_TYPE)) {
                return setImageProperties(file.getPath());
            } else {
                return setAudioProperties(file.getPath(), url);
            }
        }
    }

    private Bitmap setImageProperties(final String path) {

        Bitmap image = BitmapFactory.decodeFile(path);
        if(path == null) {
            return null;
        }

//        imageCollection.put(url, image);
        setImageOnClickListener(path);
        return image;
    }

    private void setImageOnClickListener(final String path) {
        bmImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(parentContext, ImageViewer.class);
                intent.putExtra("bitmap", Uri.parse(path));
                parentContext.startActivity(intent);
            }
        });
    }

    private Bitmap setAudioProperties(final String path, final String url) {
        bmImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(MessagesAdapter.mediaPlayer.isPlaying() && url.equals(MessagesAdapter.runningAudio.first)) {
//                        MessagesAdapter.mediaPlayer.pause();
                        stopTrack();
                        bmImage.setImageDrawable(ResourcesCompat.getDrawable(parentContext.getResources(), R.drawable.audio_play_button, null));
                    } else {
                        stopTrack();
                        MessagesAdapter.mediaPlayer.setDataSource(path);
                        MessagesAdapter.mediaPlayer.prepare();
                        MessagesAdapter.mediaPlayer.start();
                        MessagesAdapter.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                stopTrack();
                                bmImage.setImageDrawable(ResourcesCompat.getDrawable(parentContext.getResources(), R.drawable.audio_play_button, null));
                            }
                        });
                        MessagesAdapter.runningAudio = new Pair<>(url, bmImage);
                        bmImage.setImageDrawable(ResourcesCompat.getDrawable(parentContext.getResources(), R.drawable.audio_pause_button, null));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if(MessagesAdapter.runningAudio != null && url.equals(MessagesAdapter.runningAudio.first)) {
            return drawableItemToBitmap(R.drawable.audio_pause_button);
        } else {
            return drawableItemToBitmap(R.drawable.audio_play_button);
        }
     }

    private void stopTrack() {
        if(MessagesAdapter.runningAudio != null) {
            MessagesAdapter.mediaPlayer.stop();
            MessagesAdapter.mediaPlayer.release();
            MessagesAdapter.mediaPlayer = new MediaPlayer();
            MessagesAdapter.runningAudio.second.setImageDrawable(ResourcesCompat.getDrawable(parentContext.getResources(), R.drawable.audio_play_button, null));
            MessagesAdapter.runningAudio = null;
        }
    }


    private Bitmap setVideoFile(final String url) {
        bmImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(parentContext, VideoPlayer.class);
                intent.putExtra("videoUrl", url);
                parentContext.startActivity(intent);
            }
        });
//        Bitmap img = ThumbnailUtils.createVideoThumbnail(url, MediaStore.Video.Thumbnails.MINI_KIND);

        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(parentContext.getResources(),
                R.drawable.file), 100, 100, true);
    }

    private Bitmap drawableItemToBitmap(int img) {
        Drawable drawable = ResourcesCompat.getDrawable(parentContext.getResources(), img, null);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
