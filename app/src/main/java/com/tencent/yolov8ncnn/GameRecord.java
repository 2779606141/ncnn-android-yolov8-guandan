package com.tencent.yolov8ncnn;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;
import java.util.List;

@Entity(tableName = "game_records")
@TypeConverters({Converters.class})
public class GameRecord {
    @PrimaryKey(autoGenerate = true)
    public int gameId;

    public String userID;          // 需要记录的玩家0身份
    public String universalCard;      // 万能牌
    public List<List<String>> playerCards; // 四个玩家的出牌记录
    public boolean[] playerStatus;    // 玩家完成状态
    public int[] playOrder;           // 初始出牌顺序
    public int currentTurn;           // 当前总出牌次数
    public Date timestamp;            // 记录时间1

    // 空构造方法（Room需要）
    public GameRecord() {}
}