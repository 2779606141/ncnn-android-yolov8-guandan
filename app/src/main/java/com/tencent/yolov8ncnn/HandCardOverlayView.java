package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class HandCardOverlayView extends View {
    private final Paint rectPaint;
    private final Paint textPaint;
    private final int[][] players; // 玩家矩形框坐标数组
    private final String[] playerTexts; // 矩形框上的文本
    private float startX, startY; // 新增变量记录初始触摸位置
    private int startRight, startBottom; // 记录初始右下角坐标

    // 当前被拖动的矩形框索引
    private int draggingPlayerIndex = -1;

    // 回调接口，用于通知外部坐标发生变化
    public interface OnRectPositionChangedListener {
        void onRectPositionChanged(int playerIndex, int[] newRect);
    }

    private OnRectPositionChangedListener positionChangeListener;

    public HandCardOverlayView(Context context, int[] p0, int[] p1, int[] p2, int[] p3, int[] p4) {
        super(context);
        players = new int[][]{p0, p1, p2, p3, p4};
        playerTexts = new String[]{"", "", "", "", ""};

        // 矩形绘制配置
        rectPaint = new Paint();
        rectPaint.setColor(Color.RED);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(5);

        // 文本绘制配置
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < players.length; i++) {
            drawRectWithText(canvas, players[i], playerTexts[i]);
        }
    }

    private void drawRectWithText(Canvas canvas, int[] rect, String text) {
        // 绘制矩形
        canvas.drawRect(rect[0], rect[1], rect[2], rect[3], rectPaint);

        // 绘制文本（位于矩形正下方）
        if (!text.isEmpty()) {
            float textX = rect[0] + (rect[2] - rect[0]) / 3f; // 水平居中
            float textY = rect[3] + 40; // 矩形下方50像素
            canvas.drawText(text, textX, textY, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction())  {
            case MotionEvent.ACTION_DOWN:
                draggingPlayerIndex = -1;
                for (int i = 0; i < players.length;  i++) {
                    int[] rect = players[i];
                    if (x >= rect[0] && x <= rect[2] && y >= rect[1] && y <= rect[3]) {
                        draggingPlayerIndex = i;
                        startX = x;
                        startY = y;
                        startRight = rect[2]; // 记录初始右下角X
                        startBottom = rect[3]; // 记录初始右下角Y
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (draggingPlayerIndex != -1) {
                    int[] draggedRect = players[draggingPlayerIndex];
                    float deltaX = x - startX;
                    float deltaY = y - startY;

                    // 计算新右下角坐标
                    int newRight = (int) (startRight + deltaX);
                    int newBottom = (int) (startBottom + deltaY);

                    // 保持宽高不变计算新左上角
                    int width = draggedRect[2] - draggedRect[0];
                    int height = draggedRect[3] - draggedRect[1];
                    draggedRect[0] = newRight - width;
                    draggedRect[1] = newBottom - height;
                    draggedRect[2] = newRight;
                    draggedRect[3] = newBottom;

                    invalidate();
                    if (positionChangeListener != null) {
                        positionChangeListener.onRectPositionChanged(draggingPlayerIndex,  draggedRect);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                draggingPlayerIndex = -1;
                break;
        }
        return true;
    }

    // 设置回调接口
    public void setOnRectPositionChangedListener(OnRectPositionChangedListener listener) {
        this.positionChangeListener = listener;
    }

    // 更新文本的公共方法
    public void updatePlayerText(int playerIndex, String text) {
        if (playerIndex >= 0 && playerIndex < players.length) {
            playerTexts[playerIndex] = text;
            invalidate(); // 触发界面重绘
        }
    }
}