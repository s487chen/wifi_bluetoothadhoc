package com.example.wifi_bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;


public class BluetoothService extends Service {
    private BluetoothAdapter bluetoothAdapter;
    public int network = -1;
    public boolean isMaster=false;

    public int rebellionCount = 0;
    private final int MAX_TOLERANCE = 5;

    final String CHANNEL_ID = "bluetoothwifi";
    final int NOTIFICATION_ID = 1;

    public ArrayList<BluetoothDevice> discoveredBTDevices = new ArrayList<>();
    public ArrayList<FileEntry> fileList;
    private ArrayList<BluetoothDevice> slaveList = new ArrayList<>();
    private BluetoothDevice currentSlave;
    private BluetoothDevice master;


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


                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                registerReceiver(receiver, filter);
            }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if(deviceName.startsWith("WiFi_Bluetooth_Ad_hoc_")) {
                    int net = -1;
                    try {
                        net = Character.getNumericValue(device.getName().charAt(-1));
                    } catch (Exception e) {
                        return;
                    }
                    if (network != -1 && network != net) return;
                    if (network == -1) network = net;
                    discoveredBTDevices.add(device);
                    if(rebellionCount != 0) rebellionCount=0;
                }
            } else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int previous = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE,-1);
                if(previous==BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    if(slaveList.isEmpty()) {
                        Toast.makeText(context,"Unable to find any Ad hoc nearby.",Toast.LENGTH_SHORT).show();
                        stopSelf();
                    }
                }

            }
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getExtras().containsKey("network"))
        network = (int)intent.getExtras().get("network");
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter.setName("WiFi_Bluetooth_Ad_hoc_"+network);
        startForeground(NOTIFICATION_ID, buildNotification(NOTIFY_SYNC));

        if(intent.getAction().equals("slave")) {
            isMaster=false;
            discover();
        } else if(intent.getAction().equals("master")) {
            isMaster=true;
            beDiscoverable(300);
        }
        refreshFileList();
        idle();
        return START_REDELIVER_INTENT;

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
        unregisterReceiver(receiver);
        Intent wifiIntent = new Intent(getApplicationContext(), WifiService.class);
        stopService(wifiIntent);

    }

    private void discover() {
        // for slaves
        discoveredBTDevices.clear();
        while(rebellionCount<MAX_TOLERANCE) {
            if (bluetoothAdapter.startDiscovery()) {
                rebellionCount++;
            }
            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                Log.e("btservice","discover sleep interrupted");
            }
            if(rebellionCount==0) {
                return;
            }
        }

        rebel();
    }

    public void beDiscoverable(int duration) {
        // for master
        // need user permission
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        startActivity(discoverableIntent);

    }

    private void rebel() {
        isMaster = true;
        beDiscoverable(300);
    }
}