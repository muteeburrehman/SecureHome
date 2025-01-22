package io.xconn.securehome.activities;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import io.xconn.securehome.R;
import io.xconn.securehome.utils.ImageCaptureHelper;
import io.xconn.securehome.utils.ImageUploadHelper;
import io.xconn.securehome.utils.VideoCaptureHelper;
import io.xconn.securehome.utils.VideoUploadHelper;

public class RegisterActivity extends AppCompatActivity {
    private ImageCaptureHelper imageCaptureHelper;
    private VideoCaptureHelper videoCaptureHelper;
    private ImageUploadHelper imageUploadHelper;
    private VideoUploadHelper videoUploadHelper;
    private AlertDialog progressDialog;
    private boolean isVideoMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ImageView imageView = findViewById(R.id.imageView2);
        VideoView videoView = findViewById(R.id.videoView);

        imageCaptureHelper = new ImageCaptureHelper(this, imageView);
        videoCaptureHelper = new VideoCaptureHelper(this, videoView);
        imageUploadHelper = new ImageUploadHelper(this);
        videoUploadHelper = new VideoUploadHelper(this);

        findViewById(R.id.videocard).setOnClickListener(v -> {
            isVideoMode = true;
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            showVideoSourceDialog();
        });

        findViewById(R.id.gallerycard).setOnClickListener(v -> {
            isVideoMode = false;
            imageView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            imageCaptureHelper.openGallery();
        });

        findViewById(R.id.cameracard).setOnClickListener(v -> {
            if (isVideoMode) {
                videoCaptureHelper.checkPermissionsAndOpenVideoCamera();
            } else {
                imageCaptureHelper.checkPermissionsAndOpenCamera();
            }
        });

        findViewById(R.id.uploadButton).setOnClickListener(v -> {
            if (isVideoMode) {
                handleVideoUpload();
            } else {
                handleImageUpload();
            }
        });
    }

    private void showVideoSourceDialog() {
        String[] options = {"Record Video", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Video Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        videoCaptureHelper.checkPermissionsAndOpenVideoCamera();
                    } else {
                        videoCaptureHelper.openVideoGallery();
                    }
                })
                .show();
    }

    private void handleVideoUpload() {
        Uri videoUri = videoCaptureHelper.getVideoUri();
        String videoPath = videoCaptureHelper.getCurrentVideoPath();

        if (videoUri == null && videoPath == null) {
            Toast.makeText(this, "Please select or record a video first", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Uploading video...");

        videoUploadHelper.uploadVideo(videoUri, videoPath, new VideoUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(String message) {
                dismissProgressDialog();
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                dismissProgressDialog();
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleImageUpload() {
        Uri imageUri = imageCaptureHelper.getImageUri();
        String photoPath = imageCaptureHelper.getCurrentPhotoPath();

        if (imageUri == null && photoPath == null) {
            Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Uploading image...");

        imageUploadHelper.uploadImage(imageUri, photoPath, new ImageUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(String message) {
                dismissProgressDialog();
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                dismissProgressDialog();
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgressDialog(String message) {
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(message)
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