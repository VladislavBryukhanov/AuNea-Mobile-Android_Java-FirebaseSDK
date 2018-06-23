package com.example.nameless.autoupdating;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by nameless on 13.06.18.
 */

public class DownloadAvatarByUrl extends AsyncTask<String, Void, Bitmap> {

    private ImageView bmImage;
    private User user;

    public DownloadAvatarByUrl(ImageView bmImage, User user) {
        this.bmImage = bmImage;
        this.user = user;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
/*        InputStream in =null;
        try {
            in = new java.net.URL(strings[0]).openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bmp = BitmapFactory.decodeStream(in);*/
//        Bitmap bmp = BitmapFactory.decodeFile(downloadFileByUrl(strings[0]));
//        return bmp;

        Bitmap bmp = BitmapFactory.decodeFile(downloadFileByUrl(strings[0]));
        return bmp;
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        bmImage.setImageBitmap(bmp);
    }

    public String downloadFileByUrl(final String url) {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference fileReference = storage.getReferenceFromUrl(url);

        File path = new File(Environment.getExternalStorageDirectory()
                + "/AUMessanger/Users/");
        if(!path.exists()) {
            path.mkdirs();
        }

        final File imgFile = new File(path, fileReference.getName());
        if (!imgFile.exists()) {
            fileReference.getFile(imgFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap bmp = BitmapFactory.decodeFile(imgFile.getPath());
                    user.setAvatar(imgFile.getPath());
//                    bmImage.setImageBitmap(bmp);
                    onPostExecute(bmp);
                }
            });
        } else {
            user.setAvatar(imgFile.getPath());
            return imgFile.getPath();
        }
    return null;
    }
}
