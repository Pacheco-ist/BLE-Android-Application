package com.example.ricardomartins.ble.TCP;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by ricardomartins on 03/10/16.
 */
class File{
    private String filename;
    private byte[] filedata;


    File(String name, byte[] data){
        filename = name;
        filedata = data;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return filename;
    }

    @Override
    public boolean equals(Object obj) {
        if( obj == this) return true;
        if(obj==null) return false;

        if(this.getClass() != obj.getClass()) return false;

        File other = (File) obj;

        //Log.i(TAG, "compare My "+ this.device.getAddress() + " with " + other.device.getAddress());

        if(other.filename.equals(filename)){
            return true;
        }
        return false;
    }

    public byte[] getData(){
        return filedata;
    }
    public String getName(){
        return filename;
    }
}


public class FileTempStorage {

    private static final String TAG = "FileTempStorage";

    private ArrayList<File> FileStorage = new ArrayList<File>();
    private static final int cacheSize =5;


    public void addDeviceCache(String name, byte[] data){
        File newfile = new File(name, data);
        Log.i(TAG, "adding "+ name + " ->>>> " + FileStorage.contains(newfile));
        if ( FileStorage.contains(newfile)){
            int index = FileStorage.indexOf(newfile);
            FileStorage.remove(newfile);
            FileStorage.add(0,newfile);
            Log.i(TAG, "Device already in cache: old-> "+ index +" , new -> "+ FileStorage.indexOf(newfile));
        }else{
            if( FileStorage.size() >= cacheSize){
                FileStorage.remove(cacheSize-1);
                Log.i(TAG, "Too big, removed "+ 2);
            }
            FileStorage.add(0,newfile);
            Log.i(TAG, "add device, size = "+FileStorage.size());
        }
    }

    public int checkDeviceOnCache(String filename){
        File argfile = new File(filename,null);
        if(FileStorage.contains(argfile)){
            return FileStorage.indexOf(argfile);
        }
        return -1;
    }

    public byte[] getFIle(int index){
        return FileStorage.get(index).getData();
    }

    public String getFileName(int index){
        return FileStorage.get(index).getName();
    }
}
