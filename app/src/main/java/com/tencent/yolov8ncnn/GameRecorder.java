package com.tencent.yolov8ncnn;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class GameRecorder {
    private final List<List<String>> playerCards = new ArrayList<>();
    private final boolean[] finishedPlayers = new boolean[4];
    private int firstPlayerId = -1; // 第一次识别到的玩家ID
    private int currentTurn = 0; // 当前轮次中的玩家索引
    private int[] order = new int[4];
    public static String universalCard="";
    private static final int PLAYER_COUNT = 4;


    public GameRecorder() {
        for (int i = 0; i < PLAYER_COUNT; i++) {
            playerCards.add(new ArrayList<>()); // 初始化每个玩家的出牌列表
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
        if (finishedPlayers[actualPlayerId]) return; // 如果该玩家已经出完牌，则不记录
        handleTurn(actualPlayerId, cards);
    }

    private void handleTurn(int actualPlayerId, String cards) {
        while (order[currentTurn % 4] != actualPlayerId) {
            if (!finishedPlayers[order[currentTurn % 4]])
                appendPlayerCards(order[currentTurn % 4], "PASS"); // 空过
            currentTurn++;
        }

        appendPlayerCards(actualPlayerId, cards); // 实际出牌玩家的记录
        currentTurn++;
    }


    private void appendPlayerCards(int playerId, String cards) {
        playerCards.get(playerId).add(cards);
    }


    public void setPlayerFinished(int playerId) {
        finishedPlayers[playerId] = true;
    }

    public String[] getPlayerCards(int playerId) {
        return playerCards.get(playerId).toArray(new String[0]);
    }

    public void generateLog() {
        StringBuilder log = new StringBuilder();
        for (int playIndex = 0; playIndex < (currentTurn + 3) / 4; playIndex++) {
            log.append("轮次: ").append(playIndex + 1).append("\n");
            for (int playerId : order) {
                if (getPlayerCards(playerId).length > playIndex) {
                    String cardsLog = getPlayerCards(playerId)[playIndex];
                    log.append("玩家 ").append(playerId).append(" 出牌内容: ").append(cardsLog).append("\n");
                }
            }
        }
        Log.d("log", log.toString());
        Log.d("count", Integer.toString(currentTurn));
    }
    public boolean isGameEnded() {
        // 检查对位玩家是否完成
        boolean isPairFinished = (finishedPlayers[0] && finishedPlayers[2]) ||
                (finishedPlayers[1] && finishedPlayers[3]);
        if (isPairFinished) return true;

        // 检查完成玩家数量
        int finishedCount = 0;
        for (boolean finished : finishedPlayers) {
            if (finished) finishedCount++;
        }
        return finishedCount >= 3;
    }
    public void saveGameRecord() {
        // 确保在主线程外执行
        new Thread(() -> {
            AppDatabase db = MyApp.getDatabase();
            if (db == null) {
                Log.e("DB", "数据库未初始化");
                return;
            }
            GameRecord record = new GameRecord();

            // 转换数据格式
            record.userID = "player0"; // 根据实际情况设置
            record.universalCard = universalCard;
            record.playerCards = new ArrayList<>(playerCards); // 直接复制List
            record.playerStatus = Arrays.copyOf(finishedPlayers, finishedPlayers.length);
            record.playOrder = Arrays.copyOf(order, order.length);
            record.currentTurn = currentTurn;
            record.timestamp = new Date();

            db.gameRecordDao().insert(record);

            Log.d("Save", "牌局已保存 ID:" + record.gameId);
        }).start();
    }
}