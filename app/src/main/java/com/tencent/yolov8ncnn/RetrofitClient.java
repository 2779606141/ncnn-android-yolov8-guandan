package com.tencent.yolov8ncnn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();
    private static final String BASE_URL = "http://192.168.3.109:9111/";

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();

                // 从自定义SharedPreferences获取token
                SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                String token = prefs.getString("token", "");
                Log.d("RetrofitClient", "Token: " + token);

                if (!token.isEmpty()) {
                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                }
                return chain.proceed(original);
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}