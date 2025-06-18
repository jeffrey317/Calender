package com.example.calendar.ui.meals;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.FileProvider;
import com.example.calendar.R;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealType;
import com.example.calendar.databinding.DialogAddEditMealBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.location.Geocoder;
import android.location.Location;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.app.Dialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.List;
import com.example.calendar.data.FoodNutritionService;
import com.google.android.material.button.MaterialButtonToggleGroup;
import android.os.Looper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import android.widget.EditText;
import android.widget.ImageView;
import com.example.calendar.data.FoodCalories;

public class AddEditMealDialog extends DialogFragment {
    private static final String TAG = "AddEditMealDialog";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private Meal existingMeal;
    private OnMealSaveListener listener;
    private OnMealDeleteListener deleteListener;
    private DialogAddEditMealBinding binding;
    private String currentPhotoPath;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private FoodNutritionService nutritionService;
    private MealType selectedMealType;

    public interface OnMealSaveListener {
        void onMealSave(Meal meal);
    }

    public interface OnMealDeleteListener {
        void onMealDelete(Meal meal);
    }

    public void setMeal(Meal meal) {
        this.existingMeal = meal;
        Log.d(TAG, "setMeal called: " + (meal != null ? "editing existing meal" : "creating new meal"));
    }

    public void setOnMealSaveListener(OnMealSaveListener listener) {
        this.listener = listener;
        Log.d(TAG, "setOnMealSaveListener called: " + (listener != null ? "listener set" : "listener cleared"));
    }

    public void setOnMealDeleteListener(OnMealDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
        Log.d(TAG, "setOnMealDeleteListener called: " + (deleteListener != null ? "listener set" : "listener cleared"));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_App_Dialog_AddEditMeal);
        
        nutritionService = new FoodNutritionService();
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(requireContext(), "Location permission is required to get current location", Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Initialize camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Initialize camera launcher
        takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (currentPhotoPath != null) {
                        loadImage(currentPhotoPath);
                    }
                }
            }
        );

        // Initialize image picker launcher
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        currentPhotoPath = copyImageToAppStorage(uri);
                        loadImage(currentPhotoPath);
                    } catch (IOException e) {
                        Log.e(TAG, "Error copying selected image", e);
                        Toast.makeText(requireContext(), "Failed to select image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        binding = DialogAddEditMealBinding.inflate(inflater, container, false);
        
        // Get references to views
        EditText foodContentEditText = binding.foodContentEditText;
        EditText locationEditText = binding.locationEditText;
        Chip breakfastButton = binding.breakfastButton;
        Chip lunchButton = binding.lunchButton;
        Chip dinnerButton = binding.dinnerButton;
        Chip otherButton = binding.otherButton;
        MaterialButton getLocationButton = binding.getLocationButton;
        MaterialButton takePhotoButton = binding.takePhotoButton;
        MaterialButton choosePhotoButton = binding.choosePhotoButton;
        MaterialButton saveButton = binding.saveButton;
        MaterialButton deleteButton = binding.deleteButton;
        ImageView mealImageView = binding.mealImageView;

        // Set up meal type selection
        breakfastButton.setOnClickListener(v -> {
            selectedMealType = MealType.BREAKFAST;
            updateMealTypeSelection();
        });

        lunchButton.setOnClickListener(v -> {
            selectedMealType = MealType.LUNCH;
            updateMealTypeSelection();
        });

        dinnerButton.setOnClickListener(v -> {
            selectedMealType = MealType.DINNER;
            updateMealTypeSelection();
        });

        otherButton.setOnClickListener(v -> {
            selectedMealType = MealType.SNACK;
            updateMealTypeSelection();
        });

        // Update meal type selection UI
        if (existingMeal != null) {
            selectedMealType = existingMeal.getMealType();
            updateMealTypeSelection();
        }

        // Setup image buttons
        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());
        choosePhotoButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Setup location button
        getLocationButton.setOnClickListener(v -> checkLocationPermission());

        // Load existing meal data if editing
        if (existingMeal != null) {
            loadExistingMealData();
            binding.dialogTitle.setText("Edit Meal");
            // Show delete button only when editing existing meal
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            // Hide delete button for new meals
            deleteButton.setVisibility(View.GONE);
        }

        // Setup save button
        saveButton.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Save button clicked");
                
                MealType mealType = getMealTypeFromButtons();
                Log.d(TAG, "Selected meal type: " + mealType);
                
                String location = locationEditText.getText().toString().trim();
                Log.d(TAG, "Location: " + location);

                String foodContent = foodContentEditText.getText().toString().trim();
                Log.d(TAG, "Food content: " + foodContent);

                // Prioritize API for accurate USDA data (same as AI chat)
                if (!foodContent.isEmpty()) {
                    // Show loading state
                    saveButton.setEnabled(false);
                    saveButton.setText("Calculating calories...");
                    
                    nutritionService.calculateCalories(foodContent, new FoodNutritionService.NutritionCallback() {
                        @Override
                        public void onResult(int apiCalories) {
                            requireActivity().runOnUiThread(() -> {
                                saveButton.setEnabled(true);
                                saveButton.setText("Save");
                                
                                Log.d(TAG, "Calculated calories from API: " + apiCalories);
                                
                                // If API returns 0, try local database as fallback
                                if (apiCalories == 0) {
                                    int localCalories = FoodCalories.getCalories(foodContent);
                                    Log.d(TAG, "API returned 0, using local fallback: " + localCalories);
                                    saveMealWithCalories(mealType, location, foodContent, localCalories);
                                } else {
                                    // Use API calories (USDA data - same as AI chat)
                                    saveMealWithCalories(mealType, location, foodContent, apiCalories);
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            requireActivity().runOnUiThread(() -> {
                                saveButton.setEnabled(true);
                                saveButton.setText("Save");
                                
                                Log.w(TAG, "API calorie calculation failed: " + error);
                                // Fall back to local database
                                int localCalories = FoodCalories.getCalories(foodContent);
                                Log.d(TAG, "Using local database fallback: " + localCalories);
                                saveMealWithCalories(mealType, location, foodContent, localCalories);
                            });
                        }
                    });
                    return; // Exit early, callback will handle saving
                }
                
                // If food content is empty, save with 0 calories
                saveMealWithCalories(mealType, location, foodContent, 0);
            } catch (Exception e) {
                Log.e(TAG, "Error saving meal", e);
                Toast.makeText(requireContext(), "Error saving meal", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup delete button
        deleteButton.setOnClickListener(v -> {
            if (existingMeal != null) {
                // Show confirmation dialog
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Meal")
                    .setMessage("Are you sure you want to delete this meal?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        try {
                            Log.d(TAG, "Delete button clicked for meal id: " + existingMeal.getId());
                            if (deleteListener != null) {
                                deleteListener.onMealDelete(existingMeal);
                            }
                            dismiss();
                        } catch (Exception e) {
                            Log.e(TAG, "Error deleting meal", e);
                            Toast.makeText(requireContext(), "Error deleting meal", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
        
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            window.setWindowAnimations(R.style.DialogAnimation);
            
            // Set dialog properties to prevent window leaks
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);
            
            // Clear any existing flags and set proper ones
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        } else {
            Log.e(TAG, "Dialog or window is null in onStart");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up resources
        if (binding != null) {
        binding = null;
        }
    }

    @Override
    public void dismiss() {
        try {
            // Ensure proper cleanup
            if (getDialog() != null && getDialog().isShowing()) {
                super.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing dialog", e);
            super.dismissAllowingStateLoss();
        }
    }

    private void updateMealTypeSelection() {
        binding.breakfastButton.setChecked(selectedMealType == MealType.BREAKFAST);
        binding.lunchButton.setChecked(selectedMealType == MealType.LUNCH);
        binding.dinnerButton.setChecked(selectedMealType == MealType.DINNER);
        binding.otherButton.setChecked(selectedMealType == MealType.SNACK);
    }

    private MealType getMealTypeFromButtons() {
        if (binding.breakfastButton.isChecked()) {
            return MealType.BREAKFAST;
        } else if (binding.lunchButton.isChecked()) {
            return MealType.LUNCH;
        } else if (binding.dinnerButton.isChecked()) {
            return MealType.DINNER;
        } else {
            return MealType.SNACK;
        }
    }

    private void loadExistingMealData() {
        if (existingMeal == null) return;
        
        // Set meal type
        selectedMealType = existingMeal.getMealType();
        updateMealTypeSelection();

        // Set food content
        binding.foodContentEditText.setText(existingMeal.getNotes());
        
        // Set location if available
        if (existingMeal.getLocation() != null) {
            binding.locationEditText.setText(existingMeal.getLocation());
        }

        // Load image if available
        if (existingMeal.getImagePath() != null) {
            currentPhotoPath = existingMeal.getImagePath();
            loadImage(currentPhotoPath);
        }
    }

    private void dispatchTakePictureIntent() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
            }

            if (photoFile != null) {
            try {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                    "app.login.fileprovider",
                    photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                
                // Grant URI permissions
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                
                // Launch camera
                takePictureLauncher.launch(takePictureIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error creating photo URI", e);
                Toast.makeText(requireContext(), "Error launching camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(requireContext().getExternalFilesDir(null), "meal_photos");
        
        // Create the storage directory if it does not exist
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                throw new IOException("Failed to create directory");
            }
        }
        
        // Create the file
        File image = new File(storageDir, imageFileName + ".jpg");
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private String copyImageToAppStorage(Uri sourceUri) throws IOException {
        InputStream in = requireContext().getContentResolver().openInputStream(sourceUri);
        File destinationFile = createImageFile();
        OutputStream out = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
            }
        in.close();
        out.close();
            return destinationFile.getAbsolutePath();
    }

    private void loadImage(String imagePath) {
        if (imagePath != null && binding != null) {
            binding.mealImageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(requireContext(), "Getting location...", Toast.LENGTH_SHORT).show();

        LocationRequest locationRequest = new LocationRequest.Builder(5000)
            .setMinUpdateIntervalMillis(3000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdates(1)
            .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(requireContext(), "Unable to get location. Please ensure GPS is enabled.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocationUI(location);
                }
                // Remove location updates after getting the location
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateLocationUI(Location location) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder locationText = new StringBuilder();
                
                String featureName = address.getFeatureName();
                String thoroughfare = address.getThoroughfare();
                String subLocality = address.getSubLocality();
                String locality = address.getLocality();
                
                if (locality != null) locationText.append(locality);
                if (subLocality != null) {
                    if (locationText.length() > 0) locationText.append(", ");
                    locationText.append(subLocality);
                }
                if (thoroughfare != null) {
                    if (locationText.length() > 0) locationText.append(", ");
                    locationText.append(thoroughfare);
                }
                if (featureName != null && !featureName.equals(thoroughfare)) {
                    if (locationText.length() > 0) locationText.append(" ");
                    locationText.append(featureName);
                }
                
                binding.locationEditText.setText(locationText.toString());
                
                if (existingMeal != null) {
                    existingMeal.setLatitude(location.getLatitude());
                    existingMeal.setLongitude(location.getLongitude());
                }
            } else {
                String coordinates = String.format(Locale.getDefault(), "%.6f, %.6f", 
                    location.getLatitude(), location.getLongitude());
                binding.locationEditText.setText(coordinates);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address", e);
            String coordinates = String.format(Locale.getDefault(), "%.6f, %.6f", 
                location.getLatitude(), location.getLongitude());
            binding.locationEditText.setText(coordinates);
            Toast.makeText(requireContext(), "Unable to resolve address, showing coordinates", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMealWithCalories(MealType mealType, String location, String foodContent, int calories) {
        try {
            Log.d(TAG, "Saving meal with calories: " + calories);
            
            Meal meal;
            if (existingMeal != null) {
                meal = existingMeal;
                meal.setMealType(mealType);
                meal.setImagePath(currentPhotoPath);
                meal.setLocation(location);
                meal.setNotes(foodContent);
                meal.setCalories(calories);
                Log.d(TAG, "Updating existing meal id: " + meal.getId());
            } else {
                // Get current user ID for new meal
                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                String userId = currentUser != null ? currentUser.getUid() : "";
                meal = new Meal(userId, new Date(), mealType, currentPhotoPath, location, null, null, foodContent, calories);
                Log.d(TAG, "Creating new meal for date: " + new Date());
            }

            if (listener != null) {
                listener.onMealSave(meal);
            }
            dismiss();
        } catch (Exception e) {
            Log.e(TAG, "Error saving meal", e);
            Toast.makeText(requireContext(), "Error saving meal", Toast.LENGTH_SHORT).show();
        }
    }
} 