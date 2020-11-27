package com.example.wifi_bluetooth;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Protocol {
    // sequence:
    // slave: i have what
    // ->master: give u what u want, i want what from u
    // ->slave: give u what u want

    // deviceMac:version
    public static Map<String, int[]> deviceMap(Map<String, DeviceEntry> dm) {
        // i have what
        Map<String,int[]> m = new HashMap<>();

        for(DeviceEntry d:dm.values()) {
            m.put(d.mac,new int[]{d.version,d.fversion});
        }
        return m;
    }

    public static Map<String, int[]> compareDeviceMap(Map<String, int[]> mine, Map<String, int[]> received) {
        // i want what from u
        Map<String,int[]> m = new HashMap<>();

        for(String mac:received.keySet()) {
            if(!mine.containsKey(mac) ||
                    (mine.get(mac)[0]<received.get(mac)[0] && mine.get(mac)[1]<received.get(mac)[1])) {
                m.put(mac,new int[]{1,1});
            }
            else {
                if(mine.get(mac)[0]<received.get(mac)[0]) m.put(mac,new int[]{1,0});
                if(mine.get(mac)[1]<received.get(mac)[1]) m.put(mac,new int[]{0,1});
            }
        }
        return m;
    }

    // adjmasters, slaves, version
    public static ArrayList<SimpleDeviceEntry> updateMsg(Map<String, int[]> m, Map<String,DeviceEntry> dm) {
        // give u what u want
        ArrayList<SimpleDeviceEntry> n = new ArrayList<>();
        for(String mac:m.keySet()) {
            SimpleDeviceEntry a = new SimpleDeviceEntry(mac);
            int[] b = m.get(mac);
            DeviceEntry d = dm.get(mac);
            if(b[0]==1) {
                a.version=d.version;
                a.adjMasters=d.adjMasters;
                a.slaves=d.slaves;
            }
            if(b[1]==1) {
                a.fversion=d.fversion;
                a.fileList=d.fileList;
            }
            n.add(a);
        }
        return n;
    }

    public static void applyUpdateMsg(Map<String,SimpleDeviceEntry> updt, Map<String,DeviceEntry> dm) {
        for(String mac:updt.keySet()) {
            SimpleDeviceEntry sde = updt.get(mac);

            // new device entry
            if(!dm.containsKey(mac)) {
                DeviceEntry de = new DeviceEntry(mac,sde.version,sde.fversion);
                de.slaves = sde.slaves;
                de.adjMasters = sde.adjMasters;
                de.fileList = sde.fileList;
                dm.put(mac,de);
            } else {
                // update existing entry
                DeviceEntry d=dm.get(mac);
                if(sde.version>d.version) {
                    d.version=sde.version;
                    d.adjMasters = sde.adjMasters;
                    d.slaves=sde.slaves;
                }
                if(sde.fversion>d.fversion) {
                    d.fversion = sde.fversion;
                    d.fileList = sde.fileList;
                }
            }
        }

    }


    public static class SimpleDeviceEntry implements Serializable {
        public String mac;
        public int version = -1; // forwarding table version
        public int fversion = -1; //filelist version
        public Set<String> fileList = null;
        public Set<String> adjMasters =null;
        public Set<String> slaves =null;
        public SimpleDeviceEntry(String mac) {
            this.mac = mac;
        }

    }





    private static void convertToGraph(Set<DeviceEntry> deviceSet) {

    }

    public static void routing(String me, String dest, Set<DeviceEntry> deviceSet) {

    }
}
