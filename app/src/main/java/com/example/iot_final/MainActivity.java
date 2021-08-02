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
import android.os.AsyncTask;
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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import static com.example.iot_final.MainActivity.Constants.*;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener, View.OnClickListener {



    public class Constants {
        private static final char KEY = 69;

        public static final int NUM_SENSORS = 7;
        public static final int TOPIC_LIGHT = 0;
        public static final int TOPIC_TEMPERATURE = 1;
        public static final int TOPIC_COMPASS = 2;
        public static final int TOPIC_ACCELEROMETER = 3;
        public static final int TOPIC_LONGITUDE = 4;
        public static final int TOPIC_LATITUDE = 5;
        public static final int TOPIC_LED = 6;
        public static final int MAX_X = 10;
        public static final int UPLOAD_PERIOD = 30;
        public static final String CHANNEL_ID = "Adafruit";
        public static final String TOPIC_LED_PATH= "thanhdanh27600/f/iot-led";


    }

    GraphView graphViewLight;
    double buffer_index = 0.0;
    DataPoint[] dataPointLight = new DataPoint[MAX_X];
    LineGraphSeries<DataPoint> series;
    MyTask task;
    MQTTHelper mqttHelper;
    final String TAG = "IOT_FINAL";
    TextView[] textViews = new TextView[NUM_SENSORS];
    TextView textViewRemain;
    BottomNavigationView bottomNavigationView;
    boolean uartInitialized = false;
    String bufferUART;
    JSONObject jsonUART;
    EncryptionUART messageUART;
    String ADA_KEY = "aio_ftIB86Yh38Gd" + "ufYcdKbI8YoYVl6K";
    int wdt_counter = UPLOAD_PERIOD;
    private Timer timer = new Timer();

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    UsbSerialPort port;
    double longitude, latitude;
    int TAG_CODE_PERMISSION_LOCATION;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initTextView();
        initGraph();
        setupWDT();
        openUART();
        initNotificationChannel();
        task = new MyTask();
        task.execute();
        startMQTT();
        getLocation();


    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.temperature:
                sendDataMQTT(textViews[TOPIC_TEMPERATURE].getText() + "", TOPIC_TEMPERATURE);
                break;
            case R.id.light:
                sendDataMQTT(textViews[TOPIC_LIGHT].getText() + "", TOPIC_LIGHT);
                break;
            case R.id.compass:
                sendDataMQTT(textViews[TOPIC_COMPASS].getText() + "", TOPIC_COMPASS);
                break;
            case R.id.accelerometer:
                sendDataMQTT(textViews[TOPIC_ACCELEROMETER].getText() + "", TOPIC_ACCELEROMETER);
                break;
//            case R.id.button2:
//                MapObj obj = new MapObj("Danh", 10.4273388, 107.3005107, 16.5);
//                sendObjMQTT(obj);
//
//
//                break;

        }

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
                //Toast.makeText(getApplicationContext(), bufferUART , Toast.LENGTH_SHORT).show();
                if (bufferUART.contains("#") && bufferUART.contains("!")) {
                    //TODO
                    int index_soc = bufferUART.indexOf("#");
                    int index_eof = bufferUART.indexOf("!");
                    String cmd = bufferUART.substring(index_soc + 1, index_eof);
//                String[] cmd_parsed = cmd.split("\\s");
                    messageUART = new EncryptionUART(cmd, Constants.KEY);
                    cmd = messageUART.decryptUART();

                    try {
                        jsonUART = new JSONObject(cmd);
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), "Cannot Create Object", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    try {
                        textViews[TOPIC_LIGHT].setText(jsonUART.getString("l"));
                        textViews[TOPIC_TEMPERATURE].setText(jsonUART.getString("t"));
                        textViews[TOPIC_COMPASS].setText(mapCompass(jsonUART.getString("d")));
                        textViews[TOPIC_ACCELEROMETER].setText(mapAccelerometer(jsonUART.getString("a")));


                        int previousX = ((int) buffer_index >= MAX_X) ? MAX_X - 1 : (int) buffer_index;
                        if (dataPointLight[previousX].getY() != Double.parseDouble(jsonUART.getString("l"))) {
                            if (previousX == MAX_X - 1) {
                                for (int index = 1; index < MAX_X; index++) {
                                    dataPointLight[index - 1] = dataPointLight[index];
                                }
                            }
                            else{
                                for (int i = previousX + 1; i< MAX_X; i++){
                                    dataPointLight[i] = new DataPoint(previousX, Double.parseDouble(jsonUART.getString("l")));
                                }
                            }

                            dataPointLight[previousX] = new DataPoint(buffer_index, Double.parseDouble(jsonUART.getString("l")));
                            series = new LineGraphSeries<>(dataPointLight);
                            showDataOnGraph(series, graphViewLight);
                            buffer_index++;
                        }

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

    public void initGraph() {
        graphViewLight = findViewById(R.id.graphLightLevel);
        graphViewLight.getLegendRenderer().setVisible(true);
        graphViewLight.getViewport().setScalable(true);
        graphViewLight.setTitle("Light intensity (30s refresh)");
        graphViewLight.getGridLabelRenderer().setGridColor(Color.DKGRAY);

        for (int i = 0; i < MAX_X; i++) {
            dataPointLight[i] = new DataPoint(0, 0);
        }
        series = new LineGraphSeries<>(dataPointLight);
        showDataOnGraph(series, graphViewLight);

    }

    public void initTextView() {
        textViews[TOPIC_LIGHT] = findViewById(R.id.light);
        textViews[TOPIC_TEMPERATURE] = findViewById(R.id.temperature);
        textViews[TOPIC_COMPASS] = findViewById(R.id.compass);
        textViews[TOPIC_ACCELEROMETER] = findViewById(R.id.accelerometer);
        textViews[TOPIC_LONGITUDE] = findViewById(R.id.longtitude);
        textViews[TOPIC_COMPASS] = findViewById(R.id.compass);
        textViews[TOPIC_LATITUDE] = findViewById(R.id.latitude);
        textViews[TOPIC_LED] = findViewById(R.id.led);
        textViewRemain = findViewById(R.id.remain);

        for (int i = TOPIC_LIGHT; i <= TOPIC_ACCELEROMETER; i++){
            textViews[i].setOnClickListener(this);
        }

        findViewById(R.id.line_wrapper).setVisibility(View.GONE);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.line:
                        findViewById(R.id.dashboard_wrapper).setVisibility(View.GONE);
                        findViewById(R.id.location_wrapper).setVisibility(View.GONE);
                        findViewById(R.id.led_wrapper).setVisibility(View.GONE);
                        findViewById(R.id.line_wrapper).setVisibility(View.VISIBLE);
                        break;
                    case R.id.dashboard:
                        findViewById(R.id.dashboard_wrapper).setVisibility(View.VISIBLE);
                        findViewById(R.id.led_wrapper).setVisibility(View.VISIBLE);
                        findViewById(R.id.location_wrapper).setVisibility(View.VISIBLE);
                        findViewById(R.id.line_wrapper).setVisibility(View.GONE);
                        break;
                }
                return true;
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
                    //port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    //port.write("ABC#".getBytes(), 1000);

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
            byte[] b = obj.toJSON().toString().getBytes(Charset.forName("UTF-8"));
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
            byte[] b = data.getBytes(Charset.forName("UTF-8"));
            msg.setPayload(b);
            Log.d("IOT_MQTT", "Publish :" + msg);

            switch (ID) {
                case TOPIC_TEMPERATURE:
                    mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-temperature", msg);
                    break;
                case TOPIC_LIGHT:
                    mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-light", msg);
                    break;
                case TOPIC_COMPASS:
                    mqttHelper.mqttAndroidClient.publish("thanhdanh27600/feeds/iot-compass", msg);
                    break;
                case TOPIC_ACCELEROMETER:
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


                switch (topic){
                    case Constants.TOPIC_LED_PATH:
                        Log.i("MQTT", "TOPIC:" + topic + "MESSAGE:"+ mqttMessage.toString());
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Constants.CHANNEL_ID)
                                .setSmallIcon(R.drawable.logo)
                                .setContentTitle("Message from Adafruit")
                                .setStyle(new NotificationCompat.BigTextStyle().bigText("LED's status: " + mqttMessage.toString() + "\nThis message is automatically pushed from the Adafruit to let you know the feed change status"))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        builder.setContentText("LED's status: " + mqttMessage.toString());
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                        notificationManager.notify((int)(Math.random() * 1001), builder.build());
                        textViews[TOPIC_LED].setText(mqttMessage.toString());
                        String dataToGateway = "";
                        switch (mqttMessage.toString()){
                            case "ON":
                                dataToGateway = "1";
                                break;
                            default:
                                dataToGateway = "0";
                        }
                        messageUART = new EncryptionUART(dataToGateway, Constants.KEY);
                        dataToGateway = messageUART.encryptUART();
                        dataToGateway = "!" + dataToGateway + "#";
                        port.write(dataToGateway.getBytes(), 1000);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + topic);
                }
                return;
//
//                try {
//                    JSONObject jsonObjectReceive = new JSONObject(mqttMessage.toString());
//                    String ID = jsonObjectReceive.getString("id");
//                    String name = jsonObjectReceive.getString("name");
//                    String value = jsonObjectReceive.getString("data");
//                    String databaseName, databaseID, databaseFeed;
//                    for (int i = 0; i < Constants.NUM_DEVICES; i++) {
//                        JSONObject jsonObjectTemp = jsonArray.getJSONObject(i);
//                        databaseName = jsonObjectTemp.getString("name");
//                        databaseID = jsonObjectTemp.getString("id");
//                        Log.d("------------------Topic", "bruh:" + topic);
//                        if (ID.equals(databaseID) && name.equals(databaseName)) {
//                            updateTextView(ID, value);
//                            break;
//                        }
//                    }
//                    dataToGateway = "!" + ID + ":" + value + "#";
//                } catch (JSONException e) {
//                    Log.e("JSONException", "Error: " + e.toString());
//                }
//                try {
//                    port.write(dataToGateway.getBytes(), 1000);
//                } catch (Exception e) {
//
//                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }


    private class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
//            try {
//                String apiURL = "http://dadn.esp32thanhdanh.link";
//                if (!apiURL.equals("none")) {
//                    Log.d("apiURL", apiURL);
//                    Log.d("length", Integer.toString(apiURL.length()));
//                    getKey(apiURL);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

//            Log.i("KEY RESPONSE", keyResponse);
//            String split []=keyResponse.split("\"");
//            split = split[3].split(":");
//            dynamicKey1 = split[0];
//            dynamicKey2 = split[1];
//            Log.i("KEY1", dynamicKey1);
//            Log.i("KEY2", dynamicKey2);
//            startMQTT();
//            myAsyncTask = new myTask();
//            myAsyncTask.execute();
        }
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
                textViews[TOPIC_LONGITUDE].setText(String.format("%.6f", longitude));
                textViews[TOPIC_LATITUDE].setText(String.format("%.6f", latitude));
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
            textViews[TOPIC_LONGITUDE].setText(String.format("%.6f", longitude));
            textViews[TOPIC_LATITUDE].setText(String.format("%.6f", latitude));
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

    private class MyTimerTask extends TimerTask{
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    wdt_counter--;
                    if (wdt_counter < 0) {
                        wdt_counter = UPLOAD_PERIOD;
                        MapObj obj = new MapObj("Danh", latitude, longitude);
                        sendObjMQTT(obj);
                    }
                    textViewRemain.setText("next upload:" + wdt_counter);
                }
            });
        }
    }

}