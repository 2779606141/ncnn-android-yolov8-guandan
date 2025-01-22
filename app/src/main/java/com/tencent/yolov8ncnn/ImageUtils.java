package com.tencent.yolov8ncnn;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

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
}
