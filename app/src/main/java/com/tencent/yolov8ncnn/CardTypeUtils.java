package com.tencent.yolov8ncnn;

import java.util.*;

public class CardTypeUtils {
    // 牌型映射表
    private static final String[] CARD_TYPES = {
            "Single", "Pair", "Trips",
            "ThreePair", "ThreeWithTwo",
            "TripsPair", "Straight", "Boom"
    };
    public static String getCardType(Card[] cards) {
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

        // 判断基础牌型
        String baseType = checkBaseType(count, ranks, cards.length);

        // 组合判断（预留同花顺扩展）
        return enhanceTypeCheck(baseType, ranks, suits);
    }

    public static String checkBaseType(int[] count, List<Integer> ranks, int total) {
        // 优先检查炸弹（四张及以上）

        for (int c : count) {
            if (c >= 4) {
                if(checkBomb(count)){
                    return "Bomb";
                }
                else{
                    return "Unknown";
                }
            }
        }

        // 提取有效点数并排序（排除0和大小王）
        List<Integer> points = new ArrayList<>();
        for (int i = 1; i <= 13; i++) {
            if (count[i] > 0) points.add(i);
        }
        Collections.sort(points);

        // 根据牌数判断类型
        switch (ranks.size())  {
            case 1: return "Single";
            case 2: return checkPair(count);
            case 3: return checkTrips(count);
            case 5: return checkFiveCombination(count, points);
            case 6: return checkSixCombination(count, points);
            default: return "Unknown";
        }
    }

    private static String enhanceTypeCheck(String baseType,
                                           List<Integer> ranks,
                                           Set<String> suits) {
        // 同花顺判断示例（需同时满足顺子和同花）
        if ("Straight".equals(baseType) && suits.size()  == 1) {
            return "StraightFlush";
        }
        return baseType;
    }

    public static boolean checkBomb(int[] count) {
        int bombRank = -1;

        // STEP 1: 找出是否存在≥4张的同点数牌
        for (int i = 0; i < count.length;  i++) {
            if (count[i] >= 4) {
                bombRank = i;  // 记录可能的王牌点数
            }
        }

        // 未找到符合条件的基础牌型
        if (bombRank == -1) return false;

        // STEP 2: 验证其他牌的数量为0（排除44441式混合牌）
        int otherCount = 0;
        for (int i = 0; i < count.length;  i++) {
            if (i != bombRank) {
                otherCount += count[i];
            }
        }
        return otherCount == 0;
    }

    // 对子判断
    private static String checkPair(int[] count) {
        for (int c : count) {
            if (c == 2) return "Pair";
        }
        return "Unknown";
    }

    // 三张判断
    private static String checkTrips(int[] count) {
        for (int c : count) {
            if (c == 3) return "Trips";
        }
        return "Unknown";
    }

    // 五张组合判断（顺子/三带二）
    private static String checkFiveCombination(int[] count, List<Integer> points) {
        // 三带二判断
        boolean hasThree = false, hasTwo = false;
        for (int c : count) {
            if (c == 3) hasThree = true;
            if (c == 2) hasTwo = true;
        }
        if (hasThree && hasTwo) return "ThreeWithTwo";

        // 顺子判断
        if (points.size()  == 5 && (isContinuous(points) || isAceHighStraight(points))) {
            return "Straight";
        }
        return "Unknown";
    }

    // 六张组合判断（三连对/钢板）
    private static String checkSixCombination(int[] count, List<Integer> points) {
        // 钢板判断（连续两个三张）
        if (points.size()  == 2) {
            return (count[points.get(0)] == 3 && count[points.get(1)] == 3 &&
                    points.get(1)  - points.get(0)  == 1) ? "TripsPair" : "Unknown";
        }

        // 三连对判断（三个连续对子）
        if (points.size()  == 3) {
            boolean isThreePair = points.get(2)  - points.get(0)  == 2;
            for (int p : points) {
                if (count[p] != 2) {
                    isThreePair = false;
                    break;
                }
            }
            return isThreePair ? "ThreePair" : "Unknown";
        }
        return "Unknown";
    }

    // 标准顺子判断（连续五张）
    private static boolean isContinuous(List<Integer> points) {
        for (int i = 0; i < 4; i++) {
            if (points.get(i  + 1) - points.get(i)  != 1) return false;
        }
        return true;
    }

    // 特殊顺子判断（A-2-3-4-5 或 10-J-Q-K-A）
    private static boolean isAceHighStraight(List<Integer> points) {
        return points.equals(Arrays.asList(1,  10, 11, 12, 13)) ||
                points.equals(Arrays.asList(1,  2, 3, 4, 5));
    }
}