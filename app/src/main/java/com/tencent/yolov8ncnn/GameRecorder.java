package com.tencent.yolov8ncnn;

import android.util.Log;

import java.util.Arrays;

public class GameRecorder {
    private final String[][] playerCards = new String[4][]; // 保存每个玩家每轮出的牌
    private final boolean[] hasFinished = new boolean[4];
    private int firstPlayerId = -1; // 第一次识别到的玩家ID
    private int currentTurnIndex = 0; // 当前轮次中的玩家索引
    private int[] order = new int[4];
    public static String universalCard="";


    public GameRecorder() {
        for (int i = 0; i < 4; i++) {
            playerCards[i] = new String[0]; // 初始化为空数组
        }
        universalCard="";
    }

    public void recordPlay(int actualPlayerId, String cards) {
        if (firstPlayerId == -1) {
            firstPlayerId = actualPlayerId; // 设置第一次识别到的玩家为起始玩家
            for (int i = 0; i < 4; i++) {
                order[i] = (firstPlayerId + i) % 4;
            }
        }
        if (hasFinished[actualPlayerId]) return; // 如果该玩家已经出完牌，则不记录
        handleTurn(actualPlayerId, cards);
    }

    private void handleTurn(int actualPlayerId, String cards) {
        while (order[currentTurnIndex % 4] != actualPlayerId) {
            if (!hasFinished[order[currentTurnIndex % 4]])
                appendPlayerCards(order[currentTurnIndex % 4], ""); // 空过
            currentTurnIndex++;
        }

        appendPlayerCards(actualPlayerId, cards); // 实际出牌玩家的记录
        currentTurnIndex++;
    }


    private void appendPlayerCards(int playerId, String cards) {
        String[] newArray = Arrays.copyOf(playerCards[playerId], playerCards[playerId].length + 1);
        newArray[newArray.length - 1] = cards;
        playerCards[playerId] = newArray;
    }


    public void setPlayerFinished(int playerId) {
        hasFinished[playerId] = true;
    }

    public String[] getPlayerCards(int playerId) {
        return playerCards[playerId];
    }


    public void generateLog() {
        StringBuilder log = new StringBuilder();
        for (int playIndex = 0; playIndex < currentTurnIndex / 4 + 1; playIndex++) {
            log.append("轮次: ").append(playIndex + 1).append("\n");
            for (int playerId : order) {
                if (getPlayerCards(playerId).length > playIndex) {
                    String cardsLog = getPlayerCards(playerId)[playIndex];
                    log.append("玩家 ").append(playerId).append(" 出牌内容: ").append(cardsLog).append("\n");
                }
            }
        }
        Log.d("log", log.toString());
        Log.d("count", Integer.toString(currentTurnIndex));
    }
}