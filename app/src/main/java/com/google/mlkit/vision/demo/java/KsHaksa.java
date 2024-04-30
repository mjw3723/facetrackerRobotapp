package com.google.mlkit.vision.demo.java;

import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.RequiresApi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.IOException;
import java.time.LocalDate;

public class KsHaksa extends Thread{

    private String text; // 변수 선언
    private Handler mHandler;

    // 생성자 정의
    public KsHaksa(String text, Handler mHandler) {
        this.text = text; // 생성자에서 전달된 값을 클래스 변수에 저장
        this.mHandler = mHandler;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        Document doc = null;
        try {
            int currentYear = LocalDate.now().getYear();
            doc = Jsoup.connect("https://kscms.ks.ac.kr/haksa/CMS/HaksaScheduleMgr/list.do?mCode=MN104&sy="+ currentYear).get();
            Elements tables = doc.getElementsByAttributeValue("class", "haksa-schedule-tbl");
            for (Element table : tables) {
                Elements thElements = table.getElementsByAttributeValue("class","trbody");

                // 가져온 "th" 태그들 출력
                for (Element th : thElements) {
                    String firstTwoCharacters = th.text().substring(0, 2);

                    int number = Integer.parseInt(firstTwoCharacters);
                    if(Integer.parseInt(text) == number){
                        Elements pdesc = th.getElementsByAttributeValue("class", "pdesc");
                        if(pdesc.text().length() <= 0) {
                            pdesc = th.getElementsByAttributeValue("class", "pdesc  cbrown");
                        }
                        int day= Integer.parseInt(th.text().substring(3, 5));
                        Message message = mHandler.obtainMessage(1, number+"월"+day+"일"+pdesc.text());
                        mHandler.sendMessage(message);
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
