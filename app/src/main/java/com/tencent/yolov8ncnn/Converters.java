package com.tencent.yolov8ncnn;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Date;
import java.util.List;

public class Converters {
    private static final Gson gson = new Gson();

    // List<List<String>> 转换
    @TypeConverter
    public static List<List<String>> fromJsonToCards(String value) {
        return gson.fromJson(value, new TypeToken<List<List<String>>>(){}.getType());
    }

    @TypeConverter
    public static String cardsToJson(List<List<String>> cards) {
        return gson.toJson(cards);
    }

    // boolean[] 转换
    @TypeConverter
    public static boolean[] fromJsonToBooleanArray(String value) {
        return gson.fromJson(value, boolean[].class);
    }

    @TypeConverter
    public static String booleanArrayToJson(boolean[] array) {
        return gson.toJson(array);
    }

    // int[] 转换
    @TypeConverter
    public static int[] fromJsonToIntArray(String value) {
        return gson.fromJson(value, int[].class);
    }

    @TypeConverter
    public static String intArrayToJson(int[] array) {
        return gson.toJson(array);
    }

    // Date 转换
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}