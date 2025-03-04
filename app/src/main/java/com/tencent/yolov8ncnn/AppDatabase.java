package com.tencent.yolov8ncnn;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {GameRecord.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract GameRecordDao gameRecordDao();
}