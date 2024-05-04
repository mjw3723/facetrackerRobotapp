package com.google.mlkit.vision.demo.java;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.demo.R;

public class infoActivity extends AppCompatActivity {
    private static final long DELAY_TIME = 10000; // 10초 지연 시간
    public static ImageView infoview;

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


        Button backButton = findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacksAndMessages(null);
                Intent intent = new Intent(infoActivity.this, LivePreviewActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
