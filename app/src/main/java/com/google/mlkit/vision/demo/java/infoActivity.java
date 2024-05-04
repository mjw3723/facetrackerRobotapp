package com.google.mlkit.vision.demo.java;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.demo.R;

public class infoActivity extends AppCompatActivity {

    static int cnt = 15;
    int temp = 15;
    final long DELAY_TIME = cnt*1000; // 10초 지연 시간
    public static ImageView infoview;
    TextView textView;

    private Handler handler;
    private Runnable runnable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);




        infoview = findViewById(R.id.infoView);
        String option = getIntent().getStringExtra("option");
        if(option !=null && option.equals("405")){
            infoview.setImageResource(R.drawable.class405);
        }else if(option !=null && option.equals("216")){
            infoview.setImageResource(R.drawable.class216);
        }else if(option !=null && option.equals("217")){
            infoview.setImageResource(R.drawable.class217);
        }else if(option !=null && option.equals("212")){
            infoview.setImageResource(R.drawable.class212);
        }
        final Runnable delayedAction = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(infoActivity.this, LivePreviewActivity.class);
                startActivity(intent);
                finish();
            }
        };

        Handler mHandler = new Handler();
        mHandler.postDelayed(delayedAction, DELAY_TIME);

        textView = findViewById(R.id.textView2);
        handler = new Handler(Looper.getMainLooper());
        // 이벤트 발생 코드
        // 다시 자신을 호출하여 1초마다 반복
        runnable = new Runnable() {
            @Override
            public void run() {
                // 이벤트 발생 코드
                cnt -=1;
                textView.setText(String.valueOf(cnt));
                if(cnt !=0) {
                    // 다시 자신을 호출하여 1초마다 반복
                    handler.postDelayed(this, 1000);
                }else{
                    cnt = temp;
                    finish();
                }

            }
        };

        // 처음에 한 번 실행하고, 이후에는 1초마다 반복
        handler.postDelayed(runnable, 1000);

        Button backButton = findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
                handler.removeCallbacksAndMessages(null);
                Intent intent = new Intent(infoActivity.this, LivePreviewActivity.class);
                startActivity(intent);
                cnt = temp;
                finish();
            }
        });



    }
}
