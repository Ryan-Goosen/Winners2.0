package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ComplaintUI : AppCompatActivity() {

    // UI Components
    private lateinit var finalImageView: ImageView
    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var addressEditText: TextInputEditText
    private lateinit var timeEditText: TextInputEditText
    private lateinit var prioritySpinner: Spinner
    private lateinit var submitButton: Button

    // Data holders
    private var imageUri: Uri? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Launcher for Location Permissions
    private val requestLocationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                // Permission is granted. Get the location.
                Log.d("LocationPermission", "Permission Granted")
                fetchLocation()
            } else {
                // Permission is denied.
                Log.d("LocationPermission", "Permission Denied")
                addressEditText.setText("Location permission denied")
                Toast.makeText(
                    this,
                    "Location permission is required to get the address.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_report)

        // Initialize UI components
        finalImageView = findViewById(R.id.image_pothole)        // matches android:id="@+id/image_pothole"
        titleEditText = findViewById(R.id.et_description)        // No exact title field, so using description EditText as example (change if needed)
        descriptionEditText = findViewById(R.id.et_description)  // matches android:id="@+id/et_description"
//        addressEditText = findViewById(R.id.edit_text_address)   // You don’t have this id in XML, you may need to add it or change accordingly
//        timeEditText = findViewById(R.id.edit_text_time)         // Not present in XML, add or remove accordingly
        prioritySpinner = findViewById(R.id.spinner_importance)  // matches android:id="@+id/spinner_importance"
        submitButton = findViewById(R.id.btn_submit)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // --- Step 1: Receive and display the image ---
        handleReceivedImage()

        // --- Step 2: Set up the Priority Spinner ---
        setupPrioritySpinner()

        // --- Step 3: Set the current time ---
        setCurrentTime()

        // --- Step 4: Get Location and Address ---
        checkLocationPermissionAndFetch()

        // Make address field clickable to re-fetch location
        addressEditText.setOnClickListener { checkLocationPermissionAndFetch() }

        // --- Step 5: Set up the final process button ---
        submitButton.setOnClickListener {
            // This is where you'll create the JSON and send to the server
            createAndProcessJson()
        }
    }

    private fun handleReceivedImage() {
        val intent = intent
        val uriString = intent.getStringExtra("imageUri")
        if (uriString != null) {
            imageUri = Uri.parse(uriString)
            finalImageView.setImageURI(imageUri)
            Log.d("ImageReceive", "Successfully received and set image URI: $imageUri")
        } else {
            Log.e("ImageReceive", "Did not receive an image URI in the intent.")
            Toast.makeText(this, "Could not load image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPrioritySpinner() {
        val priorities = arrayOf("High", "Medium", "Low")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)
        prioritySpinner.adapter = adapter
    }

    private fun setCurrentTime() {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        timeEditText.setText(currentTime)
    }

    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is already granted
            fetchLocation()
        } else {
            // Permission is not granted, request it
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission check failed, return early
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener(this) { location: Location? ->
                if (location != null) {
                    getAddressFromLocation(location)
                } else {
                    addressEditText.setText("Could not get location. Turn on GPS.")
                    Log.w("LocationFetch", "FusedLocationClient returned null location.")
                }
            }
            .addOnFailureListener(this) { e ->
                addressEditText.setText("Location fetch failed.")
                Log.e("LocationFetch", "Failed to get location.", e)
            }
    }

    private fun getAddressFromLocation(location: Location) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressString = address.getAddressLine(0)
                addressEditText.setText(addressString)
            } else {
                addressEditText.setText("No address found for location.")
            }
        } catch (e: IOException) {
            Log.e("Geocoder", "Service not available", e)
            addressEditText.setText("Could not connect to Geocoder service.")
        }
    }

    private fun createAndProcessJson() {
        val title = titleEditText.text?.toString()?.trim() ?: ""
        val description = descriptionEditText.text?.toString()?.trim() ?: ""
        val address = addressEditText.text?.toString()?.trim() ?: ""
        val time = timeEditText.text?.toString()?.trim() ?: ""
        val priority = prioritySpinner.selectedItem.toString()

        if (title.isEmpty() || description.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Title, description, and image are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = imageUri?.let { convertImageUriToString(it) }
        if (imageBase64 == null) {
            Toast.makeText(this, "Failed to process the image.", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject()
        try {
            jsonObject.put("title", title)
            jsonObject.put("description", description)
            jsonObject.put("address", address)
            jsonObject.put("timestamp", time)
            jsonObject.put("priority", priority)
            jsonObject.put("imageData", imageBase64)
        } catch (e: JSONException) {
            Log.e("JSONError", "Error creating JSON object", e)
            return
        }

        val jsonString = jsonObject.toString()
        Log.d("JSONOutput", jsonString)

        Toast.makeText(this, "Data prepared for sending!", Toast.LENGTH_LONG).show()

        // TODO: Send jsonString to your server
    }

    private fun convertImageUriToString(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val inputData = inputStream?.use { getBytes(it) }
            Base64.encodeToString(inputData, Base64.DEFAULT)
        } catch (e: IOException) {
            Log.e("ImageConversion", "Error converting URI to Base64 String", e)
            null
        }
    }

    @Throws(IOException::class)
    private fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }
}
