package com.tencent.yolov8ncnn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    public static String cardsToString(int[] yoloList) {
        StringBuilder cardString = new StringBuilder();
        // 创建花色映射数组
        final String[] SUIT_NAMES = {"H", "D", "C", "S"};

        for (int id : yoloList) {
            if (id < 0 || id >= 54) continue;  // 忽略无效ID
            if (id < 52) {
                // 普通牌处理逻辑
                int rank = (id / 4) + 1;
                String suit = SUIT_NAMES[id % 4];
                if(cardString.length() > 0) cardString.append(" ");
                cardString.append(rank).append(suit);
            } else if (id == 52) {
                // 小王
                if(cardString.length() > 0) cardString.append(" ");
                cardString.append("RJ"); // 使用"BJ"代表小王
            } else if (id == 53) {
                // 大王
                if(cardString.length() > 0) cardString.append(" ");
                cardString.append("BJ"); // 使用"RJ"代表大王
            }
        }
        return cardString.toString();
    }
    public static int[] trimArray(int[] inputArray) {
        if (inputArray == null || inputArray.length == 0) {
            return inputArray;
        }
        int index = 0;
        // 找到第一个大于60的元素的位置
        for (; index < inputArray.length; index++) {
            if (inputArray[index] > 53) {
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
        return CardTypeUtils.getCardType(convertYoloListToCards(yoloList),convertCardFaceToNumber(GameRecorder.universalCard));
    }
    public static int convertCardFaceToNumber(String cardFace) {
        // 使用 HashMap 存储牌面到数字的映射关系
        HashMap<String, Integer> cardFaceMap = new HashMap<>();
        cardFaceMap.put("A", 1);
        cardFaceMap.put("2", 2);
        cardFaceMap.put("3", 3);
        cardFaceMap.put("4", 4);
        cardFaceMap.put("5", 5);
        cardFaceMap.put("6", 6);
        cardFaceMap.put("7", 7);
        cardFaceMap.put("8", 8);
        cardFaceMap.put("9", 9);
        cardFaceMap.put("10", 10);
        cardFaceMap.put("J", 11);
        cardFaceMap.put("Q", 12);
        cardFaceMap.put("K", 13);

        // 转换为大写（或小写）以确保不区分大小写
        return cardFaceMap.get(cardFace.toUpperCase());
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

//    public static boolean isEmpty(int[] playerCard) {
//        return Arrays.stream(playerCard).allMatch(count -> count == 0);
//    }
    public static boolean isEmpty(int[] playerCard) {
        return playerCard.length==0;
    }

    public static boolean isSubset(int[] arr1, int[] arr2) {
        if (arr1 == null || arr2 == null || arr1.length > arr2.length) {
            return false;
        }

        int i = 0, j = 0;
        while (i < arr1.length && j < arr2.length) {
            if (arr1[i] < arr2[j]) {
                // 如果arr1当前元素比arr2小，说明arr1有arr2没有的元素
                return false;
            } else if (arr1[i] != arr2[j]) {
                // 如果不相等，则移动arr2的指针
                j++;
            } else {
                // 当前元素相等，移动两个指针
                i++;
                j++;
            }
        }

        // 如果i达到了arr1的长度，意味着arr1的所有元素都在arr2中找到了对应
        return i == arr1.length;
    }
}