package com.tencent.yolov8ncnn;

import android.app.Application;

import androidx.room.Room;

public class MyApp extends Application {
    private static AppDatabase database;


    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(this, AppDatabase.class, "game-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}