package com.google.mlkit.vision.demo.java;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import com.google.mlkit.vision.demo.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;
import java.util.Objects;

public class ChatGPTClient extends Thread{
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "*********";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private Handler mHandler;
    public static String text;
    public ChatGPTClient(String text,Handler mHandler) {
        this.text = text; // 생성자에서 전달된 값을 클래스 변수에 저장
        this.mHandler = mHandler;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        Log.d("음성출력", "question: "+text);
        OkHttpClient client = new OkHttpClient();
        JSONObject object = new JSONObject();
        try {
            JSONArray messagesArray = new JSONArray();

            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put("role","assistant");
            assistantMessage.put("content","너는 앞으로 부산 경성대학교 메카트로닉스학과 뚤이라는 로봇이야. 누구냐고 물어보면 이렇게 말하면 돼. 응답할때는 존댓말을 항상 써줘.");
            messagesArray.put(assistantMessage);

            // 사용자 메시지 추가
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", text);
            messagesArray.put(userMessage);

            // 'messages' 배열을 요청 본문에 추가
            object.put("messages", messagesArray);
            object.put("model", "gpt-3.5-turbo");
            object.put("max_tokens", 4000);
            object.put("temperature", 0);
        } catch (JSONException e) {
            Log.d("chat-gpt", "chat-gpt");
            Log.d("chat-gpt", Objects.requireNonNull(e.getMessage()));
        }
        RequestBody body = RequestBody.create(object.toString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("chat-gpt", "chat-gpt");
                Log.d("chat-gpt", "Failed to load response due to " + e.getMessage());
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        JSONObject result = ((JSONArray) jsonArray).getJSONObject(0).getJSONObject("message");
                        String content = result.getString("content");
                        String content_trim = content.trim();
                        Message result_message = mHandler.obtainMessage(1,content_trim);
                        mHandler.sendMessage(result_message);
                        Log.d("음성출력", "message: "+result_message.obj);
                    } catch (JSONException e) {
                        Log.d("chat-gpt", "chat-gpt");
                        Log.d("chat-gpt", Objects.requireNonNull(e.getMessage()));
                    }
                } else {
                    Log.d("chat-gpt", "Failed to load response due to " + response.body().string());
                }
            }
        });
    }
}