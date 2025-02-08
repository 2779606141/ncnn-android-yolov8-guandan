package com.tencent.yolov8ncnn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 扑克牌结构体
class Card {
    private int rank;  // 点数
    private String suit; // 花色

    public Card(int rank, String suit) {
        this.rank  = rank;
        this.suit  = suit;
    }

    // Getters
    public int getRank() { return rank; }
    public String getSuit() { return suit; }
}
public class CardUtils {
    public static Card[] convertYoloListToCards(int[] yoloList) {
        List<Card> cards = new ArrayList<>();
        // 创建花色映射数组
        final String[] SUIT_NAMES = {"H", "D", "C", "S"};

        for (int id : yoloList) {
            if (id < 0) continue;  // 忽略无效ID
            if (id < 52) {
                // 普通牌处理逻辑
                int rank = (id / 4) + 1;
                String suit = SUIT_NAMES[id % 4];
                cards.add(new  Card(rank, suit));
            } else if (id == 52) {
                // 小王
                cards.add(new  Card(14, "Black Joker"));
            } else if (id == 53) {
                // 大王
                cards.add(new  Card(15, "Red Joker"));
            } else {
                break;  // 遇到非法ID立即终止
            }
        }
        return cards.toArray(new  Card[0]);
    }
    public static int[] trimArray(int[] inputArray) {
        if (inputArray == null || inputArray.length == 0) {
            return inputArray;
        }
        int index = 0;
        // 找到第一个大于60的元素的位置
        for (; index < inputArray.length; index++) {
            if (inputArray[index] > 60) {
                break;
            }
        }
        // 创建新的数组，长度为index，即不包括任何大于60的元素
        int[] resultArray = new int[index];
        System.arraycopy(inputArray, 0, resultArray, 0, index);
        Arrays.sort(resultArray);
        return resultArray;
    }
    public static String analyzeCardType(int[] yoloList) {
        return CardTypeUtils.getCardType(convertYoloListToCards(yoloList));
    }


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