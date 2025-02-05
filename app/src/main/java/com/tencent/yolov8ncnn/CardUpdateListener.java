package com.tencent.yolov8ncnn;

public interface CardUpdateListener {
    void onCardsUpdated(int[] playedCards,int id);
}