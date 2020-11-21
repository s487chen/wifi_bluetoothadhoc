package com.example.wifi_bluetooth;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class IOUtility {
    static final String RELATIVE_PREF_PATH = "/pref";
    static final int TRANSFER_SIZE = 2048;
    static final int READFILE_SIZE = 8192;
    static final Object lock = new Object();


    static FileInputStream readCache(Context context, String name) {
        // read temp files from cache dir
        String c = context.getExternalCacheDir().getAbsolutePath();
        return streamInFile(c+"/"+name);
    }
    static FileOutputStream saveCache(Context context, String name) {
        // save temp files to cache dir
        String c = context.getExternalCacheDir().getAbsolutePath();
        return streamOutFile(c+"/"+name);
    }

    static ArrayList<FileEntry> readPref(Context context) {
        int len = 0;
        ArrayList<FileEntry> r = new ArrayList<FileEntry>();
        synchronized(lock) {
            try {
                String c = context.getFilesDir().getAbsolutePath();
                FileInputStream fis = streamInFile(c + RELATIVE_PREF_PATH);
                ObjectInputStream ois = new ObjectInputStream(fis);
                len = ois.readInt();
                for (int i = 0; i < len; i++) {
                    r.add((FileEntry) ois.readObject());
                }
                fis.close();

            } catch (IOException e) {
                Log.d("readPref", "read objects fail");
            } catch (ClassNotFoundException e) {
                Log.d("readPref", "class mismatch");
            }
        }
        return r;
    }

    static void savePref(Context context, ArrayList<FileEntry> objects) {
        synchronized (lock) {
            try {
                String c = context.getFilesDir().getAbsolutePath();
                FileOutputStream fos = streamOutFile(c + RELATIVE_PREF_PATH);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeInt(objects.size());
                for (Object i : objects) {
                    oos.writeObject(i);
                }
                oos.flush();
                oos.close();
            } catch (IOException e) {
                Log.d("savePref", "write objects fail");
            }
        }
    }

    static void transferData(InputStream in, OutputStream out) throws IOException {
        int dataLength = 0;
        byte[] data = new byte[TRANSFER_SIZE];
        while((dataLength=in.read(data))!=-1) {
            out.write(data,0,dataLength);
        }
    }

    static void transferData(InputStream in, OutputStream out, NotificationManager manager, int id,
                             NotificationCompat.Builder builder, int size) throws IOException {
        int dataLength = 0;
        int transferred = 0;
        int percent = size/20;
        byte[] data = new byte[TRANSFER_SIZE];
        builder.setProgress(100,0,false);
        manager.notify(id,builder.build());
        while((dataLength=in.read(data))!=-1) {
            transferred += dataLength;
            out.write(data,0,dataLength);

            if(transferred>percent) {

                builder.setProgress(100,100*transferred/size,false);
                manager.notify(id,builder.build());
                percent++;
            }
            builder.setProgress(0,0,false);
            builder.setContentText("Verifying");
            manager.notify(id,builder.build());

        }
    }


    static void writeExternalFile(Context context, String path, String... caches) {
        // combine multiple caches to one file
        BufferedOutputStream out = new BufferedOutputStream(streamOutFile(path));
        try {
            for( String seg :caches) {
                FileInputStream fis = readCache(context, seg);
                transferData(fis,out);
            }
            out.flush();
            out.close();
        } catch (IOException e){
            Log.d("writeFile","file write interrupted.");
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("writeFile","file output fail to close.");
        }
    }

    static boolean isFileExist(String path) {
        File f = new File(path.trim());
        if(f.isFile()) {
            if(f.length()>0) return true;
        }
        return false;
    }

    static boolean isPrefExist(Context context) {
        String c = context.getFilesDir().getAbsolutePath();
        return isFileExist(c+RELATIVE_PREF_PATH);
    }

    static FileInputStream streamInFile(String path) {
        // stream data from file
        FileInputStream in=null;
        try {
            in = new FileInputStream(path);
        } catch(FileNotFoundException e) {
            Log.d("streamInFile",path+" file not found.");
        }
        return in;
    }

    static FileOutputStream streamOutFile(String path) {
        // stream data to file
        FileOutputStream out=null;
        try {
            out = new FileOutputStream(path, false);
        } catch(FileNotFoundException e) {
            Log.d("streamOutFile",path+" file cannot be opened/created.");
        }
        return out;
    }


    public static byte[] intToBits(int a) {
        return new byte[] {
                (byte)(a >> 24),
                (byte)(a >> 16),
                (byte)(a >> 8),
                (byte)a};
    }

    public static int bitsToInt(byte[] a) {
        return ((a[0] & 0xFF) << 24) |
                ((a[1] & 0xFF) << 16) |
                ((a[2] & 0xFF) << 8 ) |
                (a[3] & 0xFF);
    }

    static byte[] ASCIIToBits(String s) {
        // convert string to byte[]
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static String BitsToASCII(byte[] b) {
        return new String(b, StandardCharsets.US_ASCII);
    }

    static byte[] pad(String s, int maxLen) {
        // generate padded key
        if(s.length() >= maxLen) {
            return ASCIIToBits(s);
        } else {
            return ASCIIToBits(s.concat(new String(new char[maxLen-s.length()])));
        }
    }

    public static byte[] merge(byte[]... args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream( );
        try {
            for(byte[] seg :args) {
                bos.write(seg);
            }
        } catch (IOException e){
            Log.e("IOmerge","packet header merge error.");
        }
        return bos.toByteArray();
    }


    static boolean verifyHash(String filePath, String hashCode) {
        boolean result = false;
        if(hashCode.equals(getHash(filePath))) {
            result = true;
        }
        return result;
    }


    static String getHash(String filePath, ProgressDialog progress, long size) {

        final double percent = 1/(double)(1+size/READFILE_SIZE);
        int progressCount = 0;
        int div = 1;
        int length = -1;

        byte[] buffer= new byte[READFILE_SIZE];
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(filePath));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DigestInputStream dis = new DigestInputStream(bis, digest);

        while(true) {
            try {
                if (dis.read(buffer) == -1) break;
                else progressCount++;
                if(progressCount*percent>=10*div) {
                    progress.setProgress(div);
                    div++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            ;
        }
        progress.setProgress(progress.getMax());
        if(progress.isShowing()) progress.dismiss();

        try {
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] raw = digest.digest();
        BigInteger bigInt = new BigInteger(1, raw);
        StringBuilder hash = new StringBuilder(bigInt.toString(16));
        return hash.toString();

    }

    static String getHash(String filePath) {
        byte[] buffer= new byte[READFILE_SIZE];
        int length = -1;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(filePath));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DigestInputStream dis = new DigestInputStream(bis, digest);

        while(true) {
            try {
                if (!(dis.read(buffer) != -1)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            ;
        }
        try {
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] raw = digest.digest();
        BigInteger bigInt = new BigInteger(1, raw);
        StringBuilder hash = new StringBuilder(bigInt.toString(16));
        while(hash.length() < 32 ){
            hash.insert(0, '0');
        }
        return hash.toString();

    }
}
