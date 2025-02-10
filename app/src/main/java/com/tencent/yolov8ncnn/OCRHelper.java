package com.tencent.yolov8ncnn;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OCRHelper {

    // 初始化识别器（建议全局单例）
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    // 识别入口方法
    public void recognizeCharacter(Bitmap originalBitmap, final OCRCallback callback) {
        // 预处理图片（关键步骤）
        Bitmap processedBitmap = preprocessImage(originalBitmap);
//        ImageUtils.saveBitmap(null,processedBitmap);
        // 创建InputImage对象
        InputImage image = InputImage.fromBitmap(processedBitmap, 0);
        // 执行识别
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String recognizedText = result.getText();
                    // 提取最可能的单个字符
                    if (!recognizedText.isEmpty()) {
                        char character = recognizedText.charAt(0);
                        callback.onResult(String.valueOf(character));
                    } else {
                        callback.onError("No text detected");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e.getMessage());
                    }
                });
    }



    // 图片预处理（灰度化 + 二值化）
    // 图片预处理优化版（增加锐化+对比度增强+动态阈值）

    private Bitmap preprocessImage(Bitmap original) {
        int borderSize = 60; // 可根据图像尺寸调整
        original = addWhiteBorder(original, borderSize);
        // 保持原尺寸
        Bitmap processed = Bitmap.createBitmap(original.getWidth(),  original.getHeight(),  Bitmap.Config.ARGB_8888);

        // 第一步：抗锯齿预处理（轻微高斯模糊）
        Bitmap blurred = applyGaussianBlur(original, 1.2f); // 模糊半径控制

        Canvas canvas = new Canvas(processed);
        Paint paint = new Paint();

        // 调整后的增强参数（更温和）
        float contrast = 1.2f; // 降低对比度增强幅度
        float adjust = (1 - contrast) * 128;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                contrast, 0, 0, 0, adjust,
                0, contrast, 0, 0, adjust,
                0, 0, contrast, 0, adjust,
                0, 0, 0, 1, 0
        });

        // 改进型锐化矩阵（避免过度锐化）
        ColorMatrix sharpnessMatrix = new ColorMatrix(new float[] {
                1.2f, 0,    0,    0,    -20,
                0,    1.2f, 0,    0,    -20,
                0,    0,    1.2f, 0,    -20,
                0,    0,    0,    1,    0
        });

        // 合并效果（先锐化后降对比会更自然）
        ColorMatrix combined = new ColorMatrix();
        combined.postConcat(sharpnessMatrix);
        combined.postConcat(contrastMatrix);

        paint.setColorFilter(new  ColorMatrixColorFilter(combined));
        canvas.drawBitmap(blurred,  0, 0, paint); // 处理模糊后的图像

        // 第二步：带抗锯齿的二值化
        Bitmap binaryBitmap = Bitmap.createBitmap(processed.getWidth(),  processed.getHeight(),  Bitmap.Config.ARGB_8888);

        // 动态阈值（加入边缘保护）
        int threshold = 128;

        // 使用抗锯齿画笔
        Paint binaryPaint = new Paint();
        binaryPaint.setAntiAlias(true);

        // 基于区域渐变的二值化
        for (int x = 0; x < processed.getWidth();  x++) {
            for (int y = 0; y < processed.getHeight();  y++) {
                int pixel = processed.getPixel(x,  y);
                int luminance = (Color.red(pixel)  + Color.green(pixel)  + Color.blue(pixel))  / 3;

                // 带过渡效果的双阈值处理
                if (luminance > threshold + 20) {
                    binaryBitmap.setPixel(x,  y, Color.WHITE);
                } else if (luminance < threshold - 20) {
                    binaryBitmap.setPixel(x,  y, Color.BLACK);
                } else {
                    // 中间带透明度过渡（模拟抗锯齿）
                    float alpha = (luminance - (threshold - 20)) / 40f;
                    binaryBitmap.setPixel(x,  y, Color.argb(
                            (int)(255 * (1 - alpha)), 0, 0, 0));
                }
            }
        }

        return applyErosion(binaryBitmap);
    }
    private Bitmap addWhiteBorder(Bitmap src, int borderSize) {
        int newWidth = src.getWidth()  + 2 * borderSize;
        int newHeight = src.getHeight()  + 2 * borderSize;

        Bitmap output = Bitmap.createBitmap(newWidth,  newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 填充纯白背景
        canvas.drawColor(Color.WHITE);

        // 居中绘制原图
        canvas.drawBitmap(src,  borderSize, borderSize, null);

        return output;
    }

    private Bitmap applyErosion(Bitmap binary) {
        Bitmap eroded = Bitmap.createBitmap(binary.getWidth(),  binary.getHeight(),  Bitmap.Config.ARGB_8888);
        int erosionSize = 1; // 腐蚀强度

        for (int x = 0; x < binary.getWidth();  x++) {
            for (int y = 0; y < binary.getHeight();  y++) {
                if (binary.getPixel(x,  y) == Color.BLACK) {
                    // 检查3x3邻域是否全黑
                    boolean keepBlack = true;
                    outer:
                    for (int dx = -erosionSize; dx <= erosionSize; dx++) {
                        for (int dy = -erosionSize; dy <= erosionSize; dy++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && nx < binary.getWidth()  && ny >=0 && ny < binary.getHeight())  {
                                if (binary.getPixel(nx,  ny) != Color.BLACK) {
                                    keepBlack = false;
                                    break outer;
                                }
                            }
                        }
                    }
                    eroded.setPixel(x,  y, keepBlack ? Color.BLACK : Color.WHITE);
                } else {
                    eroded.setPixel(x,  y, Color.WHITE);
                }
            }
        }
        return eroded;
    }

    private Bitmap applyGaussianBlur(Bitmap src, float radius) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap output = Bitmap.createBitmap(w,  h, Bitmap.Config.ARGB_8888);

        // 3x3高斯矩阵
        float[][] kernel = {
                {1/16f, 2/16f, 1/16f},
                {2/16f, 4/16f, 2/16f},
                {1/16f, 2/16f, 1/16f}
        };

        for (int x = 1; x < w-1; x++) {
            for (int y = 1; y < h-1; y++) {
                float r = 0, g = 0, b = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int pixel = src.getPixel(x+dx,  y+dy);
                        float weight = kernel[dx+1][dy+1];
                        r += Color.red(pixel)  * weight;
                        g += Color.green(pixel)  * weight;
                        b += Color.blue(pixel)  * weight;
                    }
                }
                output.setPixel(x,  y, Color.rgb((int)r,  (int)g, (int)b));
            }
        }
        return output;
    }

    // 回调接口
    public interface OCRCallback {
        void onResult(String character);
        void onError(String error);
    }
}
