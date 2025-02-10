package com.tencent.yolov8ncnn;

import java.util.*;

public class CardTypeUtils {
    // 牌型映射表
    private static final String[] CARD_TYPES = {
            "Single", "Pair", "Trips",
            "ThreePair", "ThreeWithTwo",
            "TripsPair", "Straight", "Boom"
    };

    public static String getCardType(Card[] originalCards, int cardu) {
        List<Card> filteredList = new ArrayList<>();
        int universalCount = 0;

        for (Card c : originalCards) {
            if (c.getRank() == cardu && "H".equals(c.getSuit())) {
                universalCount++;
            } else {
                filteredList.add(c);
            }
        }

        // 获取最终使用的牌组
        Card[] cards = universalCount > 0 ?
                filteredList.toArray(new Card[0]) :
                originalCards;
        // 预处理数据
        int[] count = new int[16]; // 点数统计数组
        List<Integer> ranks = new ArrayList<>();
        Set<String> suits = new HashSet<>();

        for (Card card : cards) {
            int rank = card.getRank();
            count[rank]++;
            ranks.add(rank);
            suits.add(card.getSuit());
        }
        Collections.sort(ranks);

        return checkType(count, ranks, suits, universalCount);
    }

    public static String checkType(int[] count, List<Integer> ranks, Set<String> suits, int universalCount) {
        // 统计单张、对子、三张和炸弹的数量
        int singleCount = 0, pairCount = 0, tripsCount = 0, bombCount = 0;
        for (int c : count) {
            if (c == 1) singleCount++;
            if (c == 2) pairCount++;
            if (c == 3) tripsCount++;
            if (c >= 4) bombCount++;
        }

        // 提取有效点数并排序
        List<Integer> points = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            if (count[i] > 0) points.add(i);
        }
        Collections.sort(points);

        if (universalCount == 0) {
            // 判断牌型
            if (bombCount == 1) {
                return points.size() == 1 ? "Bomb" : "Unknown";
            } else if (ranks.size() == 1) {
                return "Single";
            } else if (ranks.size() == 2) {
                return pairCount == 1 ? "Pair" : "Unknown";
            } else if (ranks.size() == 3) {
                return tripsCount == 1 ? "Trips" : "Unknown";
            } else if (ranks.size() == 5) {
                return checkFiveCombination(singleCount, tripsCount, pairCount, points, suits);
            } else if (ranks.size() == 6) {
                if(pairCount==3) return "TripsPair";
                else if(tripsCount==2) return "PairTrips";
                else return "Unknown";
            } else {
                return "Unknown";
            }
        } else if (universalCount == 1) {
            if (bombCount == 1) {
                return points.size() == 1 ? "Bomb" : "Unknown";
            } else if (ranks.size() == 1) {
                return "Pair";
            } else if (ranks.size() == 2) {
                return pairCount == 1 ? "Trips" : "Unknown";
            } else if (ranks.size()==3) {
                return tripsCount==1&&points.size() == 1 ? "Bomb" : "Unknown";
            }else if (ranks.size()==4) {
                if(singleCount==1&&tripsCount==1||pairCount==2) return "ThreeWithTwo";
                else if(points.size()==4) return suits.size() == 1 ? "StraightFlush" : "Straight";
            }else if (ranks.size()==5){
                if(singleCount==1&&pairCount==2) return "TripsPair";
                else if(pairCount==1&&tripsCount==1) return "PairTrips";
                else return "Unknown";
            }
        }
        else if (universalCount == 2) {
            // 两张万能牌时的判断
            if (bombCount == 1) {
                return points.size() == 1 ? "Bomb" : "Unknown";
            }
            else if (ranks.size() == 1) {
                return "Trips";
            } else if (ranks.size() == 2) {
                if (pairCount == 1) return "Bomb";
            } else if (ranks.size() == 3) {
                if (tripsCount == 1) return "Bomb";
                else if(singleCount==1&&pairCount==1) return "TreeWithTwo";
                else if(points.size()==3) return suits.size() == 1 ? "StraightFlush" : "Straight";
            } else if (ranks.size() == 4) {
                if (pairCount == 2) return "TripsPair";
                else if(singleCount==1&&tripsCount==1) return "PairTrips";
            }
        }
        return "Unknown";
    }


    // 五张组合判断（顺子/三带二）
    private static String checkFiveCombination(int singleCount, int tripsCount, int pairCount, List<Integer> points, Set<String> suits) {
        // 三带二判断
        if (tripsCount == 1 && pairCount == 1) return "ThreeWithTwo";

        // 顺子判断
        if (points.size() == 5 && (isContinuous(points) || isAceHighStraight(points))) {
            if (suits.size() == 1) return "StraightFlush";
            return "Straight";
        }

        return "Unknown";
    }


    // 标准顺子判断（连续五张）
    private static boolean isContinuous(List<Integer> points) {
        for (int i = 0; i < 4; i++) {
            if (points.get(i + 1) - points.get(i) != 1) return false;
        }
        return true;
    }

    // 特殊顺子判断（A-2-3-4-5 或 10-J-Q-K-A）
    private static boolean isAceHighStraight(List<Integer> points) {
        return points.equals(Arrays.asList(1, 10, 11, 12, 13)) ||
                points.equals(Arrays.asList(1, 2, 3, 4, 5));
    }
}