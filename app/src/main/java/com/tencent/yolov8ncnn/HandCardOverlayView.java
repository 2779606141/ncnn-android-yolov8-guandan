package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class HandCardOverlayView extends View {

    private Paint paint;
    private int[] player0, player1, player2, player3;

    public HandCardOverlayView(Context context, int[] p0, int[] p1, int[] p2, int[] p3) {
        super(context);
        player0 = p0;
        player1 = p1;
        player2 = p2;
        player3 = p3;

        paint = new Paint();
        paint.setColor(Color.RED); // 设置矩形的颜色
        paint.setStyle(Paint.Style.STROKE); // 设置绘制样式为边框
        paint.setStrokeWidth(5); // 设置边框宽度
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawRect(player0, canvas);
        drawRect(player1, canvas);
        drawRect(player2, canvas);
        drawRect(player3, canvas);
    }

    private void drawRect(int[] player, Canvas canvas) {
        canvas.drawRect(player[0], player[1], player[2], player[3], paint);
    }
}