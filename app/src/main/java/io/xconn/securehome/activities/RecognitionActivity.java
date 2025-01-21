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
import io.xconn.securehome.utils.FaceRecognitionHelper;
import io.xconn.securehome.api.response.RecognitionResponse;

public class RecognitionActivity extends AppCompatActivity {
    private ImageCaptureHelper imageCaptureHelper;
    private FaceRecognitionHelper faceRecognitionHelper;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        ImageView imageView = findViewById(R.id.imageView2);
        imageCaptureHelper = new ImageCaptureHelper(this, imageView);
        faceRecognitionHelper = new FaceRecognitionHelper(this);

        // Create a custom progress dialog with a ProgressBar
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressDialog = new AlertDialog.Builder(this)
                .setTitle("Processing image...")
                .setView(progressBar)
                .setCancelable(false)
                .create();

        findViewById(R.id.gallerycard).setOnClickListener(v ->
                imageCaptureHelper.openGallery()
        );

        findViewById(R.id.cameracard).setOnClickListener(v ->
                imageCaptureHelper.checkPermissionsAndOpenCamera()
        );

        findViewById(R.id.buttonrecognize).setOnClickListener(v ->
                performFaceRecognition()
        );
    }

    private void performFaceRecognition() {
        Uri imageUri = imageCaptureHelper.getImageUri();
        String photoPath = imageCaptureHelper.getCurrentPhotoPath();

        if (imageUri == null && photoPath == null) {
            Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        faceRecognitionHelper.recognizeFace(imageUri, photoPath, new FaceRecognitionHelper.RecognitionCallback() {
            @Override
            public void onSuccess(RecognitionResponse response) {
                progressDialog.dismiss();
                if (response.isRecognized()) {
                    String message = "Welcome " + response.getUserName() +
                            "\nConfidence: " + response.getConfidence() + "%";
                    showResultDialog("Recognition Successful", message);
                } else {
                    showResultDialog("Recognition Failed", response.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(RecognitionActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
