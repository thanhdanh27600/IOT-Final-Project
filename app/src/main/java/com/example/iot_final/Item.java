package com.example.iot_final;

public class Item {
    // DATA COMUNICATION
    public static String light;
    public static String temperature;
    public static String compass;
    public static String accelerometer;
    public static String led;
    public static int next_upload;
    public static double latest_light_value;
    public static double latest_temp_value;

    // SEND TO MQTT
    public static boolean send_light;
    public static boolean send_temperature;
    public static boolean send_compass;
    public static boolean send_accelerometer;
    public static boolean send_led;

    // Longitude and Latitude
    public static double longitude;
    public static double latitude;
}
