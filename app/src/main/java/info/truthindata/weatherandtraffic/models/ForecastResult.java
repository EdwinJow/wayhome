package info.truthindata.weatherandtraffic.models;

/**
 * Created by Ed on 6/29/2017.
 */

public class ForecastResult {
    public String latitude;
    public String longitude;
    public Currently currently;
    public Minutely minutely;
    public Hourly hourly;

    public static class Currently{
        public int time;
        public String summary;
        public String nearestStormDistance;
        public String humidity;
        public float precipProbability;
        public float temperature;
    }

    public static class Minutely{
        public String summary;
    }

    public static class Hourly{
        public String summary;
    }
}

