package com.example.wifi_bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;


public class BluetoothService extends Service {
    private BluetoothAdapter bluetoothAdapter;
    final String CHANNEL_ID = "bluetoothwifi";
    final int NOTIFICATION_ID = 1;

    public ArrayList<FileEntry> fileList;
    // Binder given to clients
    private final IBinder binder = new BluetoothBinder();
    private NotificationManager notificationManager;

    // notification state
    final int NOTIFY_SYNC = -1;
    final int NOTIFY_IDLE = 0;


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
        refreshFileList();
        idle();
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
        if(state == NOTIFY_SYNC) {
            // sync
            builder.setSmallIcon(R.drawable.ic_sync)
                    .setContentText("Bluetooth Synchronizing");
        } else if(state == NOTIFY_IDLE) {
            // idle
            builder.setSmallIcon(R.drawable.ic_idle)
                    .setContentText("Bluetooth Idle");
        }


        return builder.build();

    }

    public void idle() {
        notificationManager.notify(NOTIFICATION_ID,buildNotification(NOTIFY_IDLE));
    }

    public void sync() {
        notificationManager.notify(NOTIFICATION_ID,buildNotification(NOTIFY_SYNC));
    }

    private void advertise() {
        // share all known nodes' fileList (with version number)
        // share my Mac, S/M my routing table
    }

    public void refreshFileList() {
        this.fileList = IOUtility.readPref(this);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = getSharedPreferences("IS_ONLINE", MODE_PRIVATE);
        preferences.edit().putBoolean("is_online",false);

    }
}