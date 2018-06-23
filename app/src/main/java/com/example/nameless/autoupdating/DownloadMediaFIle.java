package com.example.nameless.autoupdating;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Map;

/**
 * Created by nameless on 13.06.18.
 */

public class DownloadMediaFIle extends AsyncTask<String, Void, Bitmap> {

    private ImageView bmImage;
    private ProgressBar pbLoading;
    private Context parentContext;
    private Map<String, Bitmap> imageCollection;

    public DownloadMediaFIle(ImageView bmImage, ProgressBar pbLoading, Context parentContext, Map<String, Bitmap> imageCollection) {
        this.bmImage = bmImage;
        this.parentContext = parentContext;
        this.imageCollection  = imageCollection;
        this.pbLoading = pbLoading;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        return downloadFileByUrl(strings[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        if(bmp != null) {
            pbLoading.setVisibility(View.GONE);
            bmImage.setImageBitmap(bmp);
            bmImage.setVisibility(View.VISIBLE);
        }
    }

    private Bitmap downloadFileByUrl(final String url) {
/*        if (imageCollection.get(url) != null) {
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
*//*                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uriForIntentCollection.get(url), "image*//**//*");
                    ma.startActivity(intent);*//*
                    Intent intent = new Intent(ma, ImageViewer.class);
                    intent.putExtra("bitmap", uriForIntentCollection.get(url));
                    ma.startActivity(intent);
                }
            });
            return imageCollection.get(url);
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
            bmImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(parentContext, ImageViewer.class);
                    intent.putExtra("bitmap", Uri.parse(imgFile.getPath()));
                    parentContext.startActivity(intent);
                }
            });
            return imageCollection.get(url);
        }

        if (!imgFile.exists()) {
            fileReference.getFile(imgFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    DownloadMediaFIle downloadTask = new DownloadMediaFIle(
                            bmImage, pbLoading, parentContext, imageCollection);
                    downloadTask.execute(url);
//                    Bitmap bmp = setImageProperties(imgFile.getPath(), url);
//                    onPostExecute(bmp);
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
//            return BitmapFactory.decodeResource(parentContext.getResources(), R.drawable.loading);
            return null;
        } else if (image == null) {
            return null;
//            return BitmapFactory.decodeResource(parentContext.getResources(), R.drawable.file);
        }

        WindowManager wm = (WindowManager) parentContext.getSystemService(
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
        image = Bitmap.createScaledBitmap(image, imageWidth, imageHeight, true);

        imageCollection.put(url, image);
        bmImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(parentContext, ImageViewer.class);
                intent.putExtra("bitmap", Uri.parse(path));
                parentContext.startActivity(intent);
            }
        });
        return image;
    }
}
