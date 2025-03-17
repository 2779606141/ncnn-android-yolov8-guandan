package com.tencent.yolov8ncnn;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class GameRecorder {
    private final List<List<List<String>>> playerCards = new ArrayList<>();
    private final List<List<String>> remainingCards = new ArrayList<>(PLAYER_COUNT);
    private final boolean[] finishedPlayers = new boolean[4];
    private int firstPlayerId = -1; // 第一次识别到的玩家ID
    private int currentTurn = 0; // 当前轮次中的玩家索引
    private int[] order = new int[PLAYER_COUNT];
    public static String universalCard="";
    private static final int PLAYER_COUNT = 4;
    private final int[] finishOrder = new int[PLAYER_COUNT];
    private int finishIndex = 0;


    public GameRecorder() {
        for (int i = 0; i < PLAYER_COUNT; i++) {
            playerCards.add(new ArrayList<>()); // 初始化每个玩家的出牌列表
            remainingCards.add(new ArrayList<>()); // 初始化每个玩家的剩余手牌
        }
        universalCard="";
    }

    public void recordPlay(int actualPlayerId, String type, String cards) { // 增加参数type
        if (firstPlayerId == -1) {
            firstPlayerId = actualPlayerId;
            for (int i = 0; i < 4; i++) {
                order[i] = (firstPlayerId + i) % 4;
            }
        }
        if (finishedPlayers[actualPlayerId]) return;
        handleTurn(actualPlayerId, type, cards); // 传递type
    }

    private void handleTurn(int actualPlayerId, String type, String cards) {
        while (order[currentTurn % 4] != actualPlayerId) {
            if (!finishedPlayers[order[currentTurn % 4]])
                appendPlayerCards(order[currentTurn % 4], "PASS", "PASS"); // 修改了这一行
            currentTurn++;
        }

        appendPlayerCards(actualPlayerId, type, cards);
        currentTurn++;
    }

    private void appendPlayerCards(int playerId, String type, String cards) { // 修改了这个方法签名
        List<String> playInfo = Arrays.asList(type, cards); // 创建包含出牌类型和详情的列表
        playerCards.get(playerId).add(playInfo);
    }

    public void setPlayerFinished(int playerId) {
        if (!finishedPlayers[playerId]) { // 确保只记录一次
            finishOrder[finishIndex++] = playerId; // 在finishOrder中记录完成玩家的ID
        }
        finishedPlayers[playerId] = true;
    }

    public String[] getPlayerCards(int playerId) {
        List<String> cardsOnly = new ArrayList<>();
        for (List<String> playInfo : playerCards.get(playerId)) {
            // 假设playInfo的第一个元素是出牌类型，第二个元素是实际出牌详情
            String cards = playInfo.get(1);
            cardsOnly.add(cards);
        }
        return cardsOnly.toArray(new String[0]);
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
    public void updateRemainingCards(int playerId, String cards) {
        if (playerId < 0 || playerId >= PLAYER_COUNT) {
            Log.e("GameRecorder", "Invalid player ID: " + playerId);
            return;
        }
        if (cards == null) return;
        remainingCards.get(playerId).add(cards);
    }
    public void saveGameRecord() {
        new Thread(() -> {
            try {
                // 创建要发送的GameRecord对象
                GameRecords record = new GameRecords();
                Gson gson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                        .create();

                // 转换数据为JSON字符串
                record.setPlayer0Card(gson.toJson(playerCards.get(0)));
                record.setPlayer1Card(gson.toJson(playerCards.get(1)));
                record.setPlayer2Card(gson.toJson(playerCards.get(2)));
                record.setPlayer3Card(gson.toJson(playerCards.get(3)));
                record.setPlayOrder(gson.toJson(order));
                record.setFinishOrder(gson.toJson(finishOrder));
                record.setWildCard(universalCard);
                record.setGameTime(new Date());
                record.setTotalRounds(currentTurn);
                record.setIsComplete(isGameEnded());
                record.setWin(finishOrder[0]==0||finishOrder[0]==2);
                record.setUserId(1);
                // 假设当前游戏数据状态
//                List<List<List<String>>> playerCards = new ArrayList<>();
//                playerCards.add(Arrays.asList(
//                        Arrays.asList("SINGLE", "HK"), // 红桃K单出
//                        Arrays.asList("PAIR", "DJ DQ"), // 方片J和方片Q对子
//                        Arrays.asList("PASS", "") // 跳过
//                ));
//                playerCards.add(Arrays.asList(
//                        Arrays.asList("PASS", ""), // 跳过
//                        Arrays.asList("TRIO", "H10 C10 S10"), // 包含三个10的三带
//                        Arrays.asList("SEQUENCE", "D2 D3 D4") // 方片2、3、4顺子
//                ));
//                playerCards.add(Arrays.asList(
//                        Arrays.asList("BOMB", "C5 C5 C5 C5"), // 四个梅花5炸弹
//                        Arrays.asList("PASS", ""), // 跳过
//                        Arrays.asList("PAIR", "DK CK") // 黑桃K和梅花K对子
//                ));
//                playerCards.add(Arrays.asList(
//                        Arrays.asList("PASS", ""), // 跳过
//                        Arrays.asList("PASS", ""), // 再次跳过
//                        Arrays.asList("TRIO", "HQ CQ SQ") // 包含三个Q的三带
//                ));
//                int[] order = {0, 1, 2, 3};          // 出牌顺序
//                int[] finishOrder = {3, 1, 2, 0};     // 完成顺序
//                String universalCard = "AH";         // 万能牌
//                int currentTurn = 3;                 // 总轮次
//                record.setPlayer0Card(gson.toJson(playerCards.get(0)));
//                record.setPlayer1Card(gson.toJson(playerCards.get(1)));
//                record.setPlayer2Card(gson.toJson(playerCards.get(2)));
//                record.setPlayer3Card(gson.toJson(playerCards.get(3)));
//                record.setPlayOrder(gson.toJson(order));                // "[0,1,2,3]"
//                record.setFinishOrder(gson.toJson(finishOrder));        // "[3,1,2,0]"
//                record.setWildCard(universalCard);                     // "AH"
//                record.setGameTime(new Date());                        // 当前时间
//                record.setTotalRounds(currentTurn);                    // 7
//                record.setIsComplete(true);
//                record.setUserId(1);
//                record.setWin(finishOrder[0]==0||finishOrder[0]==2);
                // 发送网络请求
                sendToBackend(record);
            } catch (Exception e) {
                Log.e("SaveError", "保存失败", e);
            }
        }).start();
    }
    private void sendToBackend(GameRecords record) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.createGameRecord(record);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("Network", "上传成功");
                } else {
                    Log.e("Network", "服务器错误: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Network", "网络请求失败", t);
            }
        });
    }
}