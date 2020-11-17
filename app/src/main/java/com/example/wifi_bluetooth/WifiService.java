package com.example.wifi_bluetooth;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.IBinder;

import java.util.Collection;

public class WifiService extends Service {
    private Collection<WifiP2pDevice> deviceList;


    public WifiService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void updateDeviceList(Collection<WifiP2pDevice> deviceList) {
        this.deviceList = deviceList;
    }


}