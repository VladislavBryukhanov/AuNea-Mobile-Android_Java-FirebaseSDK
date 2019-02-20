package com.example.nameless.autoupdating.common.MediaFileUtils;

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
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.ImageViewer;
import com.example.nameless.autoupdating.adapters.MessagesAdapter;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MediaFileHandler extends AsyncTask<File, Void, Bitmap> {

    public static final String MUSIC_TYPE = "audio";
    public static final String IMAGE_TYPE = "image";
    public static final String VIDEO_TYPE = "video";

    private Context parentContext;
    private String fileType;
    private String fileUrl;
    private ImageView bmImage;
    private ProgressBar pbLoading;

    private LinearLayout audioUI;
    private ProgressBar audioPbLoading;
    private ImageView audioButton;
    private TextView audioTime;
    private SeekBar trackSeekBar;
    private int trackDuration;
    private Boolean isTrackPlaying = false;
    private DateFormat formatter;

    public MediaFileHandler(
            Context parentContext,
            String fileType,
            String fileUrl,
            ImageView bmImage,
            ProgressBar pbLoading) {

        this.fileUrl = fileUrl;
        this.bmImage = bmImage;
        this.pbLoading = pbLoading;
        this.parentContext = parentContext;
        this.fileType = fileType;
        formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public MediaFileHandler(
            Context parentContext,
            String fileType,
            String fileUrl,
            LinearLayout audioUI) {

        this.fileUrl = fileUrl;
        this.audioUI = audioUI;
        this.parentContext = parentContext;
        this.fileType = fileType;
        audioButton = audioUI.findViewById(R.id.audioButton);
        trackSeekBar = audioUI.findViewById(R.id.seekBar);
        audioPbLoading = audioUI.findViewById(R.id.pbLoading);
        audioTime = audioUI.findViewById(R.id.tvTime);
        formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected Bitmap doInBackground(File... file) {
        return setFileProperties(file[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        if(bmp != null) {
            switch (fileType) {
                case IMAGE_TYPE: {
                    bmImage.setImageBitmap(bmp);
                    pbLoading.setVisibility(View.GONE);
                    bmImage.setVisibility(View.VISIBLE);
                    break;
                }
                case MUSIC_TYPE: {
                    String time = milisecondToTimeString(trackDuration);

                    audioTime.setText(time);
                    audioButton.setImageBitmap(bmp);
                    audioPbLoading.setVisibility(View.GONE);
                    audioButton.setVisibility(View.VISIBLE);

                    if(isTrackPlaying) {
                        setDurationSeek();
                    }
                    break;
                }
        /*        case VIDEO_TYPE: {
                    return setVideoFile(url);
                }
                default: {

                }*/
            }
            MessagesAdapter.filesLoadingInProgress.remove(fileUrl);

        }
    }

    private Bitmap setFileProperties(File file) {

        switch (fileType) {
            case IMAGE_TYPE: {
                return setImageProperties(file.getPath(), fileUrl);
            }
            case MUSIC_TYPE: {
                return setAudioProperties(file.getPath(), fileUrl);
//                return setAudioFile(url);
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

    private Bitmap setImageProperties(String path, String url) {

        Bitmap image = MessagesAdapter.mMemoryCache.get(fileUrl);
        if (image == null) {
            image = BitmapFactory.decodeFile(path);
            MessagesAdapter.mMemoryCache.put(url, image);
        }

        setImageOnClickListener(path);
        return image;
    }

    private void setImageOnClickListener(final String path) {
        bmImage.setOnClickListener(v -> {
            Intent intent = new Intent(parentContext, ImageViewer.class);
            intent.putExtra("bitmap", Uri.parse(path));
            parentContext.startActivity(intent);
        });
    }

    private Bitmap setAudioProperties(final String path, final String url) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//        mmr.setDataSource(parentContext.getApplicationContext(), Uri.parse(path));
        mmr.setDataSource(path);

        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        trackDuration = Integer.parseInt(durationStr);

        trackSeekBar.setMax(trackDuration);
        audioButton.setOnClickListener(v -> {
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
                    MessagesAdapter.mediaPlayer.setOnCompletionListener(mp -> stopTrack(true));
                    setDurationSeek();
                    audioButton.setImageDrawable(ResourcesCompat.getDrawable(
                            parentContext.getResources(),
                            R.drawable.audio_pause_button, null));
                    MessagesAdapter.runningAudio = new Pair<>(url, audioUI);
                    MessagesAdapter.trackDuration = trackDuration;

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if(MessagesAdapter.runningAudio != null && url.equals(MessagesAdapter.runningAudio.first)) {
            isTrackPlaying = true;
            MessagesAdapter.mediaPlayer.setOnCompletionListener(mp -> stopTrack(true));
            return drawableItemToBitmap(R.drawable.audio_pause_button);
        } else {
            isTrackPlaying = false;
            return drawableItemToBitmap(R.drawable.audio_play_button);
        }
    }

    private void setDurationSeek() {
        MessagesAdapter.trackSeekBarHandler = new Handler();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                if(MessagesAdapter.mediaPlayer.isPlaying()){
                    int mCurrentPosition = MessagesAdapter.mediaPlayer.getCurrentPosition();
                    trackSeekBar.setProgress(mCurrentPosition);
                    MessagesAdapter.trackSeekBarHandler.postDelayed(this, 100);
                }
            }
        });
        th.start();

        MessagesAdapter.trackDurationHandler = new Handler();
        MessagesAdapter.trackDurationHandler.post(new Runnable() {
            @Override
            public void run() {
                int mCurrentPosition = MessagesAdapter.mediaPlayer.getCurrentPosition();
                if(MessagesAdapter.mediaPlayer.isPlaying()) {
                    String time = milisecondToTimeString(roundDigitToThousand(mCurrentPosition));
                    audioTime.setText(time);
                    MessagesAdapter.trackDurationHandler.postDelayed(this, 1000);
                }
            }
        });

        trackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    MessagesAdapter.mediaPlayer.seekTo(progress);
                    ((TextView)MessagesAdapter.runningAudio.second.findViewById(R.id.tvTime))
                            .setText(milisecondToTimeString(progress));
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
            MessagesAdapter.trackSeekBarHandler.removeCallbacksAndMessages(null);
            MessagesAdapter.mediaPlayer.stop();
            MessagesAdapter.mediaPlayer.release();
            MessagesAdapter.mediaPlayer = new MediaPlayer();

            if (isCurrent) {
                audioButton.setImageDrawable(ResourcesCompat.getDrawable(
                        parentContext.getResources(),
                        R.drawable.audio_play_button,
                        null));
                trackSeekBar.setProgress(0);
                audioTime.setText(milisecondToTimeString(trackDuration));
            } else {
                ((SeekBar)MessagesAdapter.runningAudio.second.findViewById(R.id.seekBar)).setProgress(0);
                ((ImageView)MessagesAdapter.runningAudio.second.findViewById(R.id.audioButton))
                        .setImageDrawable(ResourcesCompat.getDrawable(
                                parentContext.getResources(),
                                R.drawable.audio_play_button,
                                null));
                ((TextView)MessagesAdapter.runningAudio.second.findViewById(R.id.tvTime))
                        .setText(milisecondToTimeString(MessagesAdapter.trackDuration));
            }

            MessagesAdapter.runningAudio = null;
        }
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

    private int roundDigitToThousand(int digit) {
        digit = (int)(Math.rint((double) digit / 1000) * 1000);
        return Math.round(digit);
    }

    private String milisecondToTimeString(int trackDuration) {
        return formatter.format(new Date(trackDuration));
    }

}
