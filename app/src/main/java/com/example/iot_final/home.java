package com.example.iot_final;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link home#newInstance} factory method to
 * create an instance of this fragment.
 */
public class home extends Fragment implements View.OnClickListener {

    ItemViewModel viewModel;
    Item item;

    public class Constants {
        public static final int NUM_TEXTVIEWS = 6;
        public static final int TOPIC_LIGHT = 0;
        public static final int TOPIC_TEMPERATURE = 1;
        public static final int TOPIC_COMPASS = 2;
        public static final int TOPIC_ACCELEROMETER = 3;
        public static final int TOPIC_LED = 4;

        public static final int NEXT_UPLOAD = 5;
    }



    TextView[] textViews;

    public home() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment home.
     */
    // TODO: Rename and change types and number of parameters
    public static home newInstance(String param1, String param2) {
        home fragment = new home();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textViews = new TextView[Constants.NUM_TEXTVIEWS];
        viewModel = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        item = new Item();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_home, container, false);

        textViews[Constants.TOPIC_LIGHT] = view.findViewById(R.id.light);
        textViews[Constants.TOPIC_TEMPERATURE] = view.findViewById(R.id.temperature);
        textViews[Constants.TOPIC_COMPASS] = view.findViewById(R.id.compass);
        textViews[Constants.TOPIC_ACCELEROMETER] = view.findViewById(R.id.accelerometer);
        textViews[Constants.TOPIC_LED] = view.findViewById(R.id.led);

        for (int i = Constants.TOPIC_LIGHT; i <= Constants.TOPIC_ACCELEROMETER; i++){
            textViews[i].setOnClickListener(this);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getSelectedItem().observe(requireActivity(), item -> {
            // Perform an action with the latest item data
            textViews[Constants.TOPIC_LIGHT].setText(item.light);
            textViews[Constants.TOPIC_TEMPERATURE].setText(item.temperature);
            textViews[Constants.TOPIC_COMPASS].setText(item.compass);
            textViews[Constants.TOPIC_ACCELEROMETER].setText(item.accelerometer);
            textViews[Constants.TOPIC_LED].setText(item.led);
        });

    }

    @Override
    public void onClick(View view) {
        Log.i("CLICK:",view.getId() + "");
        switch (view.getId()) {
            case R.id.light:
                item.send_light = true;
                item.light = textViews[Constants.TOPIC_LIGHT].getText() + "";
                viewModel.selectItem(item);
                break;
            case R.id.temperature:
                item.send_temperature = true;
                item.temperature = textViews[Constants.TOPIC_TEMPERATURE].getText() + "";
                viewModel.selectItem(item);
                break;
            case R.id.compass:
                item.send_compass = true;
                item.compass = textViews[Constants.TOPIC_COMPASS].getText() + "";
                viewModel.selectItem(item);
                break;
            case R.id.accelerometer:
                item.send_accelerometer = true;
                item.accelerometer = textViews[Constants.TOPIC_ACCELEROMETER].getText() + "";
                viewModel.selectItem(item);
                break;
        }

    }


}