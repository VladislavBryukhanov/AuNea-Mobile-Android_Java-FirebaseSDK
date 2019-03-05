package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Pair;
import android.widget.LinearLayout;

public class RunningAudio {
    public static Pair<String, LinearLayout> runningAudio;
    public static MediaPlayer mediaPlayer;
    public static Handler trackDurationHandler;
    public static Handler trackSeekBarHandler;

    public static void initInstance() {
        mediaPlayer = new MediaPlayer();
        trackDurationHandler = new Handler();
        trackSeekBarHandler = new Handler();
        runningAudio = null;
    }
}
