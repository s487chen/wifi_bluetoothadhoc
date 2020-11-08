package com.example.wifi_bluetooth;

import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class IOUtility {
    static final String RELATIVE_PREF_PATH = "/pref";
    static final int TRANSFER_SIZE = 8192;
    static void saveFile() {
        // save to file dir
    }

    static FileInputStream readCache(Context context, String name) {
        // read temp files from cache dir
        String c = context.getExternalCacheDir().getAbsolutePath();
        return streamInFile(c+"/"+name);
    }
    static FileOutputStream saveCache(Context context, String hash) {
        // save temp files to cache dir
        String c = context.getExternalCacheDir().getAbsolutePath();
        return streamOutFile(c+"/"+hash);
    }

    static ArrayList<Object> readPref(Context context) {
        int len = 0;
        ArrayList<Object> r = new ArrayList<Object>();
        try {
            String c = context.getFilesDir().getAbsolutePath();
            FileInputStream fis = streamInFile(c+RELATIVE_PREF_PATH);
            ObjectInputStream ois = new ObjectInputStream(fis);
            len = ois.readInt();
            for(int i=0;i<len;i++) {
                r.add(ois.readObject());
            }

        } catch(IOException e) {
            Log.d("readPref","read objects fail");
        } catch(ClassNotFoundException e) {
            Log.d("readPref","class mismatch");
        }
        return r;
    }

    static void savePref(Context context, ArrayList<Object> objects) {
        try {
            String c = context.getFilesDir().getAbsolutePath();
            FileOutputStream fos = streamOutFile(c+RELATIVE_PREF_PATH);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeInt(objects.size());
            for (Object i : objects) {
                oos.writeObject(i);
            }
            oos.flush();
            oos.close();
        } catch(IOException e) {
            Log.d("savePref","write objects fail");
        }
    }

    static void transferData(InputStream in, OutputStream out) throws IOException {
        int dataLength = 0;
        byte[] data = new byte[TRANSFER_SIZE];
        while((dataLength=in.read(data))!=-1) {
            out.write(data,0,dataLength);
        }
    }

    static void writeExternalFile(Context context, String path, String[] caches) {
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

    static byte[] ASCIItoBits(String s) {
        // convert string to byte[]
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static boolean verifyHash(String filePath, String hashCode) {
        boolean result = false;
        if(hashCode.equals(getHash(filePath))) {
            result = true;
        }
        return false;
    }

    static String getHash(String filePath) {
        byte[] buffer= new byte[8192];
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
