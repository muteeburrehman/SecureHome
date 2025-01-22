package io.xconn.securehome.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoCaptureHelper {
    private final AppCompatActivity activity;
    private Uri videoUri;
    private String currentVideoPath;
    private final ActivityResultLauncher<Intent> galleryVideoLauncher;
    private final ActivityResultLauncher<Intent> cameraVideoLauncher;
    private final ActivityResultLauncher<String[]> permissionLauncher;

    public VideoCaptureHelper(AppCompatActivity activity, VideoView videoView) {
        this.activity = activity;

        galleryVideoLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        videoUri = result.getData().getData();
                        videoView.setVideoURI(videoUri);
                        videoView.start();
                    }
                });

        cameraVideoLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        videoView.setVideoURI(videoUri);
                        videoView.start();
                    }
                });

        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = permissions.values().stream().allMatch(Boolean::booleanValue);
                    if (allGranted) {
                        openVideoCamera();
                    } else {
                        showToast("Permissions are required to use the camera");
                    }
                });
    }

    public void openVideoGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        galleryVideoLauncher.launch(intent);
    }

    public void checkPermissionsAndOpenVideoCamera() {
        String[] permissions = {Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }

        boolean permissionsNeeded = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED) {
                permissionsNeeded = true;
                break;
            }
        }

        if (permissionsNeeded) {
            permissionLauncher.launch(permissions);
        } else {
            openVideoCamera();
        }
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = "VID_" + timeStamp;
        File storageDir = new File(activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SecureHome");

        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + storageDir.getAbsolutePath());
        }

        File videoFile = new File(storageDir, videoFileName + ".mp4");
        currentVideoPath = videoFile.getAbsolutePath();
        return videoFile;
    }

    private void openVideoCamera() {
        try {
            File videoFile = createVideoFile();
            videoUri = FileProvider.getUriForFile(activity,
                    activity.getApplicationContext().getPackageName() + ".provider",
                    videoFile);

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30); // 30 seconds max
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // High quality
            cameraVideoLauncher.launch(intent);
        } catch (IOException e) {
            showToast("Error creating video file");
        }
    }

    public Uri getVideoUri() {
        return videoUri;
    }

    public String getCurrentVideoPath() {
        return currentVideoPath;
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}