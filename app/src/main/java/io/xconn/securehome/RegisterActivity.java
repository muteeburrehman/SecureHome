package io.xconn.securehome;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import io.xconn.securehome.utils.ImageCaptureHelper;

public class RegisterActivity extends AppCompatActivity {
    private ImageCaptureHelper imageCaptureHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ImageView imageView = findViewById(R.id.imageView2);
        imageCaptureHelper = new ImageCaptureHelper(this, imageView);

        findViewById(R.id.gallerycard).setOnClickListener(v -> imageCaptureHelper.openGallery());
        findViewById(R.id.cameracard).setOnClickListener(v -> imageCaptureHelper.checkPermissionsAndOpenCamera());
    }

    // If you need the image URI or path for registration purposes, you can get them like this:
    private void handleImageForRegistration() {
        Uri imageUri = imageCaptureHelper.getImageUri();
        String photoPath = imageCaptureHelper.getCurrentPhotoPath();
        // Process the image for registration...
    }
}