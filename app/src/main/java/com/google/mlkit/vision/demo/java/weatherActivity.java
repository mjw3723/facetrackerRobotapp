package com.google.mlkit.vision.demo.java;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.demo.R;

public class weatherActivity extends AppCompatActivity {
    WebView webView;
    String source;
    private static final long DELAY_TIME = 15000; // 15초 지연 시간

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        webView = findViewById(R.id.weather_webview);
        //WebView 자바스크립트 활성화
        webView.getSettings().setJavaScriptEnabled(true);
        // 자바스크립트인터페이스 연결
        // 이걸 통해 자바스크립트 내에서 자바함수에 접근할 수 있음.
        webView.addJavascriptInterface(new MyJavascriptInterface(), "Android");
        // 페이지가 모두 로드되었을 때, 작업 정의
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 자바스크립트 인터페이스로 연결되어 있는 getHTML를 실행
                // 자바스크립트 기본 메소드로 html 소스를 통째로 지정해서 인자로 넘김
                view.loadUrl("javascript:window.Android.getHtml(document.getElementsByTagName('body')[0].innerHTML);");
            }
        });
        //지정한 URL을 웹 뷰로 접근하기
        webView.loadUrl("https://search.naver.com/search.naver?query=부산광역시남구대연동날씨");

        final Runnable delayedAction = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(weatherActivity.this, LivePreviewActivity.class);
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
                Intent intent = new Intent(weatherActivity.this, LivePreviewActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public class MyJavascriptInterface {
        @JavascriptInterface
        public void getHtml(String html) {
            //위 자바스크립트가 호출되면 여기로 html이 반환됨
            source = html;
        }
    }
}
