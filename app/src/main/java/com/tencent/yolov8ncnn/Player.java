package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Arrays;

public class Player {
    private static final String[] CARD_NAMES = {" ", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "大王", "小王"};

    final int[] bounds;
    final int id;  // 修改为int类型
    int count;
    final int[][] hist;
    int[] last;
    int state;
    long time;

    private CardUpdateListener cardUpdateListener;

    public void setCardUpdateListener(CardUpdateListener listener) {
        this.cardUpdateListener = listener;
    }
    public Player(int[] bounds, int id) {  // 修改构造函数参数
        this.bounds = bounds;
        this.id = id;
        this.count = 27;
        this.hist = new int[3][0];
        this.last = new int[0];
        this.state = 0;
        this.time = System.currentTimeMillis();
    }

    public void processPlayer(Bitmap sourceBitmap, Yolov8Ncnn yolov8ncnn, Context context) {
//        ImageUtils.saveBitmap(context, sourceBitmap);
        Bitmap playerBitmap = ImageUtils.cropBitmap(sourceBitmap, this.bounds);
        int[] yoloList = new int[30];
        Arrays.fill(yoloList, 60);
        yolov8ncnn.recognizeImage(playerBitmap, yoloList, 320);

//        ImageUtils.saveBitmap(context, playerBitmap);
        playerBitmap.recycle();

        System.arraycopy(hist, 0, hist, 1, hist.length - 1);
        yoloList=CardUtils.trimArray(yoloList);
        hist[0] = yoloList;

        if (System.currentTimeMillis() - time > 2000) {
            handlePlayerState(context);
        }
        if (state == 1 && allZeroInHistory()) {
            state = 0;
        }
    }

    private void handlePlayerState(Context context) {
        int[] current = hist[0];
        if (!CardUtils.isEmpty(current) &&
                (!Arrays.equals(current, last) || state == 0) &&
                isNotChangeWithHistory()) {
            updateStatus(current, context);
        } else if (isNewCardDetected() &&
                (!Arrays.equals(hist[hist.length - 1], last) ||
                        state == 0)) {
            updateStatus(hist[hist.length - 1], context);
        }
    }

    private void updateStatus(int[] cards, Context context) {
        if(CardUtils.analyzeCardType(cards).equals("Unknown")) return;
        state = 1;
        time = System.currentTimeMillis();
        last = cards.clone();
        count -= cards.length;

        showCard(cards);
    }

    private boolean isNewCardDetected() {
        int oldest = hist.length - 1;
        if (CardUtils.isEmpty(hist[oldest])) return false;

        for (int i = 0; i < oldest; i++) {
            if(!CardUtils.isSubset(hist[i],hist[oldest])){
                return false;
            }
        }
        return true;
    }

    private boolean isNotChangeWithHistory() {
        for (int i = 1; i < hist.length; i++) {
            if (!Arrays.equals(hist[i], hist[0])) return false;
        }
        return true;
    }

    private boolean allZeroInHistory() {
        for (int[] record : hist) {
            if (!CardUtils.isEmpty(record)) return false;
        }
        return true;
    }

    private void showCard(int[] playerCard) {
        String sb = "玩家" + id + " 出牌: " +  // 修改显示格式
                CardUtils.analyzeCardType(playerCard) +" "+
                CardUtils.cardsToString(playerCard);
        Log.d("GameLog", sb);
        if (cardUpdateListener != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                cardUpdateListener.onCardsUpdated(CardUtils.analyzeCardType(playerCard),CardUtils.cardsToString(playerCard),playerCard,id);  // 直接使用id
            });
        }
    }
    public String processFinalCards(Bitmap sourceBitmap, Yolov8Ncnn yolov8ncnn, Context context) {
        // 裁剪图像并识别牌面（与 processPlayer 相同）
        Bitmap playerBitmap = ImageUtils.cropBitmap(sourceBitmap, this.bounds);
        int[] yoloList = new int[30];
        Arrays.fill(yoloList, 60);
        yolov8ncnn.recognizeImage(playerBitmap, yoloList, 320);
        playerBitmap.recycle();

        // 清理识别结果并更新历史记录（保留部分逻辑）
        yoloList = CardUtils.trimArray(yoloList);

        // 核心逻辑：对比 last 并检查 count 是否归零
        if (!Arrays.equals(yoloList, last)) {
            count -= yoloList.length;
            if (count == 0) {
                // 更新状态并触发回调
                last = yoloList.clone();
                return CardUtils.cardsToString(yoloList);
            }
        }
        return null;
    }

}