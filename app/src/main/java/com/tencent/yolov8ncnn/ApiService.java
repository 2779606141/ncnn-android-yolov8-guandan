package com.tencent.yolov8ncnn;


import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/BookManager/game/save")
    Call<ResponseBody> createGameRecord(@Body GameRecords gameRecord);

    @POST("/BookManager/user/login")
    Call<Map<String, Object>> login(@Body User user);
}
