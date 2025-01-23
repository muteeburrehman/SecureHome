package io.xconn.securehome.activities;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import io.xconn.securehome.R;
import io.xconn.securehome.utils.ImageCaptureHelper;
import io.xconn.securehome.utils.ImageUploadHelper;

public class RegisterActivity extends AppCompatActivity {
    private ImageCaptureHelper imageCaptureHelper;
    private ImageUploadHelper imageUploadHelper;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ImageView imageView = findViewById(R.id.imageView2);
        imageCaptureHelper = new ImageCaptureHelper(this, imageView);
        imageUploadHelper = new ImageUploadHelper(this);

        findViewById(R.id.gallerycard).setOnClickListener(v ->
                imageCaptureHelper.openGallery()
        );

        findViewById(R.id.cameracard).setOnClickListener(v ->
                imageCaptureHelper.checkPermissionsAndOpenCamera()
        );

        findViewById(R.id.uploadButton).setOnClickListener(v ->
                handleImageUpload()
        );
    }

    private void handleImageUpload() {
        Uri imageUri = imageCaptureHelper.getImageUri();
        String photoPath = imageCaptureHelper.getCurrentPhotoPath();

        if (imageUri == null && photoPath == null) {
            Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog(); // Show the progress dialog

        imageUploadHelper.uploadImage(imageUri, photoPath, new ImageUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(String message) {
                dismissProgressDialog(); // Dismiss the progress dialog
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                dismissProgressDialog(); // Dismiss the progress dialog
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgressDialog() {
        // Create a custom AlertDialog with a ProgressBar
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Uploading image...")
                .setView(progressBar)
                .setCancelable(false);

        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
