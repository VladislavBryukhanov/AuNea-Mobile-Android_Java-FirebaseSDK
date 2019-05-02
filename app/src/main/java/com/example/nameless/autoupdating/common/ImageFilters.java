package com.example.nameless.autoupdating.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import java.io.File;

public class ImageFilters {

    public static Bitmap blureBitmap(Context context, File thumbnail, float coefficient) {
        RenderScript rs = RenderScript.create(context);
        Bitmap img = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
        Allocation input = Allocation.createFromBitmap(rs, img); //use this constructor for best performance, because it uses USAGE_SHARED mode which reuses memory
        Allocation output = Allocation.createTyped(rs, input.getType());
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(coefficient);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(img);
        return img;
    }
}
