package com.example.wifi_bluetooth;

import java.io.Serializable;

public class WifiTask implements Serializable {
    public char type; // 'u' and 'd'
    public String mac;
    public int state;
    public String hash;
    // 0:unfinished
    // 1:success
    // 2:fail
    public WifiTask(char type, String mac, String hash) {
        this.type = type;
        this.mac = mac;
        this.state = 0;
        this.hash = hash;
    }
}
