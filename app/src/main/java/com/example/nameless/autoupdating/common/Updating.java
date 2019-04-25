package com.example.nameless.autoupdating.common;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.example.nameless.autoupdating.BuildConfig;
import com.example.nameless.autoupdating.activities.Settings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

/**
 * Created by nameless on 07.04.18.
 */

class Updating {

    public static final String APP_UPDATING_PROCESS = "APP_UPDATING_STATE";

    private File toInstall;
    private Context context;

    Updating(Context context) {
        this.context = context;
    }

    void startUpdating() {
        String path = Environment.getExternalStorageDirectory() + "/Download/";
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference = storage.getReferenceFromUrl(
                "gs://messager-d15a0.appspot.com/app-debug.apk");
        toInstall = new File(path, "app-update.apk");
        gsReference.getFile(toInstall).addOnSuccessListener(taskSnapshot -> {
//                Toast.contextkeText(context, "Success", Toast.LENGTH_SHORT).show();
            updateApp();
        }).addOnFailureListener(e -> Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show());
    }
    
    private void updateApp() {
        SharedPreferences appData = context.getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs = appData.edit();
        prefs.putBoolean(APP_UPDATING_PROCESS, true);
        prefs.apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(context,
                    BuildConfig.APPLICATION_ID + ".provider", toInstall);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } else {
            Uri apkUri = Uri.fromFile(toInstall);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
