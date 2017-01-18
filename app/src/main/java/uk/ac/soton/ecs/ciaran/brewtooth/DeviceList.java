package uk.ac.soton.ecs.ciaran.brewtooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeviceList extends AppCompatActivity {

    public static BrewMachine curBrewMachine;

    private ListView listView;
    BluetoothAdapter mBluetoothAdapter;

    Handler mHandler;
    volatile boolean isActive = false;

    Set<BluetoothDevice> brewtoothServers;

    final int REQUEST_ENABLE_BT = 1;
    private ArrayList<BrewMachine> brewMachines = new ArrayList<BrewMachine>();
    BrewMachineAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        adapter = new BrewMachineAdapter(this, brewMachines);

        listView = (ListView)findViewById(R.id.listview_devices);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                Intent appInfo = new Intent(DeviceList.this, BrewActivity.class);
                curBrewMachine = brewMachines.get(position);
                startActivity(appInfo);
            }
        });

    }

    private void startScan(){

        //First Get paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        brewtoothServers = new HashSet<BluetoothDevice>();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {

                //Filter out unwanted names

                if(device.getName().matches("^[Bb]rewtooth.*")){
                    brewtoothServers.add(device);
                }
            }
        }

    }

    private void queryServers(){
        Log.i("Brewtooth", "Querying servers...");

        new Thread(new Runnable() {
            public void run() {
                final ArrayList<BrewMachine> futureBrewMachines = new ArrayList<BrewMachine>();
                if(mHandler != null && isActive) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            queryServers();
                        }
                    }, 10000);  //Rerun in 10 seconds
                }
                for (BluetoothDevice device : brewtoothServers) {
                    //connect to device
                    BluetoothSocket tmp = null;
                    InputStream tmpi = null;
                    OutputStream tmpo = null;

                    try {
                        tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("52993379-2ab4-4b4f-9bce-535bc1324c85"));
                        tmp.connect();

                        tmpi = tmp.getInputStream();
                        tmpo = tmp.getOutputStream();
                        int machineCount = 0;

                        byte[] buffer = new byte[1024];  // buffer store for the stream
                        int bytes; // bytes returned from read()

                        try {
                            JSONObject request = new JSONObject();
                            request.put("Request", "CAPABILITIES");
                            tmpo.write(request.toString().getBytes());
                        }
                        catch (JSONException e){
                            e.printStackTrace(); //All data is hardcoded, this should never happen...
                        }

                        bytes = tmpi.read(buffer);
                        String inputStr = new String(buffer, 0, bytes);
                        JSONTokener tokener = new JSONTokener(inputStr);

                        try {
                            JSONObject jsonObject;

                            if(tokener.more()){
                                jsonObject = (JSONObject) tokener.nextValue();
                            }
                            else{
                                bytes = tmpi.read(buffer);
                                inputStr = new String(buffer, 0, bytes);
                                tokener = new JSONTokener(inputStr);
                                jsonObject = (JSONObject) tokener.nextValue();
                            }

                            if(jsonObject.getString("Response").equals("MACHINES")){
                                machineCount = jsonObject.getInt("Quantity");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            continue;
                        }

                        for(int i = 0; i < machineCount; i++) {
                            try {
                                JSONObject jsonObject;

                                if(tokener.more()) {
                                    jsonObject = (JSONObject) tokener.nextValue();
                                }
                                else{
                                    bytes = tmpi.read(buffer);
                                    inputStr = new String(buffer, 0, bytes);
                                    tokener = new JSONTokener(inputStr);
                                    jsonObject = (JSONObject) tokener.nextValue();
                                }

                                if (jsonObject.getString("Response").equals("MACHINE_DETAILS")) {

                                    final BrewMachine brewMachine = new BrewMachine();
                                    brewMachine.name = jsonObject.getString("Name");
                                    brewMachine.location = jsonObject.getString("Location");
                                    brewMachine.capabilities = jsonObject.getString("Function");
                                    brewMachine.mBluetoothDevice = device;
                                    brewMachine.deviceID = jsonObject.getInt("Machine");

                                    futureBrewMachines.add(brewMachine);
                                }
                            }
                            catch(JSONException e){
                                continue;
                            }
                        }

                    }
                    catch (IOException e){
                        continue;
                    }
                    finally {
                        try {
                            if (tmp != null) tmp.close();
                            if (tmpi != null) tmpi.close();
                            if (tmpo != null) tmpo.close();
                        }
                        catch (IOException e){}
                    }
                }
                //Swap in new buffer
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        brewMachines.clear();
                        brewMachines.addAll(futureBrewMachines);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();

    }

    @Override
    protected void onActivityResult (int requestCode,
                           int resultCode,
                           Intent data){
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            // Create a BroadcastReceiver for ACTION_FOUND
            startScan();
            queryServers();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        isActive = true;

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else{
            if(brewtoothServers == null) startScan();
            queryServers();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        isActive = false;
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStart(){
        super.onStart();

        mHandler = new Handler();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }
}
