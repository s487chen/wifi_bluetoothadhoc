package com.example.wifi_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BTUtility {

    final static String NAME = "WiFi Bluetooth File Share";
    final static UUID MY_UUID = UUID.fromString(NAME);
    final static String TAG = "BTUtility";
    final static int MAX_SLAVES = 4;

    public static Set<String> getPairedBT(BluetoothAdapter  bluetoothAdapter) {
        // get device that is already bonded
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Set<String> bt = null;
        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
                String deviceHardwareAddress = device.getAddress(); // MAC address
                bt.add(deviceHardwareAddress);
            }
        }
        return bt;

    }

    private static class connectionRunable implements Runnable {
        BluetoothSocket socket;
        Map<String,DeviceEntry> myDeviceMap;
        Object lock;

        connectionRunable(BluetoothSocket socket,Map<String,DeviceEntry> myDeviceMap,Object lock) {
            this.socket = socket;
            this.myDeviceMap = myDeviceMap;
            this.lock = lock;
        }

        @Override
        public void run() {
            //get info
            try {
                master(socket, myDeviceMap, lock);
            } catch (IOException e) {
                Log.e("BTUtility", "inputoutputstream fail to open");
            }

        }
    }

    public static class CustomThread extends Thread {
        public void cancel() {

        }
    }

    public static class MasterThread extends CustomThread {
        // Master server
        private final BluetoothServerSocket mmServerSocket;
        private final BluetoothAdapter bluetoothAdapter;
        Map<String,DeviceEntry> myDeviceMap;
        private final Object lock = new Object(); // DeviceSet lock
        private final Context context;
        ExecutorService pool;
        boolean shutdown;
        private boolean once = true;

        public MasterThread(BluetoothAdapter bluetoothAdapter,Map<String,DeviceEntry> deviceMap, Context context) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            this.bluetoothAdapter = bluetoothAdapter;
            this.myDeviceMap = deviceMap;
            this.context = context;
            shutdown = false;
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            // Keep listening until exception occurs or a socket is returned.
            pool = Executors.newFixedThreadPool(8);

            while (!shutdown) {
                BluetoothSocket socket = null;
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                }
                pool.execute(new connectionRunable(socket,myDeviceMap ,lock));

                if(once && bluetoothAdapter.getBondedDevices().size()>=MAX_SLAVES) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
                    context.startActivity(discoverableIntent);
                    once = false;
                }

            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                shutdown = true;
                pool.shutdown();
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    public static class SlaveThread extends CustomThread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private Map<String,DeviceEntry> myDeviceMap;
        private final BluetoothAdapter bluetoothAdapter;

        public SlaveThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device,Map<String,DeviceEntry> deviceMap) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            this.mmDevice = device;
            this.bluetoothAdapter = bluetoothAdapter;
            this.myDeviceMap = deviceMap;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            try {
                slave(mmSocket, myDeviceMap);
            } catch (IOException e) {
                Log.e("BTUtility","slave mode in/out stream issue");
            }

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    // slave first report

    private static void master(BluetoothSocket socket, Map<String,DeviceEntry> myDeviceMap, Object lock) throws IOException {
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        synchronized (lock) {
            try {
                // get
                Map<String, int[]> otherMap = (Map<String, int[]>) ois.readObject();
                Map<String, int[]> myMap = Protocol.deviceMap(myDeviceMap);
                // compare
                ArrayList<Protocol.SimpleDeviceEntry> whatUNeed =
                        Protocol.updateMsg(Protocol.compareDeviceMap(otherMap, myMap), myDeviceMap);
                Map<String, int[]> whatIWant = Protocol.compareDeviceMap(myMap, otherMap);
                // reply
                if (whatUNeed.isEmpty()) oos.writeInt(0);
                else {
                    oos.writeInt(whatUNeed.size());
                    for (Protocol.SimpleDeviceEntry i : whatUNeed) oos.writeObject(i);
                }
                if (whatIWant.isEmpty()) oos.writeInt(0);
                else {
                    oos.writeInt(whatIWant.size());
                    oos.writeObject(whatIWant);

                    // get
                    Map<String, Protocol.SimpleDeviceEntry> updateMsg = new HashMap<>();
                    int size = ois.readInt();
                    for (int i = 0; i < size; i++) {
                        Protocol.SimpleDeviceEntry temp = (Protocol.SimpleDeviceEntry) ois.readObject();
                        updateMsg.put(temp.mac, temp);
                    }
                    Protocol.applyUpdateMsg(updateMsg, myDeviceMap);

                }

            } catch (ClassNotFoundException e) {
                Log.e("BTUtility", e.getMessage());
            }

        }
        oos.close();
        socket.close();
    }

    public static void slave(BluetoothSocket socket,Map<String,DeviceEntry> myDeviceMap) throws IOException {
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        try {
            // send
            Map<String, int[]> myMap = Protocol.deviceMap(myDeviceMap);
            oos.writeObject(myMap);

            // get reply
            int size = ois.readInt();
            if (size > 0) {
                Map<String, Protocol.SimpleDeviceEntry> updateMsg = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    Protocol.SimpleDeviceEntry temp = (Protocol.SimpleDeviceEntry) ois.readObject();
                    updateMsg.put(temp.mac, temp);
                }
                Protocol.applyUpdateMsg(updateMsg, myDeviceMap);
            }

            int requestSize = ois.readInt();
            if(requestSize>0) {
                Map<String, int[]> request = (Map<String, int[]>)ois.readObject();
                ArrayList<Protocol.SimpleDeviceEntry> whatUNeed = Protocol.updateMsg(request, myDeviceMap);

                // send
                oos.writeInt(whatUNeed.size());
                for (Protocol.SimpleDeviceEntry i : whatUNeed) oos.writeObject(i);

            }

        } catch (ClassNotFoundException e) {
            Log.e("BTUtility", e.getMessage());
        }
        oos.close();
        socket.close();
    }

    public static String exchangeName(ObjectInputStream in,ObjectOutputStream out, String myMac) throws Exception {
        out.writeObject(myMac);
        return (String)in.readObject();
    }

    public static void sendCmd() {

    }

    public static void readCmd() {

    }

    public static void promoteMaster() {

    }

    public static void findMaster() {

    }


}
