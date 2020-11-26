package com.example.wifi_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BTUtility {

    final String NAME = "WiFi Bluetooth File Share";
    final UUID MY_UUID = UUID.fromString(NAME);
    final String TAG = "BTUtility";

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

    private class connectionRunable implements Runnable {
        BluetoothSocket socket;
        connectionRunable(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            //get info
            BTget();

            //send info
            BTsend();

        }
    }

    private class MasterThread extends Thread {
        // Master server
        private final BluetoothServerSocket mmServerSocket;
        private final BluetoothAdapter bluetoothAdapter;
        ExecutorService pool;
        boolean shutdown;

        public MasterThread(BluetoothAdapter bluetoothAdapter) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            this.bluetoothAdapter = bluetoothAdapter;
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


                pool.execute(new connectionRunable(socket));

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

    private class SlaveThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final BluetoothAdapter bluetoothAdapter;

        public SlaveThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            this.bluetoothAdapter = bluetoothAdapter;

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
            BTsend();
            BTget();
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

    public static void BTsend() {
        // send updated filelists for each mac
        // send updated devicelist
    }

    public static void BTget() {

    }
}
