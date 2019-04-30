package com.example.nameless.autoupdating.common;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;

import com.example.nameless.autoupdating.R;

public class VideoUI extends FrameLayout {
    public VideoUI(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.video_preview_ui, this);
    }
}
