package com.google.mlkit.vision.demo.java;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class WifiUtil {
    public static void showWifiSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        context.startActivity(intent);
    }
}
