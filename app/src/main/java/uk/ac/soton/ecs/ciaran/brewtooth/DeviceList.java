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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static uk.ac.soton.ecs.ciaran.brewtooth.R.styleable.View;

public class DeviceList extends AppCompatActivity {

    private ListView listView;
    BluetoothAdapter mBluetoothAdapter;

    Set<BluetoothDevice> brewtoothServers;

    final int REQUEST_ENABLE_BT = 1;
    private ArrayList<BrewMachine> test = new ArrayList<BrewMachine>();

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

        BrewMachineAdapter adapter = new BrewMachineAdapter(this, test);

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
        new Thread(new Runnable() {
            public void run() {
                for (BluetoothDevice device : brewtoothServers) {
                    //connect to device
                    BluetoothSocket tmp = null;

                    try {
                        tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("NULL"));
                        tmp.connect();

                        InputStream tmpi = tmp.getInputStream();
                        OutputStream tmpo = tmp.getOutputStream();
                        int machineCount = 0;

                        BufferedReader streamReader = new BufferedReader(new InputStreamReader(tmpi, "UTF-8"));

                        try {
                            StringBuilder responseStrBuilder = new StringBuilder();

                            String inputStr;
                            while ((inputStr = streamReader.readLine()) != null)
                                responseStrBuilder.append(inputStr);

                            JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());

                            if(jsonObject.getString("Notify").equals("MACHINES")){
                                machineCount = jsonObject.getInt("Level");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            continue;
                        }

                        for(int i = 0; i < machineCount; i++) {
                            try {
                                StringBuilder responseStrBuilder = new StringBuilder();

                                String inputStr;
                                while ((inputStr = streamReader.readLine()) != null)
                                    responseStrBuilder.append(inputStr);

                                JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
                                if (jsonObject.getString("Notify").equals("MACHINE_DETAILS")) {

                                    final BrewMachine brewMachine = new BrewMachine();
                                    brewMachine.name = "Unnamed";
                                    brewMachine.location = "Somewhere nearby";
                                    brewMachine.capabilities = jsonObject.getString("Comments");
                                    brewMachine.mBluetoothDevice = device;
                                    brewMachine.deviceID = jsonObject.getInt("Machine");

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            test.add(brewMachine);
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
        }
    }
}
