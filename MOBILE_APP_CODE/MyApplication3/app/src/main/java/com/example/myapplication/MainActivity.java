package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;



public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout); //-> linking to app_layout.xml

        Button reportIssueButton = findViewById(R.id.report_button); //-> finding the report button in the layout

        reportIssueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent switchToImagesInfo = new Intent(MainActivity.this, ImagesInfo.class);
                startActivity(switchToImagesInfo);
            }
        });
    }
}