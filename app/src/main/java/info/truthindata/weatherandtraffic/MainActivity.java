package info.truthindata.weatherandtraffic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import info.truthindata.weatherandtraffic.models.ForecastResult;
import info.truthindata.weatherandtraffic.models.GoogleDirectionsResult;
import info.truthindata.weatherandtraffic.utils.HttpRequest;

import static android.R.attr.duration;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private static final String PreferencesFileName = "wayhome";
    private static String mDarkSkyApiKey = null;
    Map<String, Marker> markers = new HashMap<>();
    private static final String TAG = MainActivity.class.getSimpleName();
    LocationManager locationManager;
    private String provider;
    protected Location currentLocation;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private PlaceAutocompleteFragment homeAutoComplete;
    private PlaceAutocompleteFragment workAutoComplete;
    public String homePlaceId;
    public String workPlaceId;

    private class DarkSkyForecast extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String[] params) {
            try{
                String key = params[0];
                String result = HttpRequest.get(String.format("https://api.darksky.net/forecast/%s/%f,%f", key, currentLocation.getLatitude(), currentLocation.getLongitude())).body();
                Gson gson = new Gson();
                ForecastResult forecast = new ForecastResult();

                ForecastResult forecastResult = gson.fromJson(result, forecast.getClass());

                return forecastResult.minutely.summary;
            }
            catch(Exception e){
                Exception f = e;
                return f.toString();
            }
        }

        @Override
        protected void onPostExecute(String message) {}
    }

    protected class DrivingDirections extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String[] params) {
            try{
                Context context = getApplicationContext();
                String from = params[0];
                String to = params[1];
                String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=place_id:%s&destination=place_id:%s&key=%s",
                        from,
                        to,
                        context.getString(R.string.google_directions_api_key));

                String json = HttpRequest
                        .get(url)
                        .body();

                Gson gson = new Gson();

                GoogleDirectionsResult directionModel = new GoogleDirectionsResult();
                GoogleDirectionsResult directionsResult = gson.fromJson(json, directionModel.getClass());

                String result = "";

                if(directionsResult.status.equals("OK"))
                {
                    String summary = directionsResult.routes[0].summary;

                    //break off if not using the turnpike
                    if(!summary.contains("I-276")){
                        result = "WARNING: Directions routed outside of normal driving area";
                        return result;
                    }

                    result += String.format("Directions from %s", params[2]) + "\n";
                    result += directionsResult.routes[0].legs[0].distance.text + "\n";
                    result += directionsResult.routes[0].legs[0].duration.text;
                }

                return result;
            }
            catch(Exception e){
                Exception f = e;
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String message) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        if(checkLocationPermission()){
            currentLocation = locationManager.getLastKnownLocation(provider);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        homeAutoComplete  = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_home_autocomplete_fragment);

        workAutoComplete = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_work_autocomplete_fragment);

        mGoogleApiClient = new GoogleApiClient
                .Builder( this )
                .enableAutoManage( this, 0, this )
                .addApi( Places.GEO_DATA_API )
                .addOnConnectionFailedListener(this)
                .build();

        // Retrieve user saved data
        SharedPreferences prefs = getSharedPreferences(PreferencesFileName, MODE_PRIVATE);
        String home = prefs.getString("home", null);
        String work = prefs.getString("work", null);
        String apiKey = prefs.getString("darkSkyApiKey", null);

        homeAutoComplete.setHint("Enter your home address");
        workAutoComplete.setHint("Enter your work address");

        if(work != null){
            setPlaceDataById(work, "work");
            workPlaceId = work;
        }

        if(home != null){
            setPlaceDataById(home, "home");
            homePlaceId = home;
        }

        if(work != null && home != null){
            try {
                getDrivingDirections();
            } catch (ExecutionException e) {
                Log.e(TAG, e.getMessage());
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        if (apiKey != null) {
            EditText darkSkyApiKey;
            darkSkyApiKey = (EditText) findViewById(R.id.editDarkSkyApi);
            darkSkyApiKey.setText(apiKey);
            mDarkSkyApiKey = apiKey;
        }

        homeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng t = place.getLatLng();
                Log.i(TAG, "Place Selected: " + place.getName());

                Marker marker = mMap.addMarker(new MarkerOptions().position(t).title(place.getName().toString()).snippet(place.getId()));
                marker.setTag("home");

                //rewrite the marker information to ensure single instances of home/work
                markers.put("home", marker);

                fitBounds();
            }

            @Override
            public void onError(Status status) {
                Log.e(TAG, "onError: Status = " + status.toString());
            }
        });

        workAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng t = place.getLatLng();
                Log.i(TAG, "Place Selected: " + place.getName());

                Marker marker = mMap.addMarker(new MarkerOptions().position(t).title(place.getName().toString()).snippet(place.getId()));
                marker.setTag("work");

                markers.put("work", marker);
                fitBounds();
            }

            @Override
            public void onError(Status status) {
                Log.e(TAG, "onError: Status = " + status.toString());
            }
        });
    }

    private void getDrivingDirections() throws ExecutionException, InterruptedException {
        String from;
        String to;
        if(currentLocation != null){
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            Marker homeMarker = markers.get("home");
            Marker workMarker = markers.get("work");

            if(homeMarker == null && workMarker == null){
                return;
            }

            LatLng homeLatLng = homeMarker.getPosition();
            LatLng workLatLng = workMarker.getPosition();

            double distanceToHome = SphericalUtil.computeDistanceBetween(latLng, homeLatLng);
            double distanceToWork = SphericalUtil.computeDistanceBetween(latLng, workLatLng);

            String startLocation;

            if(distanceToHome < distanceToWork){
                from = homePlaceId;
                to = workPlaceId;
                startLocation = "home";
            } else{
                from = workPlaceId;
                to = homePlaceId;
                startLocation = "work";
            }

            DrivingDirections drivingDirections = new DrivingDirections();
            final AsyncTask<String, Void, String> execute = drivingDirections.execute(from, to, startLocation);

            String directions = execute.get();

            Context context = getApplicationContext();

            Toast toast = Toast.makeText(context, directions, duration);
            toast.show();
        }

//        int duration = Toast.LENGTH_LONG;
//
//        Toast toast = Toast.makeText(context, currentForecast, duration);
//        toast.show();
    }

    private void setPlaceDataById(String placeId, final String markerTag){
        Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places) {
                        if (places.getStatus().isSuccess() && places.getCount() > 0) {
                            final Place place = places.get(0);
                            Log.i(TAG, "Place found: " + place.getName());
                            Marker marker = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString()).snippet(place.getId()));
                            marker.setTag(markerTag);
                            markers.put(markerTag, marker);

                            if(markerTag.equals("home")){
                                homeAutoComplete.setText(place.getName());
                            } else{
                                workAutoComplete.setText(place.getName());
                            }

                        } else {
                            Log.e(TAG, "Place not found");
                        }
                        fitBounds();
                        places.release();
                    }
                });
    }

    public void fitBounds(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Marker marker : markers.values()) {
            builder.include(marker.getPosition());
        }

        LatLngBounds bounds = builder.build();

        int padding = 60; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cu);
    }

    public void saveData(View view) throws ExecutionException, InterruptedException {
        SharedPreferences.Editor editor = getSharedPreferences(PreferencesFileName, MODE_PRIVATE).edit();
        EditText darkSkyApiEditText = (EditText)findViewById(R.id.editDarkSkyApi);
        String darkSkyApiString = darkSkyApiEditText.getText().toString();

        if(checkLocationPermission()){
            currentLocation = locationManager.getLastKnownLocation(provider);

            if(currentLocation != null){
                DarkSkyForecast getForecast = new DarkSkyForecast();
                final AsyncTask<String, Void, String> execute = getForecast.execute(darkSkyApiString);

                String currentForecast = execute.get();

                Context context = getApplicationContext();
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, currentForecast, duration);
                toast.show();

                getDrivingDirections();
            }
        }

        for (Marker marker : markers.values()) {
            editor.putString(marker.getTag().toString(), marker.getSnippet().toString());
        }

        editor.putString("darkSkyApiKey", darkSkyApiString);
        editor.apply();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng defaultCoords = new LatLng(40.1623518,-74.9563831);
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultCoords));
    }

    @Override
    public void onLocationChanged(Location location) {
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();

        Log.i(TAG + "Location info: Lat", lat.toString());
        Log.i(TAG + "Location info: Lng", lng.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission. ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                //Request location updates:
                locationManager.requestLocationUpdates(provider, 400, 1, this);
            }
        }

    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission. ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission. ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_required_toast)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission. ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission. ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        //Request location updates:
                        locationManager.requestLocationUpdates(provider, 400, 1, this);
                    }

                } else {
                    // permission denied
                }
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
