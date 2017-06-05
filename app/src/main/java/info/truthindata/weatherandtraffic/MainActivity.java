package info.truthindata.weatherandtraffic;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback  {
    private GoogleMap mMap;
    private static final String TAG = "MainActivity";
    protected List<Marker> markers = new ArrayList<>();

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

        homeAutoComplete.setHint("Enter your home address");
        workAutoComplete.setHint("Enter your work address");

        homeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng t = place.getLatLng();
                Log.i(TAG, "Place Selected: " + place.getName());

                Marker marker = mMap.addMarker(new MarkerOptions().position(t).title(place.getName().toString()));
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

                Marker marker = mMap.addMarker(new MarkerOptions().position(t).title(place.getName().toString()));
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

        int padding = 20; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cu);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng defaultCoords = new LatLng(40.1623518,-74.9563831);
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultCoords));
    }
}
