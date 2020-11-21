package com.example.wifi_bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.MacAddress;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static android.net.wifi.p2p.WifiP2pManager.BUSY;
import static android.net.wifi.p2p.WifiP2pManager.ERROR;
import static android.util.Log.d;

public class WifiService extends Service {
    private List<String> deviceList;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiDirectBroadcastReceiver receiver;
    private final IBinder binder = new WifiService.WifiBinder();

    final String CHANNEL_ID = "bluetoothwifi_2";
    final int NOTIFICATION_ID = 2;
    private NotificationManager notificationManager;

    private boolean importantNote = false;
    private int currentWork = 0;
    private ArrayList<WifiTask> queue = new ArrayList<>();

    public WifiService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WifiDirectBroadcastReceiver(manager, channel, this);


        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        registerReceiver(receiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID,
                WifiUtility.buildNotification(WifiUtility.NOTIFY_IDLE,this,CHANNEL_ID).build());
        idle();
        return START_STICKY;
    }
    public class WifiBinder extends Binder {
        WifiService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WifiService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        importantNote=false;
        notificationManager.notify(NOTIFICATION_ID,
                WifiUtility.buildNotification(currentWork,this,CHANNEL_ID).build());
        return binder;
    }

    public void updateDeviceList(Collection<WifiP2pDevice> deviceList) {
        List<String> l = new ArrayList<>();
        for(WifiP2pDevice device:deviceList) {
            l.add(device.deviceAddress);
        }
        this.deviceList = l;
        scanDeviceList();
    }

    public void scanDeviceList() {

    }

    public void discover() {
        if(PermissionUtility.checkWifiPermission(this)) {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int reasonCode) {
                    if(reasonCode == ERROR) {
                        d("wifiService","error when discovering");
                    } else if(reasonCode==BUSY) {
                        d("wifiService","wifi module busy");
                    }
                }
            });
        }
    }

    public void disconnect() {
        // current connection gets cut
    }

    public void connect(String macAddress) {
        WifiP2pDevice device;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = macAddress;
        config.wps.setup = WpsInfo.PBC;
        if(PermissionUtility.checkWifiPermission(this))
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //success logic

            }

            @Override
            public void onFailure(int reason) {
                //failure logic
                Toast.makeText(WifiService.this, "Wifi Direct Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void quest(String key, String path) {

    }
    public void idle() {
        currentWork = WifiUtility.NOTIFY_IDLE;
        if(!importantNote)
        notificationManager.notify(NOTIFICATION_ID,
                WifiUtility.buildNotification(WifiUtility.NOTIFY_IDLE,this,CHANNEL_ID).build());

    }


    public void download(String mac, boolean doesNotify) {

        if(doesNotify) {
            importantNote = true;
            currentWork = WifiUtility.NOTIFY_DOWNLOAD;
            NotificationCompat.Builder b = WifiUtility.buildNotification(WifiUtility.NOTIFY_DOWNLOAD,this,CHANNEL_ID);
            notificationManager.notify(NOTIFICATION_ID, b.build());
        }

    }

    public void upload(String mac, boolean doesNotify) {
        if(doesNotify) {
            importantNote = true;
            currentWork = WifiUtility.NOTIFY_UPLOAD;
            NotificationCompat.Builder b = WifiUtility.buildNotification(WifiUtility.NOTIFY_UPLOAD, this, CHANNEL_ID);
            notificationManager.notify(NOTIFICATION_ID, b.build());
        }

    }

    public void relay(String mac1, String mac2) {
        currentWork = WifiUtility.NOTIFY_RELAY;
        if(!importantNote)
        notificationManager.notify(NOTIFICATION_ID,
                WifiUtility.buildNotification(WifiUtility.NOTIFY_RELAY,this,CHANNEL_ID).build());
        // cache

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        SharedPreferences preferences = getSharedPreferences("IS_ONLINE", MODE_PRIVATE);
        preferences.edit().putBoolean("is_online",false);
    }
}