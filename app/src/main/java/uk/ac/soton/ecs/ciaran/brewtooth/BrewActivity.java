package uk.ac.soton.ecs.ciaran.brewtooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

public class BrewActivity extends AppCompatActivity {

    ProgressBar waterBar;
    ProgressBar coffeeBar;
    ProgressBar milkBar;

    LinearLayout waterLayout;
    LinearLayout coffeeLayout;
    LinearLayout milkLayout;

    Button brewButton;

    SeekBar strengthBar;
    SeekBar volumeBar;

    LinearLayout strengthLayout;
    LinearLayout volumeLayout;

    boolean initialised = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brew);

        waterLayout = (LinearLayout) this.findViewById(R.id.layout_water);
        coffeeLayout = (LinearLayout) this.findViewById(R.id.layout_coffee);
        milkLayout = (LinearLayout) this.findViewById(R.id.layout_milk);
        strengthLayout = (LinearLayout) this.findViewById(R.id.layout_strength);
        volumeLayout = (LinearLayout) this.findViewById(R.id.layout_volume);

        waterBar = (ProgressBar) waterLayout.findViewById(R.id.progressBar_water);
        coffeeBar = (ProgressBar) coffeeLayout.findViewById(R.id.progressBar_coffee);
        milkBar = (ProgressBar) milkLayout.findViewById(R.id.progressBar_milk);
        strengthBar = (SeekBar) strengthLayout.findViewById(R.id.seekBar_strength);
        volumeBar = (SeekBar) volumeLayout.findViewById(R.id.seekBar_volume);

        brewButton = (Button) this.findViewById(R.id.button_brew);

        brewButton.setOnClickListener(mBrewButtonListener);
        initialised = true;
    }

    private View.OnClickListener mBrewButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if(initialised) {
                brewButton.setText("Success!");
            }
        }
    };
}
