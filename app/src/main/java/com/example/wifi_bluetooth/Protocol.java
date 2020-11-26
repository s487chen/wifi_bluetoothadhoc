package com.example.wifi_bluetooth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Protocol {
    // sequence:
    // slave: i have what
    // ->master: i have what, i want what from u
    // ->slave: give u what u want, i want what from u
    // ->master: give u what

    // deviceMac:version
    public static Map<String, int[]> deviceMap(Set<DeviceEntry> dl) {
        // i have what
        Map<String,int[]> m = new HashMap<>();

        for(DeviceEntry d:dl) {
            m.put(d.mac,new int[]{d.version,d.fversion});
        }
        return m;
    }

    public static Map<String, int[]> compareDeviceMap(Map<String, int[]> mine, Map<String, int[]> received) {
        // i want what from u
        Map<String,int[]> m = new HashMap<>();

        ArrayList<ArrayList<String>> r = new ArrayList<>();
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
    public static Map<String,SimpleDeviceEntry> updateMap() {
        // give u what u want
    }

    public static void readUpdate(Map<String,SimpleDeviceEntry> updt, Set<DeviceEntry> DeviceSet) {
        for(DeviceEntry d:DeviceSet) {
            SimpleDeviceEntry sde;
            if((sde=updt.get(d.mac))!=null) {
                if(sde.version!=-1) {
                    d.version=sde.version;
                    d.adjMasters = sde.adjMasters;
                    d.slaves=sde.slaves;
                }
                if(sde.fversion!=-1) {
                    d.fversion = sde.fversion;
                    d.fileList = sde.fileList;
                }
            }
        }
    }


    private class SimpleDeviceEntry implements Serializable {
        public int version = -1; // forwarding table version
        public int fversion = -1; //filelist version
        public Set<String> fileList = null;
        public Set<String> adjMasters =null;
        public Set<String> slaves =null;

    }



    private static void convertToGraph(Set<DeviceEntry> deviceSet) {

    }

    public static void routing(String me, String dest, Set<DeviceEntry> deviceSet) {

    }
}
