package uk.ac.soton.ecs.ciaran.brewtooth;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Ciaran Woodward on 30/11/2016.
 */

public class BrewMachineAdapter extends ArrayAdapter {

    private ArrayList<BrewMachine> data;
    private Context context;
    private int layoutId;

    public BrewMachineAdapter(Context context, ArrayList<BrewMachine> input){
        super(context, R.layout.brew_machine_view, input);
        this.layoutId = R.layout.brew_machine_view;
        this.context = context;
        this.data = input;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View toReturn = convertView;
        BrewMachineViewHolder viewHolder;

        if(position >= data.size()) return null;

        if(toReturn == null){
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            toReturn = inflater.inflate(layoutId, parent, false);

            viewHolder = new BrewMachineViewHolder();
            viewHolder.capabilities = (TextView)toReturn.findViewById(R.id.device_caps);
            viewHolder.name = (TextView)toReturn.findViewById(R.id.device_name);
            viewHolder.location = (TextView)toReturn.findViewById(R.id.device_location);
            toReturn.setTag(viewHolder);

        }
        else{
            viewHolder = (BrewMachineViewHolder) toReturn.getTag();
        }

        BrewMachine curMachine = data.get(position);
        viewHolder.capabilities.setText(curMachine.capabilities);
        viewHolder.location.setText(curMachine.location);
        viewHolder.name.setText(curMachine.name);

        return toReturn;
    }

    static class BrewMachineViewHolder{
        TextView capabilities;
        TextView location;
        TextView name;
    }
}
