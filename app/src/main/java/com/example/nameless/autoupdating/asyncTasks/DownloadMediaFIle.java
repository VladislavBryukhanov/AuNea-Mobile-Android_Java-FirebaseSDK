package com.example.nameless.autoupdating.asyncTasks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.ImageViewer;
import com.example.nameless.autoupdating.activities.UserList;
import com.example.nameless.autoupdating.activities.VideoPlayer;
import com.example.nameless.autoupdating.adapters.MessagesAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by nameless on 13.06.18.
 */

public class DownloadMediaFIle extends AsyncTask<String, Void, Bitmap> {

    public static final String MUSIC_TYPE = "audio";
    public static final String IMAGE_TYPE = "image";
    public static final String VIDEO_TYPE = "video";

    private ImageView bmImage;
    private ProgressBar pbLoading;
    private LinearLayout audioUI;
    private Context parentContext;
    private String fileType;


    private int trackDuration;
    private ImageView audioButton;
    private SeekBar trackSeekBar;
    private Boolean isTrackPlaying = false;

    public DownloadMediaFIle(ImageView bmImage,
                             ProgressBar pbLoading,
                             LinearLayout audioUI,
                             Context parentContext,
                             String fileType) {
        this.bmImage = bmImage;
        this.pbLoading = pbLoading;
        this.audioUI = audioUI;
        this.parentContext = parentContext;
        this.fileType = fileType;
        audioButton = audioUI.findViewById(R.id.audioButton);
        trackSeekBar = audioUI.findViewById(R.id.seekBar);
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        return setFileProperties(strings[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        if(bmp != null) {
            if (fileType.equals(MUSIC_TYPE)) {
                ImageView audioButton = audioUI.findViewById(R.id.audioButton);
                TextView timeDuration = audioUI.findViewById(R.id.tvTime);

                DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String time = formatter.format(new Date(trackDuration));

                timeDuration.setText(time);
                audioButton.setImageBitmap(bmp);
                pbLoading.setVisibility(View.GONE);
                audioUI.setVisibility(View.VISIBLE);

                if(isTrackPlaying) {
                    setDurationSeek();
                }

            } else {
                bmImage.setImageBitmap(bmp);
                pbLoading.setVisibility(View.GONE);
                bmImage.setVisibility(View.VISIBLE);
            }
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
                return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                        parentContext.getResources(),
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

   /*     final File path = new File(Environment.getExternalStorageDirectory()
                + "/AUMessanger/");
        if(!path.exists()) {
            path.mkdir();
        }
*/
        final File file = new File(parentContext.getCacheDir(), fileReference.getName());

        Bitmap bitmap = MessagesAdapter.mMemoryCache.get(url);
        if(bitmap != null) {
            setImageOnClickListener(file.getPath());
            return bitmap;
        }

        if (!file.exists()) {
            fileReference.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    DownloadMediaFIle downloadTask = new DownloadMediaFIle(
                            bmImage, pbLoading, audioUI, parentContext, fileType);
                    downloadTask.execute(url);
                }
            });
            return null;
        } else {
            if(type.equals(IMAGE_TYPE)) {
                return setImageProperties(file.getPath(), url);
            } else {
                return setAudioProperties(file.getPath(), url);
            }
        }
    }

    private Bitmap setImageProperties(final String path, String url) {

        //TODO если файл поврежден - перекачать (файл с одинаковым именем но разным размером
        Bitmap image = BitmapFactory.decodeFile(path);
//        if(path == null) {
        if(image == null) {
                return null;
        }
        MessagesAdapter.mMemoryCache.put(url, image);

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
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//        mmr.setDataSource(parentContext.getApplicationContext(), Uri.parse(path));
        mmr.setDataSource(path);
        
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        trackDuration = Integer.parseInt(durationStr);

        trackSeekBar.setMax(trackDuration);
        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(MessagesAdapter.mediaPlayer.isPlaying() && url.equals(
                            MessagesAdapter.runningAudio.first)) {
//                        MessagesAdapter.mediaPlayer.pause();
                        stopTrack(true);

                    } else {
                        stopTrack(false);
                        MessagesAdapter.mediaPlayer.setDataSource(path);
                        MessagesAdapter.mediaPlayer.prepare();
                        MessagesAdapter.mediaPlayer.start();
                        MessagesAdapter.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                stopTrack(true);
                            }
                        });
                        setDurationSeek();
                        audioButton.setImageDrawable(ResourcesCompat.getDrawable(
                                parentContext.getResources(),
                                R.drawable.audio_pause_button, null));
                        MessagesAdapter.runningAudio = new Pair<>(url, audioUI);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if(MessagesAdapter.runningAudio != null && url.equals(MessagesAdapter.runningAudio.first)) {
            isTrackPlaying = true;
            MessagesAdapter.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopTrack(true);
                }
            });
            return drawableItemToBitmap(R.drawable.audio_pause_button);
        } else {
            isTrackPlaying = false;
            return drawableItemToBitmap(R.drawable.audio_play_button);
        }
     }

    private void setDurationSeek() {
        MessagesAdapter.trackDurationHandler = new Handler();
        Thread th = new Thread(new Runnable() {
             @Override
             public void run() {
                 if(MessagesAdapter.mediaPlayer != null){
                     int mCurrentPosition = MessagesAdapter.mediaPlayer.getCurrentPosition();
                     trackSeekBar.setProgress(mCurrentPosition);
                     MessagesAdapter.trackDurationHandler.postDelayed(this, 100);
                 }
             }
         });
         th.start();
        trackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    MessagesAdapter.mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                MessagesAdapter.mediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MessagesAdapter.mediaPlayer.start();
            }
        });
    }

    private void stopTrack(boolean isCurrent) {
        if(MessagesAdapter.runningAudio != null) {
            MessagesAdapter.trackDurationHandler.removeCallbacksAndMessages(null);
            MessagesAdapter.mediaPlayer.stop();
            MessagesAdapter.mediaPlayer.release();
            MessagesAdapter.mediaPlayer = new MediaPlayer();

            if (isCurrent) {
                audioButton.setImageDrawable(ResourcesCompat.getDrawable(
                        parentContext.getResources(),
                        R.drawable.audio_play_button,
                        null));
                trackSeekBar.setProgress(0);
            } else {
                ((SeekBar)MessagesAdapter.runningAudio.second.findViewById(R.id.seekBar)).setProgress(0);
                ((ImageView)MessagesAdapter.runningAudio.second.findViewById(R.id.audioButton))
                        .setImageDrawable(ResourcesCompat.getDrawable(
                                parentContext.getResources(),
                                R.drawable.audio_play_button,
                                null));
            }

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
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
