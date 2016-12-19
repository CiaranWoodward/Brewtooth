package uk.ac.soton.ecs.ciaran.brewtooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.util.JsonReader;
import android.view.View;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static uk.ac.soton.ecs.ciaran.brewtooth.R.styleable.View;

public class DeviceList extends AppCompatActivity {

    private ListView listView;
    BluetoothAdapter mBluetoothAdapter;

    Set<BluetoothDevice> brewtoothServers;

    final int REQUEST_ENABLE_BT = 1;
    private ArrayList<BrewMachine> test = new ArrayList<BrewMachine>();
    BrewMachineAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        test.add(new BrewMachine());
        test.add(new BrewMachine());
        test.add(new BrewMachine());

        test.get(0).capabilities = "Coffee, Water, Milk, Froth";
        test.get(0).name = "Nespresso";
        test.get(0).location = "Meeting Room";

        test.get(1).capabilities = "Coffee, Water";
        test.get(1).name = "Instant coffee machine";
        test.get(1).location = "Atrium";

        test.get(2).capabilities = "Coffee";
        test.get(2).name = "Filter machine";
        test.get(2).location = "Kitchen";

        adapter = new BrewMachineAdapter(this, test);

        listView = (ListView)findViewById(R.id.listview_devices);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                Intent appInfo = new Intent(DeviceList.this, BrewActivity.class);
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

                //TODO: Filter out unwanted names

                if(device.getName().matches("^[Bb]rewtooth.*")){
                    brewtoothServers.add(device);
                }
            }
        }

/*        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    //TODO: Filter out unwanted names
                    BrewMachine brewMachine = new BrewMachine();
                    brewMachine.name = device.getName();
                    brewMachine.location = "The Ether";
                    brewMachine.capabilities = "None";
                    brewMachine.mBluetoothDevice = device;
                    test.add(brewMachine);
            }
            }
        };
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        mBluetoothAdapter.startDiscovery();*/
    }

    private void queryServers(){
        test.clear();
        adapter.notifyDataSetChanged();
        
        new Thread(new Runnable() {
            public void run() {
                for (BluetoothDevice device : brewtoothServers) {
                    //connect to device
                    BluetoothSocket tmp = null;

                    try {
                        tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("52993379-2ab4-4b4f-9bce-535bc1324c85"));
                        tmp.connect();

                        InputStream tmpi = tmp.getInputStream();
                        OutputStream tmpo = tmp.getOutputStream();
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

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            test.add(brewMachine);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
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
                        }
                        catch (IOException e){}
                    }
                }
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

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else{
            startScan();
            queryServers();
        }
    }
}
