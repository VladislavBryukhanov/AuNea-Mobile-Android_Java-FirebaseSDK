package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AudioComponent {

    private Context parentContext;
    private int trackDuration;

    private String fileUrl;
    private File mediaFile;

    private LinearLayout audioUI;
    private ProgressBar audioPbLoading;
    private ImageView audioButton;
    private TextView audioTime;
    private SeekBar trackSeekBar;
    private Boolean isTrackPlaying = false;
    private DateFormat formatter;

    AudioComponent(Context parentContext, File mediaFile, String fileUrl, LinearLayout audioUI) {
        this.parentContext = parentContext;
        this.fileUrl = fileUrl;
        this.mediaFile = mediaFile;
        this.audioUI = audioUI;

        audioButton = audioUI.findViewById(R.id.audioButton);
        trackSeekBar = audioUI.findViewById(R.id.seekBar);
        audioPbLoading = audioUI.findViewById(R.id.pbLoading);
        audioTime = audioUI.findViewById(R.id.tvTime);
        formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    Bitmap setAudioProperties() {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//        mmr.setDataSource(parentContext.getApplicationContext(), Uri.parse(path));
        mmr.setDataSource(mediaFile.getPath());

        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        trackDuration = Integer.parseInt(durationStr);
        trackSeekBar.setMax(trackDuration);

        if(RunningAudio.runningAudio != null && fileUrl.equals(RunningAudio.runningAudio.first)) {
            isTrackPlaying = true;
            RunningAudio.mediaPlayer.setOnCompletionListener(mp -> stopTrack(true));
            return drawableItemToBitmap(R.drawable.audio_pause_button);
        } else {
            isTrackPlaying = false;
            return drawableItemToBitmap(R.drawable.audio_play_button);
        }
    }

    void setAudioUI(Bitmap bmp) {
        setAudioOnClickListener();
        String time = milisecondToTimeString(trackDuration);

        audioTime.setText(time);
        audioButton.setImageBitmap(bmp);
        audioPbLoading.setVisibility(View.GONE);
        audioButton.setVisibility(View.VISIBLE);

        if(isTrackPlaying) {
            setDurationSeek();
        }
    }

    private void setAudioOnClickListener() {
        audioButton.setOnClickListener(v -> {
            try {
                boolean trackIsPlaying = RunningAudio.mediaPlayer.isPlaying();
                if(trackIsPlaying && fileUrl.equals(RunningAudio.runningAudio.first)) {
//                    mediaPlayer.pause();
                    stopTrack(true);
                } else {
                    stopTrack(false);
                    RunningAudio.mediaPlayer.setDataSource(mediaFile.getPath());
                    RunningAudio.mediaPlayer.prepare();
                    RunningAudio.mediaPlayer.start();
                    RunningAudio.mediaPlayer.setOnCompletionListener(mp -> stopTrack(true));
                    RunningAudio.runningAudio = new Pair<>(fileUrl, audioUI);
                    audioButton.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                    parentContext.getResources(),
                                    R.drawable.audio_pause_button,
                                    null)
                    );
                    setDurationSeek();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void setDurationSeek() {
        RunningAudio.trackSeekBarHandler = new Handler();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                if(RunningAudio.mediaPlayer.isPlaying()){
                    int mCurrentPosition = RunningAudio.mediaPlayer.getCurrentPosition();
                    trackSeekBar.setProgress(mCurrentPosition);
                    RunningAudio.trackSeekBarHandler.postDelayed(this, 100);
                }
            }
        });
        th.start();

        RunningAudio.trackDurationHandler = new Handler();
        RunningAudio.trackDurationHandler.post(new Runnable() {
            @Override
            public void run() {
                int mCurrentPosition = RunningAudio.mediaPlayer.getCurrentPosition();
                if(RunningAudio.mediaPlayer.isPlaying()) {
                    String time = milisecondToTimeString(roundDigitToThousand(mCurrentPosition));
                    audioTime.setText(time);
                    RunningAudio.trackDurationHandler.postDelayed(this, 1000);
                }
            }
        });

        trackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    RunningAudio.mediaPlayer.seekTo(progress);
                    ((TextView) RunningAudio.runningAudio.second.findViewById(R.id.tvTime))
                            .setText(milisecondToTimeString(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                RunningAudio.mediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                RunningAudio.mediaPlayer.start();
            }
        });
    }

    private void stopTrack(boolean isCurrent) {
        if(RunningAudio.runningAudio != null) {
            RunningAudio.trackDurationHandler.removeCallbacksAndMessages(null);
            RunningAudio.trackSeekBarHandler.removeCallbacksAndMessages(null);
            RunningAudio.mediaPlayer.stop();
            RunningAudio.mediaPlayer.release();
            RunningAudio.mediaPlayer = new MediaPlayer();

            if (isCurrent) {
                audioButton.setImageDrawable(ResourcesCompat.getDrawable(
                        parentContext.getResources(),
                        R.drawable.audio_play_button,
                        null));
                trackSeekBar.setProgress(0);
                trackSeekBar.setOnSeekBarChangeListener(null);
                audioTime.setText(milisecondToTimeString(trackDuration));
            } else {
                LinearLayout audioUi = RunningAudio.runningAudio.second;
                ((SeekBar) audioUi.findViewById(R.id.seekBar)).setProgress(0);
                ((SeekBar) audioUi.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(null);
                ((ImageView) audioUi.findViewById(R.id.audioButton))
                        .setImageDrawable(ResourcesCompat.getDrawable(
                                parentContext.getResources(),
                                R.drawable.audio_play_button,
                                null));
                ((TextView) audioUi.findViewById(R.id.tvTime))
                        .setText(milisecondToTimeString(trackDuration));
            }
            RunningAudio.runningAudio = null;
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
