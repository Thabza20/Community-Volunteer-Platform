package com.error404.communityvolunteerplatform.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {

    public interface OnLocationDetectedListener {
        void onLocationDetected(String locationName);
        void onError(String error);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void detectLocation(OnLocationDetectedListener listener) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                processLocation(location, listener);
            } else {
                requestNewLocation(listener);
            }
        }).addOnFailureListener(e -> requestNewLocation(listener));
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocation(OnLocationDetectedListener listener) {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    processLocation(location, listener);
                } else {
                    listener.onError("Could not get location");
                }
                stopUpdates();
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void processLocation(Location location, OnLocationDetectedListener listener) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String result = formatAddress(address);
                if (result.isEmpty()) {
                    listener.onLocationDetected(location.getLatitude() + ", " + location.getLongitude());
                } else {
                    listener.onLocationDetected(result);
                }
            } else {
                listener.onLocationDetected(location.getLatitude() + ", " + location.getLongitude());
            }
        } catch (IOException e) {
            listener.onError("Geocoder service unavailable: " + e.getMessage());
        }
    }

    private String formatAddress(Address address) {
        String city = address.getLocality();
        String province = address.getAdminArea();
        String subAdmin = address.getSubAdminArea();
        String country = address.getCountryName();

        if (city != null && province != null) return city + ", " + province;
        if (subAdmin != null && province != null) return subAdmin + ", " + province;
        if (province != null && country != null) return province + ", " + country;
        if (province != null) return province;
        if (city != null) return city;
        
        return "";
    }

    public void stopUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}