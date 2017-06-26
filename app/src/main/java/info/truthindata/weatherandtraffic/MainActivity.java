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
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
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
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    private GoogleMap mMap;
    private static final String PreferencesFileName = "wayhome";
    private static String mDarkSkyApiKey = null;
    protected List<Marker> markers = new ArrayList<>();
    protected JsonObject forecastResult;
    private static final String TAG = MainActivity.class.getSimpleName();
    LocationManager locationManager;
    private String provider;
    private Location currentLocation;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

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

        // Retrieve the PlaceAutocompleteFragment.
        PlaceAutocompleteFragment homeAutoComplete = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_home_autocomplete_fragment);

        // Retrieve the PlaceAutocompleteFragment.
        PlaceAutocompleteFragment workAutoComplete = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_work_autocomplete_fragment);

        // Retrieve user saved data
        SharedPreferences prefs = getSharedPreferences(PreferencesFileName, MODE_PRIVATE);
        String home = prefs.getString("home", null);
        String work = prefs.getString("work", null);
        String apiKey = prefs.getString("darkSkyApiKey", null);

        homeAutoComplete.setHint("Enter your home address");
        workAutoComplete.setHint("Enter your work address");

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

                //rewrite the marker information to ensure single instances of home/work
                for(int i = 0; i < markers.size(); i++){
                    Marker marker = markers.get(i);
                    if(marker.getTag() == "home"){
                        marker.remove();
                    }
                }

                Marker marker = mMap.addMarker(new MarkerOptions().position(t).title(place.getName().toString()).snippet(place.getId()));
                marker.setTag("home");

                markers.add(marker);
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

                for(int i = 0; i < markers.size(); i++){
                    Marker marker = markers.get(i);
                    if(marker.getTag() == "work"){
                        marker.remove();
                    }
                }

                Marker marker = mMap.addMarker(new MarkerOptions().position(t).title(place.getName().toString()).snippet(place.getId()));
                marker.setTag("work");

                markers.add(marker);
                fitBounds();
            }

            @Override
            public void onError(Status status) {
                Log.e(TAG, "onError: Status = " + status.toString());
            }
        });
    }

    public void fitBounds(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        int padding = 40; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cu);
    }

    public void saveData(View view){
        if(checkLocationPermission()){
            currentLocation = locationManager.getLastKnownLocation(provider);
        }

        SharedPreferences.Editor editor = getSharedPreferences(PreferencesFileName, MODE_PRIVATE).edit();
        EditText darkSkyApiKey;
        darkSkyApiKey = (EditText)findViewById(R.id.editDarkSkyApi);
        for (Marker marker : markers) {
            editor.putString(marker.getTag().toString(), marker.getSnippet().toString());
        }
        editor.putString("darkSkyApiKey", darkSkyApiKey.getText().toString());
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
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

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

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission. ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
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
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission. ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        locationManager.requestLocationUpdates(provider, 400, 1, this);
                    }

                } else {
                    // permission denied
                }
                return;
            }

        }
    }
}
