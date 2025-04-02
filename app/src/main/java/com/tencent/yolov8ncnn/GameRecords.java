package com.tencent.yolov8ncnn;

import java.io.Serializable;
import java.util.Date;

public class GameRecords implements Serializable {
    private Integer id;

    private String player0Card;
    private String player1Card;
    private String player2Card;
    private String player3Card;
    private Date gameTime;
    private String playOrder;
    private String finishOrder;
    private Boolean isComplete;

    public Boolean getWin() {
        return isVictory;
    }

    public void setWin(Boolean isvictory) {
        isVictory = isvictory;
    }

    private Boolean isVictory;
    private Integer totalRounds;
    private String wildCard; // 注意拼写与后端一致

    // 必须有无参构造函数
    public GameRecords() {}

    // Getter/Setter 必须完整
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getPlayer0Card() {
        return player0Card;
    }

    public void setPlayer0Card(String player0Card) {
        this.player0Card = player0Card;
    }

    public String getPlayer1Card() {
        return player1Card;
    }

    public void setPlayer1Card(String player1Card) {
        this.player1Card = player1Card;
    }

    public String getPlayer2Card() {
        return player2Card;
    }

    public void setPlayer2Card(String player2Card) {
        this.player2Card = player2Card;
    }

    public String getPlayer3Card() {
        return player3Card;
    }

    public void setPlayer3Card(String player3Card) {
        this.player3Card = player3Card;
    }

    public Date getGameTime() {
        return gameTime;
    }

    public void setGameTime(Date gameTime) {
        this.gameTime = gameTime;
    }

    public String getPlayOrder() {
        return playOrder;
    }

    public void setPlayOrder(String playOrder) {
        this.playOrder = playOrder;
    }

    public String getFinishOrder() {
        return finishOrder;
    }

    public void setFinishOrder(String finishOrder) {
        this.finishOrder = finishOrder;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean complete) {
        isComplete = complete;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }

    public String getWildCard() {
        return wildCard;
    }

    public void setWildCard(String wildCard) {
        this.wildCard = wildCard;
    }

    // 可选：toString方法便于调试
    @Override
    public String toString() {
        return "GameRecord{" +
                "id=" + id +

                ", player0Card='" + player0Card + '\'' +
                ", player1Card='" + player1Card + '\'' +
                ", player2Card='" + player2Card + '\'' +
                ", player3Card='" + player3Card + '\'' +
                ", gameTime=" + gameTime +
                ", playOrder='" + playOrder + '\'' +
                ", finishOrder='" + finishOrder + '\'' +
                ", isComplete=" + isComplete +
                ", totalRounds=" + totalRounds +
                ", universialCard='" + wildCard + '\'' +
                '}';
    }
}
