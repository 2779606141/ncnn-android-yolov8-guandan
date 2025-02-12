package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class HandCardOverlayView extends View {
    private final Paint rectPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final WindowManager windowManager;
    private String displayText="jjj";
    private int viewIndex;
    private float lastX, lastY;
    private final int textHight=60;
    private final int textWidth=30;

    public HandCardOverlayView(Context context, int index, String text) {
        super(context);
        // 通过context获取WindowManager实例
        this.windowManager  = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        viewIndex=index;
        displayText=text;
        // 初始化样式配置
        rectPaint.setColor(Color.RED);   // 纯红色边框
        rectPaint.setStyle(Paint.Style.STROKE);   // 描边模式
        rectPaint.setStrokeWidth(5f);   // 边框宽度5像素

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0,  0, getWidth()-textWidth, getHeight()-textHight, rectPaint);
        // 绘制左对齐文本
        float textX = 10; // 文本距离左侧的偏移量（可根据需要调整）
        float textY = getHeight() - 20; // 文本基线位置
        canvas.drawText(displayText, textX, textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();

        switch (event.getAction())  {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                WindowManager.LayoutParams params =
                        (WindowManager.LayoutParams) getLayoutParams();

                // 计算偏移量
                float deltaX = x - lastX;
                float deltaY = y - lastY;

                // 更新位置
                params.x += deltaX;
                params.y += deltaY;
                windowManager.updateViewLayout(this,  params);

                // 更新最后位置
                lastX = x;
                lastY = y;
                return true;

            case MotionEvent.ACTION_UP:
                performClick();
                return true;
        }
        return false;
    }

    // 更新文本方法
    public void updateText(String newText) {
        displayText = newText;
        postInvalidate();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
