package com.example.nameless.autoupdating.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.nameless.autoupdating.R;

public class ImageViewer extends AppCompatActivity {

    private Pair<Float, Float> xy1,xy2;
    private ImageView imgViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_image_viewer);

        imgViewer = findViewById(R.id.imgViewer);
        Intent intent = getIntent();
        String path = (intent.getParcelableExtra("bitmap")).toString();

        /*final ScaleGestureDetector scaling = new ScaleGestureDetector(getApplicationContext(),
                new ScaleGestureDetector.OnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        Toast.makeText(ImageViewer.this, "zoom end, scale: " + detector.getScaleFactor(), Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        return false;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                    }
        });*/

        imgViewer.setImageBitmap(BitmapFactory.decodeFile(path));
        imgViewer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
//                scaling.onTouchEvent(motionEvent);
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:{
                        xy1 = new Pair<>( motionEvent.getX(), motionEvent.getY());
                        break;
                    }
                    case MotionEvent.ACTION_UP:{
                        xy2 = new Pair<>( motionEvent.getX(), motionEvent.getY());
                        getSide();
                        break;
                    }
                }
                return true;
            }
        });
    }

    private void getSide() {
        Float xLine = Math.abs(xy1.first - xy2.first);
        Float yLine = Math.abs(xy1.second - xy2.second);

        if(xLine < yLine){
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}
