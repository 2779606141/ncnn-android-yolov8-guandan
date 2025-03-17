package com.tencent.yolov8ncnn;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/BookManager/game/save")
    Call<ResponseBody> createGameRecord(@Body GameRecords gameRecord);
}
