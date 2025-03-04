package com.tencent.yolov8ncnn;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface GameRecordDao {
    @Insert
    void insert(GameRecord record);

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    List<GameRecord> getAll();

    @Query("SELECT * FROM game_records WHERE gameId = :id")
    GameRecord getById(int id);

    @Query("DELETE FROM game_records WHERE gameId = :id")
    void delete(int id);
}