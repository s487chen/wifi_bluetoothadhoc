package com.example.wifi_bluetooth;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Utils {
    private static final int REQUEST_ACCESS_FINE_LOCATION = 110;
    private static final int REQUEST_ACCESS_WIFI_STATE = 111;
    private static final int REQUEST_CHANGE_WIFI_STATE =112;
    private static final int REQUEST_CHANGE_NETWORK_STATE=113;
    private static final int REQUEST_ACCESS_NETWORK_STATE=114;
    private static final int REQUEST_INTERNET = 115;
    private static final int REQUEST_BLUETOOTH = 116;
    private static final int REQUEST_BLUETOOTH_ADMIN = 117;

    private static Object lock = new Object();
    private static boolean allPass = true;

    public static boolean checkPermissions(Activity activity, boolean isRequest) {
        synchronized (lock) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
            } else if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_ACCESS_WIFI_STATE);
            } else if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.CHANGE_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CHANGE_WIFI_STATE}, REQUEST_CHANGE_WIFI_STATE);
            } else if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_NETWORK_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_ACCESS_NETWORK_STATE);
            } else if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.CHANGE_NETWORK_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, REQUEST_CHANGE_NETWORK_STATE);
            } else if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET);
            } else if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH}, REQUEST_BLUETOOTH);
            } else if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                allPass = false;
                if (isRequest) ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_ADMIN);
            }
        }
        return allPass;

    }

    public static boolean getPass() {
        synchronized (lock) {
            return allPass;
        }
    }


}
