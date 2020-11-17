package com.example.wifi_bluetooth;

import java.io.Serializable;

public class FileEntry implements Serializable {
    public String fname;
    public String fpath;
    public String fhash = "not found.";
    public long size = 0;

    public FileEntry(String name, String path, String hashCode, long size)  {
        this.fname = name;
        this.fpath = path;
        this.fhash = hashCode;
        this.size = size;
    }


}

// file changes
// file not exist any more
