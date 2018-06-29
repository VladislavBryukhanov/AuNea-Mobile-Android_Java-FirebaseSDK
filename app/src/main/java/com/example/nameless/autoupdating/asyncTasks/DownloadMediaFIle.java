package com.example.nameless.autoupdating.asyncTasks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.ImageViewer;
import com.example.nameless.autoupdating.activities.VideoPlayer;
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

    private ImageView bmImage;
    private ProgressBar pbLoading;
    private Context parentContext;
    private Map<String, Bitmap> imageCollection;
    private String fileType;

    public DownloadMediaFIle(ImageView bmImage, ProgressBar pbLoading, Context parentContext, Map<String, Bitmap> imageCollection, String fileType) {
        this.bmImage = bmImage;
        this.parentContext = parentContext;
        this.imageCollection  = imageCollection;
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
            pbLoading.setVisibility(View.GONE);
            bmImage.setImageBitmap(bmp);
            bmImage.setVisibility(View.VISIBLE);
        }
//        if(fileType.equals("video")) {
//            pbLoading.setVisibility(View.GONE);
//            bmVideo.setVisibility(View.VISIBLE);
//
//            bmVideo.setVideoURI(Uri.parse(url));
//            bmVideo.setMediaController(new MediaController(parentContext));
//            bmVideo.requestFocus(0);
//        }
    }

    private Bitmap setFileProperties(String url) {
        switch (fileType) {
            case "image": {
                return  downloadFileByUrl(url);
            }
            case "video": {
                return setVideoFile(url);
            }
//            case "audio": {
//                return setAudioFile(url);
//            }
            default: {
                return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(parentContext.getResources(),
                    R.drawable.file), 160, 160, true);
            }
        }
    }

    private Bitmap downloadFileByUrl(final String url) {
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

        final File path = new File(Environment.getExternalStorageDirectory()
                + "/AUMessanger/");
        if(!path.exists()) {
            path.mkdir();
        }

        final File imgFile = new File(path, fileReference.getName());

        if(imageCollection.containsKey(url)) {
            return  setImageOnClickListener(imgFile.getPath(), url);
        }

        if (!imgFile.exists()) {
            fileReference.getFile(imgFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    DownloadMediaFIle downloadTask = new DownloadMediaFIle(
                            bmImage, pbLoading, parentContext, imageCollection, fileType);
                    downloadTask.execute(url);
                }
            });
            return null;
        } else {
            return setImageProperties(imgFile.getPath(), url);
        }

//        String path = getContext().getCacheDir().getAbsolutePath() + file.getName();
//        return BitmapFactory.decodeFile(imgFile.getPath());
    }

    private Bitmap setImageProperties(final String path, String url) {
        //getting screen size & and calculating optimal scale for image

        Bitmap image = BitmapFactory.decodeFile(path);
        if(path == null) {
            return null;
        }

        /*WindowManager wm = (WindowManager) parentContext.getSystemService(
                Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);


        size.x *= 0.5;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        double scale = (double) imageWidth / size.x ;
        if(imageWidth > size.x) {
            imageWidth = size.x;
            imageHeight /= scale;
        }
        image = Bitmap.createScaledBitmap(image, imageWidth, imageHeight, true);*/

        imageCollection.put(url, image);
        setImageOnClickListener(path, url);
        return image;
    }

    private Bitmap setImageOnClickListener(final String path, String url) {
        bmImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(parentContext, ImageViewer.class);
                intent.putExtra("bitmap", Uri.parse(path));
                parentContext.startActivity(intent);
            }
        });
        return imageCollection.get(url);
    }

/*    private Bitmap setAudioFile(final String url) {
//        bmImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    MediaPlayer mp = new MediaPlayer();
//                    mp.setDataSource(url);
//                    mp.prepare();
//                    if(!mp.isPlaying()) {
//                        mp.start();
//                    } else {
//                        mp.stop();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(parentContext.getResources(),
//                    R.drawable.audio_play_button), 100, 100, true);
        return BitmapFactory.decodeResource(parentContext.getResources(),
                R.drawable.audio_play_button);
     }*/

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


}
