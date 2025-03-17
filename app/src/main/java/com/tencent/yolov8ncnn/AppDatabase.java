package com.tencent.yolov8ncnn;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {GameRecord.class}, version = 3)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract GameRecordDao gameRecordDao();
}