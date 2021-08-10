package com.example.iot_final;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.material.slider.RangeSlider;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link setting#newInstance} factory method to
 * create an instance of this fragment.
 */
public class setting extends Fragment {

    //some
    RangeSlider timer,light;
    Switch autoLight;
    ItemViewModel viewModel;
    Item item;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public setting() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment setting.
     */
    // TODO: Rename and change types and number of parameters
    public static setting newInstance(String param1, String param2) {
        setting fragment = new setting();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        item = new Item();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_setting, container, false);

        timer = view.findViewById(R.id.timer);
        light = view.findViewById(R.id.light);
        autoLight = view.findViewById(R.id.auto_light);
        light.setEnabled(false);

        timer.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {
                //do nothing
            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                List timerValue = timer.getValues();
                int returnTimerValue = Math.round((Float) timerValue.get(0));
                //Log.d("Timer", returnTimerValue+"");
                //do something
                item.timerSetting = returnTimerValue;
                viewModel.selectItem(item);

            }
        });

        light.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {
                //do nothing
            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                List lightValues = light.getValues();
                int returnMinLightThreshold = Math.round((Float) lightValues.get(0));
                int returnMaxLightThreshold = Math.round((Float) lightValues.get(1));
                //Log.d("Timer", "Min:" + returnMinLightThreshold + " | Max:" + returnMaxLightThreshold);
                //do something
                item.lightThreshold[0] = returnMinLightThreshold;
                item.lightThreshold[1] = returnMaxLightThreshold;
                viewModel.selectItem(item);

            }
        });

        autoLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                light.setEnabled(true);
                //do something

            } else {
                light.setEnabled(false);
                //do something

            }
        });
        return view;
    }
}