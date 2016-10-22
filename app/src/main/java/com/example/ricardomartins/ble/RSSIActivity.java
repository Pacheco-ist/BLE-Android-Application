package com.example.ricardomartins.ble;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.Switch;

import com.example.ricardomartins.ble.BLE.BluetoothLeService;
import com.example.ricardomartins.ble.BLE.LeDeviceListAdapter;
import com.example.ricardomartins.ble.BLE.serverList;
import com.example.ricardomartins.ble.TCP.FileTempStorage;
import com.example.ricardomartins.ble.TCP.TCPClient;
import com.example.ricardomartins.ble.ViewComponents.ZoomableMapImage;
import com.example.ricardomartins.ble.ViewComponents.locationPopUp;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RSSIActivity extends AppCompatActivity {

    final String TAG = "RSSIACT";

    //permission variables
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    //BLE scanning variable
    private static final long SCAN_PERIOD = 1000;
    private boolean mScanning;
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();

    //Handler variables
    private Handler mHandler;
    public static final int UPDATE = 6;
    public static final int REQFILE = 5;
    public static final int ERROR = 4;
    public static final int CONNECTING = 3;
    public static final int SENDING = 2;
    public static final int FAIL_BLE = 1;

    // TCP variables
    private Socket socket;
    private int SERVERPORT;
    private StringBuilder SERVER_IP = new StringBuilder("");
    private Context ActContext;
    private String devices_found;
    private String message;

    //MAPview variable
    private ZoomableMapImage MapView;
    private Bitmap map;
    private double xcoord;
    private double ycoord;
    private volatile boolean running = true;
    private volatile boolean updating=false;
    StringBuilder MapName = new StringBuilder("");

    /*gatt handling variable */
    StringBuilder servername = new StringBuilder();
    private Gatt_discoverer mGatt_discoverer;
    private BluetoothLeService mBluetoothLeService;
    boolean mConnected;

    //custom classes
    private LeDeviceListAdapter mLeDeviceListAdapter;
    locationPopUp popup = null;
    FileTempStorage fileStorage = new FileTempStorage();

    // Service creation for BLE device communication
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "service binded");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                mHandler.sendEmptyMessage(RSSIActivity.FAIL_BLE);
            }
            Log.i(TAG,"CONNECTING");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rssi);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        ActContext = this;

        Button popupbutton = (Button) findViewById(R.id.button);
        popup  = new locationPopUp( new PopupMenu(getApplicationContext(), popupbutton));
        popupbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                popup.show();
            }
        });

        MapView = (ZoomableMapImage) findViewById(R.id.Mapview);


        final Switch aswitch = (Switch) findViewById(R.id.switch1);
        aswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    Log.i(TAG, "switch on");


                    running = true;
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                while(running) {
                                    while(updating){
                                        Log.i(TAG,"Waiting to finish download");
                                        sleep(2000);
                                    }
                                    updating=true;
                                    Log.i(TAG, "Searching");

                                    mLeDeviceListAdapter.clear();
                                    mGatt_discoverer.resetDiscoverer();
                                    scanLeDevice(true);
                                    sleep(5000);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    thread.start();

                }else{
                    Log.i(TAG, "switch off");
                    running = false;
                }
            }
        });

        mHandler = new Handler(){
            public void handleMessage(Message msg) {


                switch(msg.what) {
                    case FAIL_BLE:
                        Log.e(TAG, "In Handler's fail BLE");
                        finish();
                        break;
                    case SENDING:
                        Log.w(TAG, "In Handler's sending");
                        break;
                    case ERROR:
                        Log.i(TAG, "In Handler's error");
                        break;
                    case CONNECTING:
                        Log.i(TAG, "In Handler's connecting");
                        break;
                    case REQFILE:
                        Log.i(TAG, "In Handler's REQFILE");
                        updating=true;
                        break;
                    case UPDATE:
                        Log.i(TAG,"In Handler's Update");
                        MapView.UpdateMap(map,xcoord,ycoord);
                        updating=false;
                        break;
                }
            }
        };

        mLeDeviceListAdapter = new LeDeviceListAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (BTAdapter == null || !BTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if( !isNetworkAvailable(this)){
            Log.i(TAG,"No fucking internet");
            Switch mswitch = (Switch) findViewById(R.id.switch1);
            mswitch.setClickable(false);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mGatt_discoverer = new Gatt_discoverer();
        BTAdapter.startLeScan(mLeScanCallback);

    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"TERMINATE");
                   /* TextView rssi_msg = (TextView) findViewById(R.id.textView1);
                    rssi_msg.setText(rssi_msg.getText() +"finished...\n");*/
                    mScanning = false;
                    BTAdapter.stopLeScan(mLeScanCallback);

                    BluetoothDevice BLdevice;

                    mGatt_discoverer.getDeviceName();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            BTAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            BTAdapter.stopLeScan(mLeScanCallback);
        }
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    final int rssi_value = rssi;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device, rssi_value);
                        }
                    });
                }
            };





    public class AsyncTcp extends AsyncTask<String, String, TCPClient> {

        private TCPClient tcpClient;
        private Handler mHandler                         ;
        private static final String TAG = "AsyncTcp";

        /**
         * ShutdownAsyncTask constructor with handler passed as argument. The UI is updated via handler.
         * In doInBackground(...) method, the handler is passed to TCPClient object.
         * @param mHandler Handler object that is retrieved from MainActivity class and passed to TCPClient
         *                 class for sending messages and updating UI.
         */
        public AsyncTcp(Handler mHandler){
            this.mHandler = mHandler;
        }

        /**
         * Overriden method from AsyncTask class. There the TCPClient object is created.
         * @param params From MainActivity class empty string is passed.
         * @return TCPClient object for closing it in onPostExecute method.
         */
        @Override
        protected TCPClient doInBackground(String... params) {
            Log.d(TAG, "In do in background");

            try{
                tcpClient = new TCPClient(mHandler,
                        devices_found,
                        SERVER_IP.toString(),
                        new TCPClient.MessageCallback() {
                            @Override
                            public void callbackMessageReceiver(String message) {
                                publishProgress(message);
                            }
                        }, SERVERPORT,
                        MapName.toString(),
                        popup,
                        fileStorage);

            }catch (NullPointerException e){
                Log.d(TAG, "Caught null pointer exception");
                e.printStackTrace();
            }
            tcpClient.run();
            return null;
        }

        /**
         * Overriden method from AsyncTask class. Here we're checking if server answered properly.
         * @param values If "restart" message came, the client is stopped and computer should be restarted.
         *               Otherwise "wrong" message is sent and 'Error' message is shown in UI.
         */
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.w(TAG, "In progress update, values: " + values.toString());
            if(values[0].equals("error")){
                mHandler.sendEmptyMessageDelayed(RSSIActivity.ERROR, 2000);
            }else if (values[0].equals("map")){

                Log.i(TAG, "Updating map!");
                map = tcpClient.map;
                MapName.replace(0, tcpClient.new_map_name.length(), tcpClient.new_map_name);
                Log.i(TAG,"NEW MAP->" + MapName);
                xcoord = tcpClient.xcoordinate;
                ycoord = tcpClient.ycoordinate;
                popup = tcpClient.popUp;
                mHandler.sendEmptyMessageDelayed(RSSIActivity.UPDATE, 2000);
            }else if (values[0].equals("coords")){
                if( xcoord != tcpClient.xcoordinate || ycoord != tcpClient.ycoordinate){
                    Log.i(TAG, "Updating map (new coords)!");

                    xcoord = tcpClient.xcoordinate;
                    ycoord = tcpClient.ycoordinate;
                    popup = tcpClient.popUp;
                    mHandler.sendEmptyMessageDelayed(RSSIActivity.UPDATE, 2000);
                }else{
                    updating = false;
                }
            }else{
                message = values[0];
            }
        }

        @Override
        protected void onPostExecute(TCPClient result){
            super.onPostExecute(result);
            Log.d(TAG, "In on post execute");

        }
    }


    /*GATT FUNTIONS*/


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG,action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "DATA AQUI, wut");
                String info;
                int port;
                String name = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if(name.equals("INESC")){
                    Log.i(TAG, "INESC device -> 146.193.41.154:10000");
                    info = "146.193.41.154";
                    port = 10000;
                    //mGatt_discoverer.addNS("146.193.41.154", 10000);
                }else if(name.equals("192:168:1:72.10000")){
                    Log.i(TAG, "IST device -> 146.193.41.154:10001");
                    info = "146.193.41.154";
                    port = 10001;
                    //mGatt_discoverer.addNS("146.193.41.154", 10001);
                }else{
                    info= "WUT";
                    port = 1234;
                    Log.i(TAG, "device ??? ->" + name);
                }
                //String[] address = name.split(".");
                //mGatt_discoverer.addNS(address[0], Integer.parseInt(address[1]));
                //Log.i(TAG, name);
                mBluetoothLeService.disconnect();

                mGatt_discoverer.addDeviceCache(info,port);
                mGatt_discoverer.addNS(info,port);

                Log.i(TAG, "calling next device");
                mGatt_discoverer.getDeviceName();

            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        Log.i(TAG, "Procurar");

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            if( mBluetoothLeService.UUID_DEVICE_SERVICE.equals(gattService.getUuid())) {
                Log.i(TAG, "ENCONTREI SERVIÇO");

                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                    if( mBluetoothLeService.UUID_SERVER_CARAC.equals(gattCharacteristic.getUuid())){
                        Log.i(TAG, "ENCONTREI SERVER, PEDIR PARA LER");
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                }
            }
        }
    }

    private class Gatt_discoverer{

        private BluetoothDevice device;
        private int current_device;

        private serverList mserverList = new serverList();

        private int max_rssi;

        private ArrayList<Device> cacheDevices = new ArrayList<Device>();

        private static final int cacheSize =5;

        Gatt_discoverer(){
            resetDiscoverer();
        }

        public void resetDiscoverer(){
            current_device=-1;
            max_rssi=0;
            mserverList.reset();
        }
        public void getDeviceName() {
            current_device++;

            Log.i(TAG, "Device "+ current_device + " of " + mLeDeviceListAdapter.getCount());

            if ( current_device >= mLeDeviceListAdapter.getCount()){
                if (current_device==0){
                    Log.i("ERROR", "No Devices found...");
                    updating = false;
                    return;
                }else {
                    mGatt_discoverer.launchTcp();
                    return;
                }
            }
            device = mLeDeviceListAdapter.getDevice(current_device);


            int index;
            if( (index = checkDeviceOnCache(device)) != -1){
                Log.i(TAG,"Found on cache pos "+index);
                addNS(cacheDevices.get(index).ip, cacheDevices.get(index).port);
                getDeviceName();
            }else{
                mBluetoothLeService.connect(device.getAddress());
            }

        }


        public void addDeviceCache( String ip, int port){
            Device argdevice = new Device(device,ip,port);
            Log.i(TAG, "adding "+ device.getAddress() + " ->>>> " + cacheDevices.contains(argdevice));
            if ( cacheDevices.contains(argdevice)){
                int index = cacheDevices.indexOf(argdevice);
                cacheDevices.remove(argdevice);
                cacheDevices.add(0,argdevice);
                Log.i(TAG, "Device already in cache: old-> "+ index +" , new -> "+ cacheDevices.indexOf(argdevice));
            }else{
                if( cacheDevices.size() >= cacheSize){
                    cacheDevices.remove(cacheSize-1);
                    Log.i(TAG, "Too big, removed "+ 2);
                }
                cacheDevices.add(0,argdevice);
                Log.i(TAG, "add device, size = "+cacheDevices.size());
            }
        }

        public int checkDeviceOnCache(BluetoothDevice device){
            Device argdevice = new Device(device,"",0);
            if(cacheDevices.contains(argdevice)){
                return cacheDevices.indexOf(argdevice);
            }
            return -1;
        }

        public void addNS(String ip, int port){
            mserverList.addServer(ip,port, mLeDeviceListAdapter.getRSSI(current_device));
        }

        public void launchTcp(){

            Log.i(TAG, "Launching TCP");

            serverList.server server = mserverList.getProminentServer();

            SERVER_IP.replace(0, server.getIpaddr().length(), server.getIpaddr());
            SERVERPORT = server.getPort();

            Log.i(TAG, "Connectiong tcp to ->" + SERVER_IP + ":" + SERVERPORT);

            devices_found ="-l ";
            for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                device = mLeDeviceListAdapter.getDevice(i);
                devices_found += device.getAddress() + "\\" + mLeDeviceListAdapter.getRSSI(i);
                if( (i+1)!=mLeDeviceListAdapter.getCount()){
                    devices_found+="!";
                }
            }

            new AsyncTcp(mHandler).execute();
        }

    }


    class Device{
        BluetoothDevice device;
        String ip;
        int port;


        Device( BluetoothDevice device,String ip, int port){
            this.device=device;
            this.ip=ip;
            this.port=port;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return device.getAddress();
        }

        @Override
        public boolean equals(Object obj) {
            if( obj == this) return true;
            if(obj==null) return false;

            if(this.getClass() != obj.getClass()) return false;

            Device other = (Device) obj;

            //Log.i(TAG, "compare My "+ this.device.getAddress() + " with " + other.device.getAddress());

            if(device.getAddress().equals(other.device.getAddress())){
                return true;
            }
            return false;
        }
    }




    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
