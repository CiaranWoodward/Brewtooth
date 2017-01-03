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
}
