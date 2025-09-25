package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImagesInfo extends AppCompatActivity {
    private ImageView imageView;
    private String currentPhotoPath = "";
    private ActivityResultLauncher<Intent> resultCameraImage;
    private ActivityResultLauncher<Intent> resultGalleryImage;
    private ActivityResultLauncher<String> requestPermission;
    private Uri imageUri;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_issue);

        imageView = findViewById(R.id.imageView);
        Button cameraButton = findViewById(R.id.take_photo);
        Button galleryButton = findViewById(R.id.gallery_choice);
        continueButton = findViewById(R.id.continue_button);

        // --- Result Launchers --- //
        // Handles the result from the camera
        resultCameraImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // The image was saved to this.imageUri. We can use it directly.
                        imageView.setImageURI(this.imageUri);
                        continueButton.setEnabled(true);
                    }
                });

        // Handles the result from the gallery
        resultGalleryImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        imageView.setImageURI(imageUri); //-> display the selected photo
                        // We will use this URI to get the image data for JSON
                        continueButton.setEnabled(true);
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
            // Check for camera permission & launches
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                // Request permission before launching
                requestPermission.launch(Manifest.permission.CAMERA);
            }
        });

        galleryButton.setOnClickListener(v -> launchGallery());
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(ImagesInfo.this, ComplaintUI.class);
            if (imageUri != null) {
                intent.putExtra("imageUri", imageUri.toString()); // Pass URI as a String
                startActivity(intent);
            }
        });
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the file and get its URI
        File photoFile;
        try {
            photoFile = createImageFile(); // A helper method to create a temp file
        } catch (IOException ex) {
            Log.e("CameraError", "Error occurred while creating the File", ex);
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri photoURI = FileProvider.getUriForFile(this,
                "com.example.yourapp.provider", // Change to your authority
                photoFile);
        this.imageUri = photoURI;

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        resultCameraImage.launch(cameraIntent);

    }

    private void launchGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        resultGalleryImage.launch(galleryIntent);
    }

    private File createImageFile() throws IOException {
        // 1. Create a unique filename using a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // 2. Get the app's private picture storage directory
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // 3. Create the temporary file in that directory
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}