package com.example.iot_final;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.eclipse.paho.android.service.BuildConfig;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {


    public static class Constants {
        private static final char KEY = 69;
        public static int UPLOAD_PERIOD = 30;
        public static int minLightThreshold = -1;
        public static int maxLightThreshold = -1;
        public static final int NUM_SENSORS = 7;
        public static final int TOPIC_LIGHT = 0;
        public static final int TOPIC_TEMPERATURE = 1;
        public static final int TOPIC_COMPASS = 2;
        public static final int TOPIC_ACCELEROMETER = 3;
        public static final int TOPIC_LONGITUDE = 4;
        public static final int TOPIC_LATITUDE = 5;
        public static final int TOPIC_LED = 6;
        public static final int MAX_X = 70;
        public static final String CHANNEL_ID = "Adafruit";
        public static final String TOPIC_LED_PATH = "thanhdanh27600/f/iot-led";
        public static final int SEND_LOCATION_FLAG = 0;
    }

    double buffer_index = 0d;
    GraphView graphViewLight;
    GraphView graphViewTemp;
    double latest_light_value = Double.MIN_VALUE;
    double latest_temp_value = Double.MIN_VALUE;
    LineGraphSeries<DataPoint> series1;
    LineGraphSeries<DataPoint> series2;
    MQTTHelper mqttHelper;
    final String TAG = "IOT_FINAL";
    TextView[] textViews = new TextView[Constants.NUM_SENSORS];

    boolean uartInitialized = false;
    String bufferUART;
    JSONObject jsonUART;
    EncryptionUART messageUART;
    String ADA_KEY = "aio_ftIB86Yh38Gd" + "ufYcdKbI8YoYVl6K";
    int wdt_counter = Constants.UPLOAD_PERIOD;
    private final Timer timer = new Timer();
    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    UsbSerialPort port;
    double longitude, latitude;
    int TAG_CODE_PERMISSION_LOCATION;


    ItemViewModel viewModel;
    Item item;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViewModel();
        initNotificationChannel();
        setupWDT();
        openUART();
        startMQTT();
        getLocation();
    }

    private void showDataOnGraph(LineGraphSeries<DataPoint> series, GraphView graph) {

        if (graph.getSeries().size() > 0) {
            graph.getSeries().remove(0);
        }
        graph.addSeries(series);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(10);
    }


    @Override
    public void onNewData(byte[] data) {
        try {
            runOnUiThread(() -> {
                bufferUART += new String(data);
                if (bufferUART.contains("#") && bufferUART.contains("!")) {
                    //TODO
                    int index_soc = bufferUART.indexOf("#");
                    int index_eof = bufferUART.indexOf("!");
                    String cmd = bufferUART.substring(index_soc + 1, index_eof);
                    messageUART = new EncryptionUART(cmd, Constants.KEY);
                    cmd = messageUART.decryptUART();

                    try {
                        jsonUART = new JSONObject(cmd);
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), "Cannot Create Object", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    try {
                        item.light = jsonUART.getString("l");
                        item.temperature = jsonUART.getString("t");
                        item.compass =  mapCompass(jsonUART.getString("d"));
                        item.accelerometer =  mapAccelerometer(jsonUART.getString("a"));
                        latest_light_value = Double.parseDouble(jsonUART.getString("l"));
                        latest_temp_value = Double.parseDouble(jsonUART.getString("t"));

                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), "Cannot Parse Value", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    bufferUART = "";
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String mapCompass(String value) {
        switch (value.charAt(0)) {
            case '1':
                return "N";
            case '2':
                return "NE";
            case '3':
                return "E";
            case '4':
                return "SE";
            case '5':
                return "S";
            case '6':
                return "SW";
            case '7':
                return "W";
            case '8':
                return "NW";
        }
        return "?";
    }

    public String mapAccelerometer(String value) {
        switch (value.charAt(0)) {
            case '1':
                return "UP";
            case '2':
                return "DOWN";
            case '3':
                return "LEFT";
            case '4':
                return "RIGHT";
            case '5':
                return "FACE UP";
            case '6':
                return "FACE DOWN";
            case '7':
                return "SHAKE";
        }
        return "?";
    }

    @Override
    public void onRunError(Exception e) {
        e.printStackTrace();
    }

    public void initViewModel() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.getNavController());


        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        item = new Item();
        item.next_upload = Constants.UPLOAD_PERIOD;
        item.light = "?";
        item.temperature = "?";
        item.compass = "?";
        item.accelerometer = "?";
        item.led = "?";
        item.latest_temp_value = Double.MIN_VALUE;
        item.latest_light_value = Double.MIN_VALUE;

        viewModel.selectItem(item);
        viewModel.getSelectedItem().observe(this, item -> {
            if(item.send_light){
                sendDataMQTT(item.light, Constants.TOPIC_LIGHT);
                item.send_light = false;
            }
            if(item.send_temperature){
                sendDataMQTT(item.temperature, Constants.TOPIC_TEMPERATURE);
                item.send_temperature = false;
            }
            if(item.send_compass){
                sendDataMQTT(item.compass, Constants.TOPIC_COMPASS);
                item.send_compass = false;
            }
            if(item.send_accelerometer){
                sendDataMQTT(item.accelerometer, Constants.TOPIC_ACCELEROMETER);
                item.send_accelerometer = false;
            }
            if(item.lightThreshold[0] != -1){
                Log.i("FROM MAIN, light", item.lightThreshold[0] + "-"+item.lightThreshold[1]);
                Constants.minLightThreshold = item.lightThreshold[0];
                Constants.maxLightThreshold = item.lightThreshold[1];
                item.lightThreshold[0] = -1;
            }
            if(item.timerSetting != -1){
                Log.i("FROM MAIN, timerSetting", item.timerSetting + "");
                //change timer period
                Constants.UPLOAD_PERIOD = item.timerSetting;
                wdt_counter = item.timerSetting;
                String dataToGateway = "t:" + item.timerSetting;
                messageUART = new EncryptionUART(dataToGateway, Constants.KEY);
                dataToGateway = messageUART.encryptUART();
                dataToGateway = "#" + dataToGateway + "!";
                try {
                    if(port != null) port.write(dataToGateway.getBytes(), 1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                item.timerSetting = -1;
            }

        });

    }

    private void openUART() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Toast.makeText(MainActivity.this, "UART is not available", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getApplicationContext(), "UART OK", Toast.LENGTH_SHORT).show();
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);
                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));
                return;
            } else {
                port = driver.getPorts().get(0);
                try {
                    port.open(connection);
                    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                    Toast.makeText(MainActivity.this, "UART is openned", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "UART is erorr", Toast.LENGTH_SHORT).show();
                }
            }
        }
        uartInitialized = true;
    }


    private void setupWDT() {
        timer.schedule(new MyTimerTask(), 3000, 1000);
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendObjMQTT(MapObj obj) {
        MqttMessage msg = new MqttMessage();
        msg.setId((int) Math.round(Math.random() * 1001));
        msg.setQos(0);
        try {
            byte[] b = obj.toJSON().toString().getBytes(StandardCharsets.UTF_8);
            msg.setPayload(b);
            Log.d("IOT_MQTT", "Publish :" + msg);
            mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-final", msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void sendDataMQTT(String data, int ID) {

        MqttMessage msg = new MqttMessage();
        msg.setId((int) Math.round(Math.random() * 1001));
        msg.setQos(0);

        try {
            byte[] b = data.getBytes(StandardCharsets.UTF_8);
            msg.setPayload(b);
            Log.d("IOT_MQTT", "Publish :" + msg);

            switch (ID) {
                case Constants.TOPIC_TEMPERATURE:
                    mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-temperature", msg);
                    break;
                case Constants.TOPIC_LIGHT:
                    mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-light", msg);
                    break;
                case Constants.TOPIC_COMPASS:
                    mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-compass", msg);
                    break;
                case Constants.TOPIC_ACCELEROMETER:
                    mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-accelerometer", msg);
                    break;
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void startMQTT() {
        mqttHelper = new MQTTHelper(getApplicationContext(), "user" + (int) (Math.random() * 1001), "thanhdanh27600/f/+", "thanhdanh27600", ADA_KEY);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.d(TAG, "Init successful");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "Connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                switch (topic) {
                    case Constants.TOPIC_LED_PATH:
                        Log.i("MQTT", "TOPIC:" + topic + "MESSAGE:" + mqttMessage.toString());
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Constants.CHANNEL_ID)
                                .setSmallIcon(R.drawable.logo)
                                .setContentTitle("Message from Adafruit")
                                .setStyle(new NotificationCompat.BigTextStyle().bigText("LED's status: " + mqttMessage.toString() + "\nThis message is automatically pushed from the Adafruit to let you know the feed change status"))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        builder.setContentText("LED's status: " + mqttMessage.toString());
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                        notificationManager.notify((int) (Math.random() * 1001), builder.build());
                        item.led = mqttMessage.toString();
                        viewModel.selectItem(item);
                        String dataToGateway = "";
                        switch (mqttMessage.toString()) {
                            case "ON":
                                dataToGateway = "l:1";
                                break;
                            default:
                                dataToGateway = "l:0";
                        }
                        messageUART = new EncryptionUART(dataToGateway, Constants.KEY);
                        dataToGateway = messageUART.encryptUART();
                        dataToGateway = "#" + dataToGateway + "!";
                        port.write(dataToGateway.getBytes(), 1000);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + topic);
                }
                return;
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void updateLightTemp() throws IOException {
        item.latest_light_value = latest_light_value;
        item.latest_temp_value = latest_temp_value;
        viewModel.selectItem(item);
        String dataToGateway = "";
        if( Constants.minLightThreshold <= latest_light_value && latest_temp_value <= Constants.maxLightThreshold){
            dataToGateway = "l:1";
        }
        else{
            dataToGateway = "l:0";
        }
        messageUART = new EncryptionUART(dataToGateway, Constants.KEY);
        dataToGateway = messageUART.encryptUART();
        dataToGateway = "#" + dataToGateway + "!";
        if(port !=null) port.write(dataToGateway.getBytes(), 1000);
    }


    private void getLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, locationListener);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            try {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                textViews[Constants.TOPIC_LONGITUDE].setText(String.format("%.6f", longitude));
                textViews[Constants.TOPIC_LATITUDE].setText(String.format("%.6f", latitude));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    TAG_CODE_PERMISSION_LOCATION);
        }
        return;
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    wdt_counter--;
                    if (wdt_counter < 0) {
                        wdt_counter = Constants.UPLOAD_PERIOD;
                        try {
                            updateLightTemp();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        MapObj obj = new MapObj("Danh", latitude, longitude);
                        if (Constants.SEND_LOCATION_FLAG == 1) sendObjMQTT(obj);
                    }
                    item.next_upload = wdt_counter;
                    viewModel.selectItem(item);
                }
            });
        }
    }

}