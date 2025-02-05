package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import java.util.Arrays;

public class Player {
    private static final String[] CARD_NAMES = {" ", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "大王", "小王"};

    final int[] bounds;
    final String name;
    int count;
    final int[][] hist;
    int[] last;
    int state;
    long time;
    private CardUpdateListener cardUpdateListener;

    public void setCardUpdateListener(CardUpdateListener listener) {
        this.cardUpdateListener = listener;
    }
    public Player(int[] bounds, String name) {
        this.bounds = bounds;
        this.name = name;
        this.count = 27;
        this.hist = new int[3][16];
        this.last = new int[16];
        this.state = 0;
        this.time = System.currentTimeMillis();
    }

    public void processPlayer(Bitmap sourceBitmap, Yolov8Ncnn yolov8ncnn, Context context) {
        Bitmap playerBitmap = ImageUtils.cropBitmap(sourceBitmap, this.bounds);
        int[] yoloList = new int[30];
        Arrays.fill(yoloList, 60);
        yolov8ncnn.recognizeImage(playerBitmap, yoloList, 640);
        playerBitmap.recycle();

        System.arraycopy(hist, 0, hist, 1, hist.length - 1);
        hist[0] = CardUtils.convertYoloListToPlayerCard(yoloList);

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
        state = 1;
        time = System.currentTimeMillis();
        last = cards.clone();
        count -= CardUtils.countCard(cards);

        showCardToast(cards, context);
            if (cardUpdateListener != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    cardUpdateListener.onCardsUpdated(cards,Character.getNumericValue(name.charAt(name.length() - 1)));
                });
            }
    }

    private boolean isNewCardDetected() {
        int oldest = hist.length - 1;
        if (CardUtils.isEmpty(hist[oldest])) return false;

        for (int i = 0; i < oldest; i++) {
            for (int j = 0; j < hist[i].length; j++) {
                if (hist[oldest][j] < hist[i][j]) return false;
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

    private void showCardToast(int[] playerCard, Context context) {
        StringBuilder sb = new StringBuilder(name).append(" 出牌: ");
        for (int i = 1; i < playerCard.length; i++) {
            if (playerCard[i] != 0) {
                for (int j = 0; j < playerCard[i]; j++) {
                    sb.append(CARD_NAMES[i]).append(" ");
                }
            }
        }
        Log.d("GameLog", sb.toString());
//        new Handler(Looper.getMainLooper()).post(() ->
//                Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show()
//        );
    }
}