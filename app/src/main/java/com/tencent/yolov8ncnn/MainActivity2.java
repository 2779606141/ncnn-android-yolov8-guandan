package com.tencent.yolov8ncnn; // 请根据实际情况调整包名

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.graphics.Matrix;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity2 extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST_CODE = 1; // 请求码用于识别图像选择结果
    private Yolov8Ncnn yolov8ncnn; // YOLOv8模型实例
    private ImageView imageView; // 显示选中图片的ImageView
    private Matrix matrix = new Matrix(); // 用于处理图像变换（如缩放、移动）
    private float scale = 1f; // 当前缩放比例
    private ScaleGestureDetector scaleGestureDetector; // 处理缩放手势的探测器
    private float lastTouchX, lastTouchY; // 上一次触摸点的位置，用于计算拖动距离

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); // 设置活动布局

        imageView = findViewById(R.id.imageView); // 初始化ImageView组件

        Button buttonSelectImage = findViewById(R.id.buttonSelectImage); // 初始化选择图片按钮
        buttonSelectImage.setOnClickListener(new View.OnClickListener() { // 设置点击事件监听器
            @Override
            public void onClick(View v) {
                openImageChooser(); // 打开图像选择器
            }
        });

        // 初始化YOLOv8模型实例
        yolov8ncnn = new Yolov8Ncnn();

        // 尝试加载模型文件
        if (!loadModel()) {
            Toast.makeText(this, "模型加载失败", Toast.LENGTH_SHORT).show();
            finish(); // 如果模型加载失败，则关闭当前活动
        }

        // 初始化缩放手势探测器
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    // 尝试加载YOLOv8模型
    private boolean loadModel() {
        try {
            return yolov8ncnn.loadModel(getAssets(), 2, 0); // 使用默认的模型索引和CPU/GPU设置
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to load model: " + e.getMessage());
            return false;
        }
    }

    // 打开系统图像选择器
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
    }

    // 处理图像选择结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData()); // 获取选中的图片
                int[] list = new int[100]; // 假设最多有100个标签
                boolean success = yolov8ncnn.recognizeImage(bitmap, list); // 对图片进行识别
                if (success) {
                    imageView.setImageBitmap(bitmap); // 在ImageView中显示图片
                } else {
                    Toast.makeText(this, "识别失败", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 处理触摸事件，支持拖动和缩放
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event); // 处理缩放手势

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX(); // 记录按下时的X坐标
                lastTouchY = event.getY(); // 记录按下时的Y坐标
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX; // 计算X轴方向上的位移
                float dy = event.getY() - lastTouchY; // 计算Y轴方向上的位移
                matrix.postTranslate(dx, dy); // 更新矩阵以反映位移
                imageView.setImageMatrix(matrix); // 应用变换到ImageView
                lastTouchX = event.getX(); // 更新上一次触摸点的位置
                lastTouchY = event.getY();
                break;
        }

        return true;
    }

    // 缩放手势监听器
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor(); // 根据手势更新缩放比例
            scale = Math.max(1f, Math.min(scale, 5f)); // 限制缩放比例在1到5之间

            matrix.setScale(scale, scale, detector.getFocusX(), detector.getFocusY()); // 应用缩放到矩阵
            imageView.setImageMatrix(matrix); // 更新ImageView的矩阵
            return true;
        }
    }

    // 清理资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        yolov8ncnn.destroy(); // 清理模型资源（目前被注释掉）
    }
}