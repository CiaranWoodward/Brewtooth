package uk.ac.soton.ecs.ciaran.brewtooth;

import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BrewActivity extends AppCompatActivity {

    ProgressBar waterBar;
    ProgressBar coffeeBar;
    ProgressBar milkBar;

    LinearLayout waterLayout;
    LinearLayout coffeeLayout;
    LinearLayout milkLayout;

    Button brewButton;

    SeekBar coffeeSlide;
    SeekBar waterSlide;
    SeekBar milkSlide;
    SeekBar frothSlide;

    LinearLayout coffeeSlideLayout;
    LinearLayout waterSlideLayout;
    LinearLayout milkSlideLayout;
    LinearLayout frothSlideLayout;

    BrewMachine mBrewMachine;
    BluetoothSocket mSocket;
    InputStream inStr;
    OutputStream outStr;

    boolean initialised = false;

    readerThread mReader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brew);

        mBrewMachine = DeviceList.curBrewMachine;
        try {
            mSocket = mBrewMachine.mBluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("52993379-2ab4-4b4f-9bce-535bc1324c85"));
            mSocket.connect();

            inStr = mSocket.getInputStream();
            outStr = mSocket.getOutputStream();

            mReader = new readerThread(inStr);
            new Thread(mReader);
        }
        catch (IOException e){
            //TODO: Recover gracefully
            e.printStackTrace();
        }

        waterLayout = (LinearLayout) this.findViewById(R.id.layout_water);
        coffeeLayout = (LinearLayout) this.findViewById(R.id.layout_coffee);
        milkLayout = (LinearLayout) this.findViewById(R.id.layout_milk);
        coffeeSlideLayout = (LinearLayout) this.findViewById(R.id.layout_coffee_choice);
        waterSlideLayout = (LinearLayout) this.findViewById(R.id.layout_water_choice);
        milkSlideLayout = (LinearLayout) this.findViewById(R.id.layout_milk_choice);
        frothSlideLayout = (LinearLayout) this.findViewById(R.id.layout_froth_choice);

        waterBar = (ProgressBar) waterLayout.findViewById(R.id.progressBar_water);
        coffeeBar = (ProgressBar) coffeeLayout.findViewById(R.id.progressBar_coffee);
        milkBar = (ProgressBar) milkLayout.findViewById(R.id.progressBar_milk);

        coffeeSlide = (SeekBar) coffeeSlideLayout.findViewById(R.id.seekBar_coffee);
        waterSlide = (SeekBar) waterSlideLayout.findViewById(R.id.seekBar_water);
        milkSlide = (SeekBar) coffeeSlideLayout.findViewById(R.id.seekBar_milk);
        frothSlide = (SeekBar) waterSlideLayout.findViewById(R.id.seekBar_froth);

        brewButton = (Button) this.findViewById(R.id.button_brew);

        brewButton.setOnClickListener(mBrewButtonListener);
        initialised = true;

        sendFeatureListRequest();
        sendLevelRequest();

    }

    private void sendFeatureListRequest(){
        try {
            JSONObject request = new JSONObject();
            request.put("Request", "FEATURE_LIST");
            request.put("Machine", mBrewMachine.deviceID);
            outStr.write(request.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            //Should never happen, all hardcoded.
            return;
        }
    }

    private void sendLevelRequest(){
        try {
            JSONObject request = new JSONObject();
            request.put("Request", "LEVELS");
            request.put("Machine", mBrewMachine.deviceID);
            outStr.write(request.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            //Should never happen, all hardcoded.
            return;
        }
    }

    private View.OnClickListener mBrewButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if(initialised) {
                try {
                    JSONObject request = new JSONObject();
                    request.put("Request", "MAKE_COFFEE");
                    request.put("Machine", mBrewMachine.deviceID);
                    outStr.write(request.toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    //Should never happen, all hardcoded.
                    return;
                }
            }
        }
    };

    void processCoffeeDone(JSONObject jsonObj) throws JSONException{
        brewButton.setText(R.string.button_brew_text);

        if(jsonObj.has("Milk")){
            milkBar.setProgress(jsonObj.getInt("Milk"));
        }

        if(jsonObj.has("Water")){
            milkBar.setProgress(jsonObj.getInt("Water"));
        }

        if(jsonObj.has("Coffee")){
            milkBar.setProgress(jsonObj.getInt("Coffee"));
        }
    }

    void processStatus(JSONObject jsonObj) throws JSONException{
        String status = jsonObj.getString("StatusCode");

        if(status.equals("READY")){
            brewButton.setEnabled(true);
            brewButton.setText(R.string.button_brew_text);
        }
        else if(status.equals("NO_CUP")){
            brewButton.setEnabled(false);
            brewButton.setText(R.string.button_brew_nocup_text);
        }
        else if(status.equals("CLEAN_ME")){
            brewButton.setEnabled(false);
            brewButton.setText(R.string.button_brew_cleanme_text);
        }
        else if(status.equals("MAINTAIN")){
            brewButton.setEnabled(false);
            brewButton.setText(R.string.button_brew_maintain_text);
        }
    }

    private void processShowHide(JSONObject jsonObj, String name, View view) throws  JSONException{
        if(jsonObj.has(name) && jsonObj.getString(name).equals("YES")){
            view.setVisibility(View.VISIBLE);
        }
        else{
            view.setVisibility(View.GONE);
        }
    }

    void processFeatureList(JSONObject jsonObj) throws JSONException{

        processShowHide(jsonObj, "MilkLevel", milkBar);
        processShowHide(jsonObj, "CoffeeLevel", coffeeBar);
        processShowHide(jsonObj, "WaterLevel", waterBar);
        processShowHide(jsonObj, "StrengthParam", coffeeSlide);
        processShowHide(jsonObj, "WaterParam", waterSlide);
        processShowHide(jsonObj, "MilkParam", milkSlide);
        processShowHide(jsonObj, "FrothParam", frothSlide);

    }

    void processLevels(JSONObject jsonObj) throws JSONException{
        if(jsonObj.has("Milk")){
            milkBar.setProgress(jsonObj.getInt("Milk"));
        }

        if(jsonObj.has("Water")){
            milkBar.setProgress(jsonObj.getInt("Water"));
        }

        if(jsonObj.has("Coffee")){
            milkBar.setProgress(jsonObj.getInt("Coffee"));
        }
    }

    private class readerThread implements Runnable{

        private InputStream in;

        public readerThread(InputStream in){
            this.in = in;
        }

        @Override
        public void run(){

            while(true) {
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bytes; // bytes returned from read()
                JSONObject jsonObject;

                try {
                    bytes = in.read(buffer);
                    String inputStr = new String(buffer, 0, bytes);
                    JSONTokener tokener = new JSONTokener(inputStr);

                    try {
                        if (tokener.more()) {
                            jsonObject = (JSONObject) tokener.nextValue();
                        } else {
                            bytes = in.read(buffer);
                            inputStr = new String(buffer, 0, bytes);
                            tokener = new JSONTokener(inputStr);
                            jsonObject = (JSONObject) tokener.nextValue();
                        }
                    } catch (JSONException e) {
                        continue; //Ignore invalid input
                        //TODO: Recover the invalid string into the next jsontokener input (This might not be necessary)
                    }

                    final JSONObject passObj = jsonObject;
                    try {
                        if(jsonObject.has("Response")) {
                            if (jsonObject.getString("Response").equals("COFFEE_START")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        brewButton.setText(R.string.button_brew_brewing_text);
                                    }
                                });
                            }
                            else if (jsonObject.getString("Response").equals("FEATURE_LIST")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            processFeatureList(passObj);
                                        }
                                        catch (JSONException e){
                                            return; //non critical -> ignore and continue
                                        }
                                    }
                                });
                            }
                            else if (jsonObject.getString("Response").equals("LEVELS")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            processLevels(passObj);
                                        }
                                        catch (JSONException e){
                                            return; //non critical -> ignore and continue
                                        }
                                    }
                                });
                            }
                        }
                        else if(jsonObject.has("Notify")){
                            if (jsonObject.getString("Notify").equals("COFFEE_DONE")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            processCoffeeDone(passObj);
                                        }
                                        catch (JSONException e){
                                            return; //non critical -> ignore and continue
                                        }
                                    }
                                });
                            }
                            else if (jsonObject.getString("Notify").equals("STATUS")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            processStatus(passObj);
                                        }
                                        catch (JSONException e){
                                            return; //non critical -> ignore and continue
                                        }
                                    }
                                });
                            }
                        }
                    }
                    catch(JSONException e){
                        continue; //Ignore invalid JSON
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
