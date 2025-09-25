package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComplaintUI extends AppCompatActivity {

    // UI Components
    private ImageView finalImageView;
    private TextInputEditText titleEditText, descriptionEditText, addressEditText, timeEditText;
    private Spinner prioritySpinner;
    private Button sumbitButton;

    // Data holders
    private Uri imageUri;
    private FusedLocationProviderClient fusedLocationClient;

    // Launcher for Location Permissions
    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                        Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION))) {
                    // Permission is granted. Get the location.
                    Log.d("LocationPermission", "Permission Granted");
                    fetchLocation();
                } else {
                    // Permission is denied.
                    Log.d("LocationPermission", "Permission Denied");
                    addressEditText.setText("Location permission denied");
                    Toast.makeText(this, "Location permission is required to get the address.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_process);

        // Initialize UI components
        finalImageView = findViewById(R.id.final_image_view);
        titleEditText = findViewById(R.id.edit_text_title);
        descriptionEditText = findViewById(R.id.edit_text_description);
        addressEditText = findViewById(R.id.edit_text_address);
        timeEditText = findViewById(R.id.edit_text_time);
        prioritySpinner = findViewById(R.id.spinner_priority);
        sumbitButton = findViewById(R.id.button_process_json);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // --- Step 1: Receive and display the image ---
        handleReceivedImage();

        // --- Step 2: Set up the Priority Spinner ---
        setupPrioritySpinner();

        // --- Step 3: Set the current time ---
        setCurrentTime();

        // --- Step 4: Get Location and Address ---
        checkLocationPermissionAndFetch();

        // Make address field clickable to re-fetch location
        addressEditText.setOnClickListener(v -> checkLocationPermissionAndFetch());

        // --- Step 5: Set up the final process button ---
        sumbitButton.setOnClickListener(view -> {
            // This is where you'll create the JSON and send to the server
            createAndProcessJson();
        });
    }

    private void handleReceivedImage() {
        Intent intent = getIntent();
        String uriString = intent.getStringExtra("imageUri"); // Get URI as a String
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            finalImageView.setImageURI(imageUri);
            Log.d("ImageReceive", "Successfully received and set image URI: " + imageUri.toString());
        } else {
            Log.e("ImageReceive", "Did not receive an image URI in the intent.");
            Toast.makeText(this, "Could not load image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPrioritySpinner() {
        String[] priorities = new String[]{"High", "Medium", "Low"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities);
        prioritySpinner.setAdapter(adapter);
    }

    private void setCurrentTime() {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        timeEditText.setText(currentTime);
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            fetchLocation();
        } else {
            // Permission is not granted, request it
            requestLocationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchLocation() {
        // Double-check permission before using the location client (required by Android)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Should not happen if checkLocationPermissionAndFetch is used correctly
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Location found, now geocode it to an address
                        getAddressFromLocation(location);
                    } else {
                        addressEditText.setText("Could not get location. Turn on GPS.");
                        Log.w("LocationFetch", "FusedLocationClient returned null location.");
                    }
                })
                .addOnFailureListener(this, e -> {
                    addressEditText.setText("Location fetch failed.");
                    Log.e("LocationFetch", "Failed to get location.", e);
                });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Build a readable address string
                String addressString = address.getAddressLine(0); // e.g., "1600 Amphitheatre Parkway, Mountain View, CA"
                addressEditText.setText(addressString);
            } else {
                addressEditText.setText("No address found for location.");
            }
        } catch (IOException e) {
            Log.e("Geocoder", "Service not available", e);
            addressEditText.setText("Could not connect to Geocoder service.");
        }
    }

    private void createAndProcessJson() {
        // --- Get all data from UI fields ---
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String time = timeEditText.getText().toString().trim();
        String priority = prioritySpinner.getSelectedItem().toString();

        // Validate input
        if (title.isEmpty() || description.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Title, description, and image are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert image to Base64 String
        String imageBase64 = convertImageUriToString(imageUri);
        if (imageBase64 == null) {
            Toast.makeText(this, "Failed to process the image.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Create JSON Object ---
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("title", title);
            jsonObject.put("description", description);
            jsonObject.put("address", address);
            jsonObject.put("timestamp", time);
            jsonObject.put("priority", priority);
            jsonObject.put("imageData", imageBase64); // The image data as a string
        } catch (JSONException e) {
            Log.e("JSONError", "Error creating JSON object", e);
            return;
        }

        // --- For now, we just log the JSON object ---
        // In a real app, you would send this string to your server using a library like Retrofit or Volley.
        String jsonString = jsonObject.toString();
        Log.d("JSONOutput", jsonString);

        Toast.makeText(this, "Data prepared for sending!", Toast.LENGTH_LONG).show();
        // Here you would call your server sending method, e.g.,
        // sendJsonToServer(jsonString);
    }

    private String convertImageUriToString(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] inputData = getBytes(inputStream);
            return Base64.encodeToString(inputData, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e("ImageConversion", "Error converting URI to Base64 String", e);
            return null;
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
