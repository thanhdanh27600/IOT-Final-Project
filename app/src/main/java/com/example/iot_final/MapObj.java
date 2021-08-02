package com.example.iot_final;

import org.json.JSONException;
import org.json.JSONObject;

public class MapObj {
    private static String value;
    private static double lat;
    private static double lon;

    public MapObj(String value, double lat, double lon){
        this.value = value;
        this.lat = lat;
        this.lon = lon;
    }



    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value",this.value);
            jsonObject.put("lat",new Double(this.lat));
            jsonObject.put("lon",new Double(this.lon));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}
