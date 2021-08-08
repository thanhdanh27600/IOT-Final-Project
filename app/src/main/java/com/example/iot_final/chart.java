package com.example.iot_final;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
 * Use the {@link chart#newInstance} factory method to
 * create an instance of this fragment.
 */
public class chart extends Fragment {
    GraphView graphViewLight;
    GraphView graphViewTemp;
    LineGraphSeries<DataPoint> series1;
    LineGraphSeries<DataPoint> series2;
    ItemViewModel viewModel;
    TextView viewNextRefresh;
    double buffer_index = 0d;
    public chart() {
        // Required empty public constructor

    }



    public static chart newInstance(String param1, String param2) {
        chart fragment = new chart();
        return fragment;
    }


    private void initChart(View view) {
        graphViewLight = view.findViewById(R.id.graphLightLevel);
        graphViewLight.getLegendRenderer().setVisible(true);
        graphViewLight.getGridLabelRenderer().setGridColor(R.color.main_card);
        graphViewLight.getViewport().setScalable(true);
        graphViewLight.getViewport().setScrollable(true);
        graphViewLight.setTitle("Light intensity (30s refresh)");
        graphViewLight.getGridLabelRenderer().setHorizontalAxisTitle("time (second)");
        graphViewLight.getGridLabelRenderer().setVerticalAxisTitle("Microbit Level");
        series1 = new LineGraphSeries<>();
        showDataOnGraph(series1, graphViewLight);

        graphViewTemp = view.findViewById(R.id.graphTempLevel);
        graphViewTemp.getLegendRenderer().setVisible(true);
        graphViewTemp.getGridLabelRenderer().setGridColor(R.color.main_card);
        graphViewTemp.getViewport().setScalable(true);
        graphViewTemp.getViewport().setScrollable(true);
        graphViewTemp.setTitle("Temperature (°C) (30s refresh)");
        graphViewTemp.getGridLabelRenderer().setHorizontalAxisTitle("time (second)");
        graphViewTemp.getGridLabelRenderer().setVerticalAxisTitle("Celcius");
        series2 = new LineGraphSeries<>();
        series2.setColor(Color.rgb(255, 0, 0));
        showDataOnGraph(series2, graphViewTemp);
    }

    private void showDataOnGraph(LineGraphSeries<DataPoint> series, GraphView graph) {
        if (graph.getSeries().size() > 0) {
            graph.getSeries().remove(0);
        }
        graph.addSeries(series);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(8);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_chart, container, false);
        viewNextRefresh = view.findViewById(R.id.next_refresh);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initChart(view);
        graphViewLight.setDrawingCacheEnabled(true);
        graphViewTemp.setDrawingCacheEnabled(true);
        viewModel = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe(requireActivity(), item -> {
            // Perform an action with the latest item data
            buffer_index++;
            viewNextRefresh.setText("next refresh: " + item.next_upload);
            try {
                    series1.appendData(new DataPoint(buffer_index, item.latest_light_value), true, MainActivity.Constants.MAX_X);
                    series2.appendData(new DataPoint(buffer_index, item.latest_temp_value), true, MainActivity.Constants.MAX_X);

            }catch (NumberFormatException e){
                e.printStackTrace();
            }


        });
    }

}