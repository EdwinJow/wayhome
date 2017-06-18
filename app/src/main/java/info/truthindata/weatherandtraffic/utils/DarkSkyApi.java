package info.truthindata.weatherandtraffic.utils;

import android.location.Location;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DarkSkyApi {
    public JsonObject GetCurrentForecast(Location location, String apiKey){
        String result = HttpRequest.get(String.format("https://api.darksky.net/forecast/%s/%d,%d", apiKey, location.getLatitude(), location.getLongitude())).body();
        return new JsonParser().parse(result).getAsJsonObject();
    }
}
