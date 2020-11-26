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
    public int version; // forwarding table version
    public int fversion; //filelist version
    public Set<String> fileList;
    public Set<String> adjMasters;
    public Set<String> slaves;

    public DeviceEntry(String mac, int version, int fversion) {
        this.mac = mac;
        this.version = version;
        this.version = fversion;
        this.fileList=new HashSet<String>();
        this.adjMasters=new HashSet<String>();
        this.slaves=new HashSet<String>();

    }

    @Override
    public int hashCode() {
        //final int prime = 31;
        //int result = 1;
        //result = prime * result + mac.hashCode();
        //result = prime * result + version;
        return mac.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeviceEntry other = (DeviceEntry) obj;
        if (!mac.equals(other.mac))
            return false;
        return true;
    }
}
