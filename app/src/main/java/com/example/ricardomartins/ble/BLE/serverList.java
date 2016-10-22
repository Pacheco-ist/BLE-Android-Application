package com.example.ricardomartins.ble.BLE;

import android.util.Log;

import java.util.LinkedList;

/**
 * Created by ricardomartins on 26/09/16.
 */
public class serverList {

    private LinkedList<server> list;

    public serverList(){
        list = new LinkedList<server>();
    }

    public void addServer(String ip, int port, int rssi){
        server element;
        for (server serv: list) {
            if( serv.isEqual(ip,port,rssi) ){
                return;
            }
        }
        list.add(new server(ip,port,rssi));
    }

    public server getProminentServer(){
        double bestscore=0;
        server bestserver=null;
        double serverscore;
        for (server serv: list) {
            serverscore = serv.getScore();
            Log.i("VOTE", serv.getPort() + " has " + serverscore);
            if( serverscore > bestscore){
                bestscore= serverscore;
                bestserver = serv;
            }
        }
        return bestserver;
    }

    public void reset(){
        list.clear();
    }


    public class server{
        private String ipaddr;
        private int port;
        private int[] rssi = new int[25];
        private int occorences = 0;

        server(String ipaddr, int port, int rssi){
            this.ipaddr = ipaddr;
            this.port = port;
            this.rssi[occorences++]= rssi;

        }

        public boolean isEqual(String ipaddr, int port, int rssi){
            if (this.ipaddr == ipaddr){
                if (this.port == port){
                    this.rssi[occorences++]= rssi;
                    return true;
                }
            }
            return false;
        }

        public double getScore(){
            double score = 0;

            for (int index = 0 ; index < occorences; index++) {
                Log.i("Values", rssi[index]  + "->" + ((rssi[index] + 100.0)/20) + " = " + Math.exp( (rssi[index] + 100.0)/20));
                score += Math.exp( (rssi[index] + 100.0)/20);
            }

            return score;
        }

        public String getIpaddr() {
            return ipaddr;
        }

        public int getPort() {
            return port;
        }
    }

}
