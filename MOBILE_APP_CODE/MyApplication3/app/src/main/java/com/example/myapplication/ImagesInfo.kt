package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImagesInfo : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private var currentPhotoPath: String = ""
    private lateinit var resultCameraImage: ActivityResultLauncher<Intent>
    private lateinit var resultGalleryImage: ActivityResultLauncher<Intent>
    private lateinit var requestPermission: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.report_issue)

        imageView = findViewById(R.id.imageView)
        val cameraButton: Button = findViewById(R.id.take_photo)
        val galleryButton: Button = findViewById(R.id.gallery_choice)
//        continueButton = findViewById(R.id.continue_button)

        // --- Result Launchers --- //
        resultCameraImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageView.setImageURI(imageUri)
                continueButton.isEnabled = true
            }
        }

        resultGalleryImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                imageUri = result.data?.data
                imageView.setImageURI(imageUri)
                continueButton.isEnabled = true
            }
        }

        requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Button Listeners --- //
        cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestPermission.launch(Manifest.permission.CAMERA)
            }
        }

        galleryButton.setOnClickListener {
            launchGallery()
        }

//        continueButton.setOnClickListener {
//            val intent = Intent(this@ImagesInfo, ComplaintUI::class.java)
//            imageUri?.let {
//                intent.putExtra("imageUri", it.toString())
//                startActivity(intent)
//            }
//        }
    }

    private fun launchCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("CameraError", "Error occurred while creating the File", ex)
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            return
        }

        val photoURI: Uri = FileProvider.getUriForFile(
            this,
            "com.example.yourapp.provider", // Change this to your actual FileProvider authority
            photoFile
        )
        imageUri = photoURI

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        resultCameraImage.launch(cameraIntent)
    }

    private fun launchGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultGalleryImage.launch(galleryIntent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }
}
