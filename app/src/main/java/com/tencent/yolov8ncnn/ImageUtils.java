package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ImageUtils {
    public static Bitmap imageToBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        // 两个像素的距离
        int pixelStride = planes[0].getPixelStride();
        // 整行的距离
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

        image.close();
        return bitmap;
    }

    /**
     * 通过反射，获取包含虚拟键的整体屏幕高度
     * @return
     */
    public static int getHasVirtualKey(WindowManager windowManager) {
        int dpi = 0;
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        @SuppressWarnings("rawtypes")
        Class c;
        try {
            c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            dpi = dm.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dpi;
    }

    public static Bitmap cropBitmap(Bitmap sourceBitmap, int[] area) {
        Bitmap croppedBitmap = Bitmap.createBitmap(sourceBitmap, area[0], area[1], area[2] - area[0], area[3] - area[1]);
        return croppedBitmap;
    }

    public static void saveBitmap(Context context, Bitmap bitmap) {
        // 定义保存Bitmap的文件路径和文件名
        String filename = UUID.randomUUID().toString() + ".png";
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appPicturesDirectory = new File(picturesDirectory, "MyAppFolder"); // "MyAppFolder"是你应用的文件夹
        if (!appPicturesDirectory.exists()) {
            if (!appPicturesDirectory.mkdirs()) {
                Log.e("BitmapSave", "Failed to create directory for saving images.");
                return;
            }
        }
        File outputFile = new File(appPicturesDirectory, filename);

        try {
            // 将Bitmap压缩成文件
            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // 100表示不压缩
            out.flush();
            out.close();

            // 将文件插入到媒体库，这样相册应用就能看到这张图片了
            MediaScannerConnection.scanFile(context, new String[]{outputFile.getAbsolutePath()}, null, null);


        } catch (IOException e) {
            e.printStackTrace();
            Log.e("BitmapSave", "Error saving bitmap: " + e.getMessage());
        }
    }
}
