package uk.ac.soton.ecs.ciaran.brewtooth;

import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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

    Thread mWorker;
    readerThread mReader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brew);

        if(!initialised) {
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
            milkSlide = (SeekBar) milkSlideLayout.findViewById(R.id.seekBar_milk);
            frothSlide = (SeekBar) frothSlideLayout.findViewById(R.id.seekBar_froth);

            brewButton = (Button) this.findViewById(R.id.button_brew);

            brewButton.setOnClickListener(mBrewButtonListener);
            initialised = true;
        }

    }

    @Override
    protected void onStart(){
        super.onStart();
        mBrewMachine = DeviceList.curBrewMachine;

        mReader = new readerThread(inStr);
        mWorker = new Thread(mReader);
        mWorker.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.d("Brewtooth", "Stopping...");
        try {
            if(inStr != null) inStr.close();
            if(outStr != null) outStr.close();
            mSocket.close();
        }catch (IOException e){
            Log.e("Brewtooth", "Problem closing socket");
        }
        mWorker.interrupt();
        try {
            while (mWorker.isAlive() || mSocket.isConnected()) Thread.sleep(10);
        }catch(InterruptedException e){
            Log.e("Brewtooth", "Unexpected interruption!");
        }
    }

    private void sendFeatureListRequest(){
        try {
            JSONObject request = new JSONObject();
            request.put("Request", "FEATURE_LIST");
            request.put("Machine", mBrewMachine.deviceID);
            outStr.write(request.toString().getBytes());
        } catch (IOException e) {
            //Output stream has probably been closed, not the end of the world, probably intentional, just move on
            Log.w("Brewtooth", "outStr IOException raised");
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
            //Output stream has probably been closed, not the end of the world, probably intentional, just move on
            Log.w("Brewtooth", "outStr IOException raised");
        } catch (JSONException e) {
            //Should never happen, all hardcoded.
            return;
        }
    }

    private View.OnClickListener mBrewButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if(initialised) {

                if((mWorker == null) || (!mWorker.isAlive())){
                    mReader = new readerThread(inStr);
                    mWorker = new Thread(mReader);
                    mWorker.start();
                }

                if(outStr == null)  return;

                try {
                    JSONObject request = new JSONObject();
                    request.put("Request", "MAKE_COFFEE");
                    request.put("Machine", mBrewMachine.deviceID);
                    if(coffeeSlideLayout.getVisibility() == View.VISIBLE) request.put("Strength", coffeeSlide.getProgress());
                    if(waterSlideLayout.getVisibility() == View.VISIBLE) request.put("Water", waterSlide.getProgress());
                    if(milkSlideLayout.getVisibility() == View.VISIBLE) request.put("Milk", milkSlide.getProgress());
                    if(frothSlideLayout.getVisibility() == View.VISIBLE) request.put("Froth", frothSlide.getProgress());
                    outStr.write(request.toString().getBytes());
                } catch (IOException e) {
                    //Output stream has probably been closed, not the end of the world, probably intentional, just move on
                    Log.w("Brewtooth", "outStr IOException raised");
                } catch (JSONException e) {
                    //Should never happen, all hardcoded.
                    return;
                }
            }
        }
    };

    void processCoffeeDone(JSONObject jsonObj) throws JSONException{
        brewButton.setEnabled(true);
        brewButton.setText(R.string.button_brew_text);

        processLevels(jsonObj);
    }

    void processStatus(JSONObject jsonObj) throws JSONException{
        String status = jsonObj.getString("StatusCode");

        if(status.equals("READY")){
            brewButton.setEnabled(true);
            brewButton.setText(R.string.button_brew_text);
        }
        else if(status.equals("NO_CUP")){
            brewButton.setText(R.string.button_brew_nocup_text);
        }
        else if(status.equals("CLEAN_ME")){
            brewButton.setText(R.string.button_brew_cleanme_text);
        }
        else if(status.equals("MAINTAIN")){
            brewButton.setText(R.string.button_brew_maintain_text);
        }
        else if(status.equals("MORE_COFFEE")){
            brewButton.setText(R.string.button_brew_refillcoffee_text);
        }
        else if(status.equals("MORE_MILK")){
            brewButton.setText(R.string.button_brew_refillmilk_text);
        }
        else if(status.equals("MORE_WATER")){
            brewButton.setText(R.string.button_brew_refillwater_text);
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

        processShowHide(jsonObj, "MilkLevel", milkLayout);
        processShowHide(jsonObj, "CoffeeLevel", coffeeLayout);
        processShowHide(jsonObj, "WaterLevel", waterLayout);
        processShowHide(jsonObj, "StrengthParam", coffeeSlideLayout);
        processShowHide(jsonObj, "WaterParam", waterSlideLayout);
        processShowHide(jsonObj, "MilkParam", milkSlideLayout);
        processShowHide(jsonObj, "FrothParam", frothSlideLayout);

    }

    void doLevelAnim(ProgressBar bar, int value){
        ObjectAnimator anim = ObjectAnimator.ofInt(bar, "progress", bar.getProgress(), value);
        anim.setDuration(500);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    void processLevels(JSONObject jsonObj) throws JSONException{
        if(jsonObj.has("Milk")){
            doLevelAnim(milkBar, jsonObj.getInt("Milk"));
        }

        if(jsonObj.has("Water")){
            doLevelAnim(waterBar, jsonObj.getInt("Water"));
        }

        if(jsonObj.has("Coffee")){
            doLevelAnim(coffeeBar, jsonObj.getInt("Coffee"));
        }
    }

    void processCoffeeStart(JSONObject jsonObj) throws JSONException{
        if(jsonObj.getString("Status").equals("SUCCESS")) {
            brewButton.setEnabled(false); //Disable button until coffee is done
            brewButton.setText(R.string.button_brew_brewing_text);
        }
        else{
            brewButton.setText(R.string.button_brew_fail_text);
        }
    }

    private class readerThread implements Runnable{

        private InputStream in;

        public readerThread(InputStream in){
            this.in = in;
        }

        @Override
        public void run(){

            try {
                mSocket = mBrewMachine.mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("52993379-2ab4-4b4f-9bce-535bc1324c85"));
                mSocket.connect();

                inStr = mSocket.getInputStream();
                outStr = mSocket.getOutputStream();

            } catch (IOException e) {
                //TODO: Recover gracefully
                e.printStackTrace();
            }
            this.in = inStr;

            if(outStr == null || inStr == null) return;

            sendFeatureListRequest();
            sendLevelRequest();

            while(!Thread.currentThread().isInterrupted()) {
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bytes; // bytes returned from read()
                final JSONObject jsonObject;

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

                        if(jsonObject.has("Machine")){
                            if((jsonObject.getInt("Machine") != mBrewMachine.deviceID)){
                                continue;   //Not relevant
                            }
                        }

                        if(jsonObject.has("Response")) {
                            if (jsonObject.getString("Response").equals("COFFEE_START")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                    try {
                                        processCoffeeStart(passObj);
                                    }
                                    catch (JSONException e){
                                        return; //non critical -> ignore and continue
                                    }
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

                } catch (IOException e){
                    //Thread interrupted or socket closed. end processing.
                    try {
                        Log.i("Brewtooth", "Closing socket...");
                        mSocket.close();
                    }
                    catch (IOException e2){
                        //How could this happen?
                        Log.e("Brewtooth", "Unable to close bluetooth socket");
                    }
                    return;
                }
            }

            try {
                Log.i("Brewtooth", "Closing socket...");
                mSocket.close();
            }
            catch (IOException e){
                //How could this happen?
                Log.e("Brewtooth", "Unable to close bluetooth socket");
            }
        }
    }
}
