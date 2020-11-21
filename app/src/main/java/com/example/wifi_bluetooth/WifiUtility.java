package com.example.wifi_bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.core.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class WifiUtility {
    final static int TIMEOUT = 200;
    final static int PORT = 8020;
    final static int CTRL_SIZE = 4+32+4;

    final static int NOTIFY_IDLE = 0;
    final static int NOTIFY_RELAY = 1;
    final static int NOTIFY_DOWNLOAD = 2;
    final static int NOTIFY_UPLOAD = 3;

    // set server
    // get file
    public static class FileServerAsyncTask extends AsyncTask<String,Integer,String> {
        private Context context;
        private int mID;
        private NotificationManager manager;
        private NotificationCompat.Builder builder;
        private boolean isCache;


        public FileServerAsyncTask(Context context, NotificationManager manager, int mID, NotificationCompat.Builder builder, boolean isCache) {
            this.context = context;
            this.isCache = isCache;
            this.mID = mID;
            this.manager = manager;
            this.builder = builder;
        }

        @Override
        protected String doInBackground(String... params) {
            String path = params[0];

            try {

                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                ServerSocket serverSocket = new ServerSocket(PORT);
                Socket client = serverSocket.accept();

                /**
                 * If this code is reached, a client has connected and transferred data
                 * read control msg
                 */
                InputStream inputstream = client.getInputStream();

                byte[] ctrlBuffer = new byte[CTRL_SIZE];
                inputstream.read(ctrlBuffer);
                int fileSize = getCtrlSize(ctrlBuffer);

                String fileHash = getCtrlHash(ctrlBuffer);
                byte[] nameBuffer = new byte[getCtrlNameSize(ctrlBuffer)];
                inputstream.read(nameBuffer);
                String fileName = IOUtility.BitsToASCII(nameBuffer);

                IOUtility.transferData(inputstream,IOUtility.saveCache(context,fileName),manager,mID, builder, fileSize);

                serverSocket.close();

                // verify
                String c=context.getExternalCacheDir().getAbsolutePath()+"/"+fileName;
                if(IOUtility.verifyHash(c,fileHash)) {
                    if(!isCache) {
                        IOUtility.writeExternalFile(context,path+"/"+fileName,c);
                        return path+"/"+fileName;
                    }
                } else {
                    return null;
                }

                // write to external
                return c;
            } catch (IOException e) {
                Log.e("wifiutil_server", e.getMessage());
                return null;
            }

        }

        /**
         * Start activity that can handle the JPEG image
         */
        @Override
        protected void onPostExecute(String result) {
            if (result == null) {

                if(!isCache) manager.notify(mID,builder.setContentText("Download fail").setSmallIcon(R.drawable.ic_fail).build());
            } else {
                if(!isCache) manager.notify(mID,builder.setContentText("Download complete").setSmallIcon(R.drawable.ic_idle).build());
            }
        }
    }

    // set client
    // send file


    private static byte[] MakeCtrlMsg(int size, String hashCode, String name) {
        return IOUtility.merge(IOUtility.intToBits(size),IOUtility.pad(hashCode, 32),
                IOUtility.intToBits(name.length()), IOUtility.ASCIIToBits(name));
    }

    private static int getCtrlSize(byte[] msg) {
        return IOUtility.bitsToInt(Arrays.copyOfRange(msg,0,4));
    }

    private static String getCtrlHash(byte[] msg) {
        return IOUtility.BitsToASCII(Arrays.copyOfRange(msg,4,36));
    }

    private static int getCtrlNameSize(byte[] msg) {
        return IOUtility.bitsToInt(Arrays.copyOfRange(msg,36,40));
    }

    public static NotificationCompat.Builder buildNotification(int state, Context context, String channel) {
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context, channel)
                .setContentTitle("Bluetooth-WiFi File Sharing");

        if(state == NOTIFY_IDLE) {
            // idle
            nBuilder.setContentText("Idle").setSmallIcon(R.drawable.ic_idle)
                    .setPriority(NotificationCompat.PRIORITY_MIN);
        } else if(state == NOTIFY_RELAY) {
            // relay
            nBuilder.setSmallIcon(R.drawable.ic_relay)
                    .setContentText("WiFi Relaying").setPriority(NotificationCompat.PRIORITY_MIN);
        } else if(state == NOTIFY_UPLOAD) {
            // upload
            nBuilder.setSmallIcon(R.drawable.ic_upload)
                    .setContentText("WiFi Uploading").setPriority(NotificationCompat.PRIORITY_DEFAULT);
        } else if(state == NOTIFY_DOWNLOAD) {
            // download
            nBuilder.setSmallIcon(R.drawable.ic_download)
                    .setContentText("WiFi Downloading").setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        return nBuilder;

    }

}
