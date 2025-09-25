package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ImagesInfo extends AppCompatActivity {
    private ImageView imageView;
    private ActivityResultLauncher<Intent> launchCamera;
    private ActivityResultLauncher<Intent> launchGallery;
    private ActivityResultLauncher<String> requestPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_collection);

        imageView = findViewById(R.id.imageView);
        Button cameraButton = findViewById(R.id.button_camera);
        Button galleryButton = findViewById(R.id.button_gallery);

        // --- Result Launchers --- //
        // Handles the result from the camera
        launchCamera = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        imageView.setImageBitmap(imageBitmap); //-> displays the bitmap captured
                    }
                });

        // Handles the result from the gallery
        launchGallery = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        imageView.setImageURI(imageUri); //-> display the selected photo
                        // Later, we will use this URI to get the image data for JSON
                    }
                });

        // Handles the permission request result
        requestPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, launch the camera
                        launchCamera();
                    } else {
                        // Permission denied
                        Toast.makeText(this, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show();
                    }
                });

        // --- Button OnClick Listeners ---
        cameraButton.setOnClickListener(v -> {
            // Check for camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        galleryButton.setOnClickListener(v -> launchGallery());
    }
    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void launchGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }



}