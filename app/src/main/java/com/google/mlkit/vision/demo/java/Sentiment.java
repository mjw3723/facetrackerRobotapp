package com.google.mlkit.vision.demo.java;

import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Sentiment extends Thread{

    private static final String API_URL = "https://naveropenapi.apigw.ntruss.com/sentiment-analysis/v1/analyze";
    private static final String API_ID = "************";
    private static final String API_KEY = "*****************";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private Handler cHandler;
    public static String text;
    public Sentiment(String text, Handler cHandler){
        this.text = text; // 생성자에서 전달된 값을 클래스 변수에 저장
        this.cHandler = cHandler;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run(){
        OkHttpClient client = new OkHttpClient();
        JSONObject object = new JSONObject();
        try{
            object.put("content",text);
        }catch (Exception e){

        }

        RequestBody body = RequestBody.create(object.toString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .header("X-NCP-APIGW-API-KEY-ID",API_ID)
                .header("X-NCP-APIGW-API-KEY", API_KEY)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("뚤-api", "CLOVA");
                Log.d("뚤-api", "Failed to load response due to " + e.getMessage());
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {

                        String responseBody = response.body().string();
                        // JSON 응답을 파싱하여 sentiment 추출
                        jsonObject = new JSONObject(responseBody);
                        String sentiment = jsonObject.getJSONObject("document").getString("sentiment");
                        String sentiment_trim = sentiment.trim();
                        Message result_message = cHandler.obtainMessage(1,sentiment_trim);
                        cHandler.sendMessage(result_message);
                    } catch (JSONException e) {
                        Log.d("뚤-api", "CLOVA");
                        Log.d("뚤-api", Objects.requireNonNull(e.getMessage()));
                    }
                } else {
                    Log.d("뚤-api", "Failed to load response due to " + response.body().string());
                }
            }
        });

    }

}
