package info.truthindata.weatherandtraffic.models;

public class GoogleDirectionsResult {
    public String status;
    public Routes[] routes;

    public static class Routes{
        public String summary;
        public OverviewPolyline overview_polyline;
        public String[] warnings;
        public Legs[] legs;
    }

    public static class OverviewPolyline{
        public String points;
    }

    public static class Legs {
        public Distance distance;
        public Duration duration;
    }

    public static class Distance {
        public String text;
    }

    public static class Duration{
        public String text;
    }
}

