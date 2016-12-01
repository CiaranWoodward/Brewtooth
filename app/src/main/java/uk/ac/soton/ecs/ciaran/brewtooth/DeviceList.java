package uk.ac.soton.ecs.ciaran.brewtooth;

import android.view.View;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import static uk.ac.soton.ecs.ciaran.brewtooth.R.styleable.View;

public class DeviceList extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        ArrayList<BrewMachine> test = new ArrayList<BrewMachine>();
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
}
