package com.example.calendar.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.Address;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class LocationUtils {
    private static FusedLocationProviderClient fusedLocationClient;

    private LocationUtils() {
        // Private constructor to prevent instantiation
    }

    public static void init(Context context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        }
    }

    public static CompletableFuture<Location> getCurrentLocation(Context context) {
        if (!hasLocationPermission(context)) {
            CompletableFuture<Location> future = new CompletableFuture<>();
            future.completeExceptionally(new SecurityException("Location permission not granted"));
            return future;
        }

        CompletableFuture<Location> future = new CompletableFuture<>();
        
        Task<Location> locationTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null);
        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                future.complete(location);
            } else {
                future.completeExceptionally(new Exception("Location not available"));
            }
        }).addOnFailureListener(e -> future.completeExceptionally(e));

        return future;
    }

    public static String getAddressFromLocation(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                List<String> addressParts = new ArrayList<>();
                
                // Add address components if they exist
                if (address.getThoroughfare() != null) addressParts.add(address.getThoroughfare());
                if (address.getSubLocality() != null) addressParts.add(address.getSubLocality());
                if (address.getLocality() != null) addressParts.add(address.getLocality());
                if (address.getAdminArea() != null) addressParts.add(address.getAdminArea());
                
                if (!addressParts.isEmpty()) {
                    return String.join(", ", addressParts);
                }
            }
        } catch (Exception e) {
            // Fall back to coordinates if geocoding fails
        }
        return String.format(Locale.getDefault(), "%f, %f", latitude, longitude);
    }

    private static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
} 