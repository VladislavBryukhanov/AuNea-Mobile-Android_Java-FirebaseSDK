package com.example.nameless.autoupdating.activities;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.nameless.autoupdating.R;

public class ImageViewer extends AppCompatActivity {

//    private Pair<Float, Float> xy1,xy2;
    private ImageView imgViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_image_viewer);

        imgViewer = findViewById(R.id.imgViewer);
        Intent intent = getIntent();
        String path = (intent.getParcelableExtra("bitmap")).toString();

        imgViewer.setImageBitmap(BitmapFactory.decodeFile(path));
    /*    imgViewer.setOnTouchListener((view, motionEvent) -> {
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
        });*/
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.abc_fade_out);
    }

   /* private void getSide() {
        Float xLine = Math.abs(xy1.first - xy2.first);
        Float yLine = Math.abs(xy1.second - xy2.second);

        if(xLine < yLine){
            finish();
        }
    }*/
}