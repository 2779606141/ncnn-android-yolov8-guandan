package com.tencent.yolov8ncnn;

public interface CardUpdateListener {
    void onCardsUpdated(String type,String s, int[] playedCards, int id);
}