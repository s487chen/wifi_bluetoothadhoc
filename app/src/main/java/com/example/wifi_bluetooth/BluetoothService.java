package com.example.wifi_bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;


public class BluetoothService extends Service {
    private BluetoothAdapter bluetoothAdapter;
    final String CHANNEL_ID = "bluetoothwifi";
    final int NOTIFICATION_ID = 1;
    // Binder given to clients
    private final IBinder binder = new BluetoothBinder();
    private NotificationManager notificationManager;

    // notification state
    final int NOTIFY_SYNC = 0;
    final int NOTIFY_RELAY = 1;
    final int NOTIFY_DOWNLOAD = 2;
    final int NOTIFY_UPLOAD = 3;


    public BluetoothService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
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
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        startForeground(NOTIFICATION_ID, buildNotification(NOTIFY_SYNC));

        return START_STICKY;

    }

    public class BluetoothBinder extends Binder {
        BluetoothService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothService.this;
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        // bind to first fragment
        // relaying
        return binder;

        // bind to second fragment
        // downloading
    }

    private Notification buildNotification(int state) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth-WiFi File Sharing")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if(state == 0) {
            // sync
            builder.setSmallIcon(R.drawable.ic_sync)
                    .setContentText("Synchronizing...");
        } else if(state == 1) {
            // relay
            builder.setSmallIcon(R.drawable.ic_relay)
                    .setContentText("Relaying...");
        } else if(state == 2) {
            // upload
            builder.setSmallIcon(R.drawable.ic_upload)
                    .setContentText("Uploading...");
        } else if(state == 3) {
            // download
            builder.setSmallIcon(R.drawable.ic_download)
                    .setContentText("Downloading...");
        }


        return builder.build();

    }

    public void quest(String key, String path) {

    }

    public void download(String mac) {
        notificationManager.notify(1,buildNotification(NOTIFY_DOWNLOAD));
        Intent it = new Intent(getApplicationContext(),WifiService.class);
        it.putExtra("target",mac);
        it.setAction("download");
        startForegroundService(it);
    }

    public void upload(String mac) {
        notificationManager.notify(1,buildNotification(NOTIFY_UPLOAD));
        Intent it = new Intent(getApplicationContext(),WifiService.class);
        it.putExtra("target",mac);
        it.setAction("upload");
        startForegroundService(it);
    }

    public void relay(String mac1, String mac2) {
        notificationManager.notify(1,buildNotification(NOTIFY_RELAY));
        Intent it = new Intent(getApplicationContext(),WifiService.class);
        it.putExtra("source",mac1);
        it.putExtra("target",mac2);
        it.setAction("relay");
        startForegroundService(it);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}