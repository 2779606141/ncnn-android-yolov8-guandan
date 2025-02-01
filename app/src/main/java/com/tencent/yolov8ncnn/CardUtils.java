package com.tencent.yolov8ncnn;

import java.util.Arrays;

public class CardUtils {
    public static int[] convertYoloListToPlayerCard(int[] yoloList) {
        int[] playerCard = new int[16];
        for (int id : yoloList) {
            if (id >= 0 && id < 52) {
                int cardValue = id / 4 + 1;
                playerCard[cardValue]++;
            } else if (id == 52) {
                playerCard[14]++;
            } else if (id == 53) {
                playerCard[15]++;
            } else {
                break;
            }
        }
        return playerCard;
    }

    public static int countCard(int[] playerCard) {
        return Arrays.stream(playerCard).sum();
    }

    public static boolean isEmpty(int[] playerCard) {
        return Arrays.stream(playerCard).allMatch(count -> count == 0);
    }
}