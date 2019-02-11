package com.example.nameless.autoupdating.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.example.nameless.autoupdating.BuildConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

/**
 * Created by nameless on 07.04.18.
 */

public class Updating {

    private File toInstall;
    private Context ma;

    public Updating(Context ma) {
        this.ma = ma;
    }

    public void startUpdating() {
        String path = Environment.getExternalStorageDirectory() + "/Download/";
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference = storage.getReferenceFromUrl(
                "gs://messager-d15a0.appspot.com/app-debug.apk");
        toInstall = new File(path, "app-update.apk");
        gsReference.getFile(toInstall).addOnSuccessListener(taskSnapshot -> {
//                Toast.makeText(ma, "Success", Toast.LENGTH_SHORT).show();
            updateApp();
        }).addOnFailureListener(e -> Toast.makeText(ma, "Error", Toast.LENGTH_SHORT).show());
    }
    public void updateApp() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(ma,
                    BuildConfig.APPLICATION_ID + ".provider", toInstall);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ma.startActivity(intent);
        } else {
            Uri apkUri = Uri.fromFile(toInstall);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ma.startActivity(intent);
        }
    }
}
