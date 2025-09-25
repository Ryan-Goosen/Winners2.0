package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;



public class MainActivity_java extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout); //-> linking to app_layout.xml

        Button reportIssueButton = findViewById(R.id.report_button); //-> finding the report button in the layout
        System.out.println(reportIssueButton);


        ImageView myImageView = findViewById(R.id.imageView2); //-> finding the image view in the layout

        myImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d("ImageTap", "Image was tapped!");
            }
        }
        );

        reportIssueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent switchToImagesInfo = new Intent(MainActivity_java.this, ImagesInfo.class);
                startActivity(switchToImagesInfo);
            }
        });

    }
}