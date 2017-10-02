package com.destiny.sta.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.destiny.sta.R;
import com.destiny.sta.model.LoginResponse;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * Created by Bobo on 8/30/2017.
 */

public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = MapFragment.class.getSimpleName();

    public static final int REQUEST_CHECK_SETTINGS = 1;

    public static final int PERMISSIONS_REQUEST_LOCATION = 1;

    private static final int MAX_DISTANCE = 20;

    public interface LocationCallback {
        void onLocationUpdated(Location location);
    }

    private LocationCallback locationCallback;
    private LoginFragment.LoginCallback loginCallback;

    private Button submitButton;
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LocationCallback) {
            locationCallback = (LocationCallback) context;
        } else {
            throw new IllegalArgumentException("Activity must implement LocationCallback!");
        }

        if (context instanceof LoginFragment.LoginCallback) {
            loginCallback = (LoginFragment.LoginCallback) context;
        } else {
            throw new IllegalArgumentException("Activity must implement LoginCallback!");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.find_location);
        setHasOptionsMenu(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        submitButton = view.findViewById(R.id.submitButton);
        submitButton.setEnabled(false);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationCallback.onLocationUpdated(currentLocation);

                Fragment pictureFragment = new PictureFragment();
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, pictureFragment)
                        .commit();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);

        checkLocationSettings();
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(getActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    task.getResult(ApiException.class);

                    getCurrentLocation();
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                /*resolvable.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);*/

                                startIntentSenderForResult(resolvable.getResolution().getIntentSender(), REQUEST_CHECK_SETTINGS, null, 0, 0, 0, null);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            if (currentLocation != null) {
                                if (currentLocation.distanceTo(location) > 5) {
                                    googleMap.clear();
                                } else {
                                    return;
                                }
                            }
                            currentLocation = location;

                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here!"));
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18.0f)); //16.0f

                            LoginResponse user = loginCallback.getUser();
                            Double branchLatitude = user.getBranch().getLatitude();
                            Double branchLongitude = user.getBranch().getLongitude();
//                            Double branchLatitude = 13.598178;
//                            Double branchLongitude = 100.326855;
                            Location branchLocation = new Location("branch");
                            branchLocation.setLatitude(branchLatitude);
                            branchLocation.setLongitude(branchLongitude);
                            Log.v(TAG, "branch latitude: " + branchLatitude + " longitude: " + branchLongitude);

                            Circle circle = googleMap.addCircle(new CircleOptions()
                                    .center(new LatLng(branchLatitude, branchLongitude))
                                    .radius(MAX_DISTANCE)
                                    .strokeColor(Color.TRANSPARENT)
                                    .fillColor(Color.GRAY));

                            PolylineOptions line =
                                    new PolylineOptions().add(currentLatLng,
                                            new LatLng(branchLatitude, branchLongitude))
                                            .width(3).color(Color.RED);

                            googleMap.addPolyline(line);

//                            float[] results = new float[1];
//                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
//                                    branchLatitude, branchLongitude, results);
                            float distance = location.distanceTo(branchLocation);
                            Log.v(TAG, "distance: " + distance);

                            if (distance <= MAX_DISTANCE) {
                                if (!submitButton.isEnabled()) submitButton.setEnabled(true);
                            } else {
                                if (submitButton.isEnabled()) submitButton.setEnabled(false);
                            }
//                            if (!submitButton.isEnabled()) submitButton.setEnabled(true);
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // All required changes were successfully made
                    getCurrentLocation();
                    break;
                case Activity.RESULT_CANCELED:
                    // The user was asked to change settings, but chose not to
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                checkLocationSettings();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
