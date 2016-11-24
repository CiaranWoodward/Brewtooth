package uk.ac.soton.ecs.ciaran.brewtooth;

import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import static uk.ac.soton.ecs.ciaran.brewtooth.R.styleable.View;

public class DeviceList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        if(savedInstanceState == null){
           // getSupportFragmentManager().beginTransaction().add(R.id.listview_devices, new PlaceholderFragment()).commit();
        }
    }

    //Placeholder
    public static class PlaceholderFragment extends Fragment {
        public PlaceholderFragment() {
        }
    }
}
