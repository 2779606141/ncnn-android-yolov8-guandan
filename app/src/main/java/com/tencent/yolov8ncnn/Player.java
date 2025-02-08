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
        this.hist = new int[3][0];
        this.last = new int[0];
        this.state = 0;
        this.time = System.currentTimeMillis();
    }

    public void processPlayer(Bitmap sourceBitmap, Yolov8Ncnn yolov8ncnn, Context context) {

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
            Log.d("111","111");
            updateStatus(current, context);

        } else if (isNewCardDetected() &&
                (!Arrays.equals(hist[hist.length - 1], last) ||
                        state == 0)) {
            Log.d("111","222");
            updateStatus(hist[hist.length - 1], context);
        }
    }

    private void updateStatus(int[] cards, Context context) {
        state = 1;
        time = System.currentTimeMillis();
        last = cards.clone();
        count -= cards.length;

        showCard(cards, context);
//            if (cardUpdateListener != null) {
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    cardUpdateListener.onCardsUpdated(cards,Character.getNumericValue(name.charAt(name.length() - 1)));
//                });
//            }
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

    private void showCard(int[] playerCard, Context context) {
        String sb = name + " 出牌: " +
                CardUtils.analyzeCardType(playerCard) +" "+
                CardUtils.cardsToString(playerCard);
//        for (int i = 1; i < playerCard.length; i++) {
//            if (playerCard[i] != 0) {
//                for (int j = 0; j < playerCard[i]; j++) {
//                    sb.append(CARD_NAMES[i]).append(" ");
//                }
//            }
//        }
        Log.d("GameLog", sb);
//        new Handler(Looper.getMainLooper()).post(() ->
//                Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show()
//        );
    }

    public void lastProcessPlayer(Bitmap sourceBitmap, Yolov8Ncnn yolov8ncnn, Context context) {
        Bitmap playerBitmap = ImageUtils.cropBitmap(sourceBitmap,  this.bounds);
        int[] yoloList = new int[30];
        Arrays.fill(yoloList,  60);
        yolov8ncnn.recognizeImage(playerBitmap,  yoloList, 320);
        playerBitmap.recycle();

        int[] currentCards = CardUtils.convertYoloListToPlayerCard(yoloList);
        if (CardUtils.isEmpty(currentCards))  {
            // 未识别到目标，不用管
            return;
        }

        int detectedCardCount = CardUtils.countCard(currentCards);
        if (count - detectedCardCount == 0) {
            // count减去此次识别到的牌数为0，加入记录
            // 这里假设加入记录的逻辑是将currentCards复制到hist[0]
            updateStatus(currentCards, context);
        } else if (count - detectedCardCount < 0 && (last.length  + count - detectedCardCount) == 0) {
            // count减去此次识别到的牌数小于0且加上last的长度等于0，显示出来
            showCard(currentCards, context);
            if (cardUpdateListener!= null) {
                new Handler(Looper.getMainLooper()).post(()  -> {
                    cardUpdateListener.onCardsUpdated(currentCards,  Character.getNumericValue(name.charAt(name.length()  - 1)));
                });
            }
        }
    }
}