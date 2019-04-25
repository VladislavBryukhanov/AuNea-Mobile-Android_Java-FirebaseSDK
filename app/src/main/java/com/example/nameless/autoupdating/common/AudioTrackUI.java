package com.example.nameless.autoupdating.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.example.nameless.autoupdating.R;

public class AudioTrackUI extends LinearLayout {

    public AudioTrackUI(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.audio_track_ui, this);
    }
}
