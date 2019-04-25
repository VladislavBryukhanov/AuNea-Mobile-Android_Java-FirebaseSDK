package com.example.nameless.autoupdating.common.MediaFileUtils;

import android.graphics.Bitmap;
import android.util.LruCache;

import java.util.ArrayList;

class FilesMemoryCache {
    public static LruCache<String, Bitmap> memoryCache  = new LruCache<String, Bitmap>(40) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return 1;
        }
    };
    public static ArrayList<String> filesLoadingInProgress = new ArrayList<>();
}
