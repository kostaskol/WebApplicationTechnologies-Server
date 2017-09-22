package com.WAT.airbnb.util.helpers;

import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.util.XmlParser;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *  Static class that makes an HTTP call to the google maps API
 *  and returns the street name, city and country of the specified latitude and longitude
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class ReverseGeocoder {
    // BUG: Sometimes the API doesn't return a street name OR/AND city for the given latitude
    //      and longitude
    static public String[] convert(float lat, float lng) throws MalformedURLException, IOException {
        XmlParser parser = new XmlParser(Constants.DIR + "/config.xml");
        URL mapsUrl = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng +
                "&key=" + parser.get("api-key"));
        HttpURLConnection con = (HttpURLConnection) mapsUrl.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        if (con.getResponseCode() != 200) {
            throw new RuntimeException("Connection failed with http error code " + con.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String outp;
        StringBuilder addrBuilder = new StringBuilder();
        while ((outp = reader.readLine()) != null) {
            addrBuilder.append(outp);
        }


        JsonReader jsonReader = Json.createReader(new StringReader(addrBuilder.toString()));
        JsonObject object = jsonReader.readObject();
        if (object == null) {
            System.err.println("Json Object is null");
            return null;
        }
        jsonReader.close();
        if (object.getString("status").equals("OK")) {

            String[] response = new String[3];
            JsonObject result = object.getJsonArray("results").getJsonObject(0);
            response[Constants.ADDR_OFFS] = result.getString("formatted_address");
            JsonArray arr = result.getJsonArray("address_components");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject tmpObject = arr.getJsonObject(i);
                if (tmpObject.getJsonArray("types").getString(0).equals("country")) {
                    response[Constants.COUNTRY_OFFS] = tmpObject.getString("long_name");
                    break;
                } else if (tmpObject.getJsonArray("types").getString(0).equals("locality")) {
                    response[Constants.CITY_OFFS] = tmpObject.getString("long_name");
                }
            }
            return response;
        }
        throw new RuntimeException("Bad status code: " + object.getString("status"));
    }
}
