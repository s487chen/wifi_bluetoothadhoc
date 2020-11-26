package com.example.wifi_bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DeviceEntry implements Serializable  {
    public String mac;
    public Set<String> fileList;
    public Set<String> adjMasters;
    public Set<String> slaves;

    public DeviceEntry(String mac) {
        this.mac = mac;
        this.fileList=new HashSet<String>();
        this.adjMasters=new HashSet<String>();
        this.slaves=new HashSet<String>();

    }
}
