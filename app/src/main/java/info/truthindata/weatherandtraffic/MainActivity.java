package info.truthindata.weatherandtraffic;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

import info.truthindata.weatherandtraffic.utils.DarkSkyApi;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PreferencesFileName = "wayhome";
    private static String mDarkSkyApiKey = null;
    private FusedLocationProviderClient mFusedLocationClient;
    protected List<Marker> markers = new ArrayList<>();

    private JsonObject GetDarkSkyForecast(){
        JsonObject result = new JsonObject();

        try{
            Location location = mFusedLocationClient.getLastLocation().getResult();

            result = new DarkSkyApi().GetCurrentForecast(location, mDarkSkyApiKey);
        } catch(SecurityException e){
            Log.e(TAG, e.getMessage());
        }

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

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

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Retrieve user saved data
        SharedPreferences prefs = getSharedPreferences(PreferencesFileName, MODE_PRIVATE);
        String home = prefs.getString("home", null);
        String work = prefs.getString("work", null);
        String apiKey = prefs.getString("darkSkyApiKey", null);

        homeAutoComplete.setHint("Enter your home address");
        workAutoComplete.setHint("Enter your work address");

        if(apiKey != null){
            EditText darkSkyApiKey;
            darkSkyApiKey = (EditText)findViewById(R.id.editDarkSkyApi);
            darkSkyApiKey.setText(apiKey);
            mDarkSkyApiKey = apiKey;

            JsonObject forecast = GetDarkSkyForecast();
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
}
