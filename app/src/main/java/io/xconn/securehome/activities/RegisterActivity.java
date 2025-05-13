package io.xconn.securehome.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.SelectedImagesAdapter;
import io.xconn.securehome.utils.ImageCaptureHelper;
import io.xconn.securehome.utils.ImageUploadHelper;
import io.xconn.securehome.utils.NetworkChangeReceiver;
import io.xconn.securehome.utils.ServerCheckUtility;

public class RegisterActivity extends AppCompatActivity implements
        SelectedImagesAdapter.OnImageRemoveListener,
        NetworkChangeReceiver.NetworkChangeListener {

    // Changed required count to 1
    private static final int REQUIRED_PHOTO_COUNT = 1;
    private static final int MAX_PHOTO_COUNT = 1;

    private ImageCaptureHelper imageCaptureHelper;
    private ImageUploadHelper imageUploadHelper;
    private SelectedImagesAdapter selectedImagesAdapter;
    private TextView progressText;
    private ExtendedFloatingActionButton uploadButton;
    private AlertDialog uploadDialog;
    private LinearProgressIndicator uploadProgress;
    private RecyclerView recyclerView;
    private CardView selectedPhotoContainer;
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean isNetworkCheckPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Setup network change receiver
        networkChangeReceiver = new NetworkChangeReceiver(this, this);

        // Use lifecycle observers if available
        try {
            ProcessLifecycleOwner.get().getLifecycle().addObserver(networkChangeReceiver);
        } catch (NoClassDefFoundError e) {
            // Fallback - manually register the receiver
            networkChangeReceiver.register();
        }

        // Check if server is configured first, before initializing anything else
        if (!ServerCheckUtility.checkServerConfigured(this)) {
            // The activity will be in a paused state, waiting for the user
            // to return from ServerDiscoveryActivity
            isNetworkCheckPending = true;
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupImageHelpers();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If we're returning from ServerDiscoveryActivity, check again
        if (isNetworkCheckPending) {
            isNetworkCheckPending = false;
            // Check if server is now configured
            if (ServerCheckUtility.checkServerConfigured(this)) {
                // Now initialize the activity
                initializeViews();
                setupRecyclerView();
                setupImageHelpers();
                setupClickListeners();
            } else {
                // Still not configured, we'll wait
                isNetworkCheckPending = true;
            }
        }
    }

    private void initializeViews() {
        progressText = findViewById(R.id.tv_progress);
        uploadButton = findViewById(R.id.fab_upload);
        uploadButton.setEnabled(false);
        selectedPhotoContainer = findViewById(R.id.selected_photo_container);
        updateProgressText(0);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_selected_images);

        // Set fixed size for better performance
        recyclerView.setHasFixedSize(true);

        // Configure layout manager - single column for one image
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);

        recyclerView.setLayoutManager(layoutManager);
        selectedImagesAdapter = new SelectedImagesAdapter(this, this);
        recyclerView.setAdapter(selectedImagesAdapter);

        // Disable nested scrolling on RecyclerView
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupImageHelpers() {
        imageCaptureHelper = new ImageCaptureHelper(
                this,
                null,
                imageUris -> {
                    if (imageUris != null && !imageUris.isEmpty()) {
                        // Clear existing images if we already have one
                        if (selectedImagesAdapter.getItemCount() > 0) {
                            // Remove all existing images one by one
                            while (selectedImagesAdapter.getItemCount() > 0) {
                                selectedImagesAdapter.removeImage(0);
                            }
                        }

                        // Only add the first image
                        List<Uri> urisToAdd = new ArrayList<>();
                        urisToAdd.add(imageUris.get(0));

                        selectedImagesAdapter.addImages(urisToAdd);
                        recyclerView.post(() ->
                                recyclerView.smoothScrollToPosition(0)
                        );

                        // Show the photo container only when images are added
                        selectedPhotoContainer.setVisibility(View.VISIBLE);

                        updateProgressText(selectedImagesAdapter.getItemCount());
                        updateUploadButtonState();
                    }
                }
        );
        imageUploadHelper = new ImageUploadHelper(this);
    }

    private void showMaxLimitWarning(int skippedCount) {
        new AlertDialog.Builder(this)
                .setTitle("Maximum Limit Reached")
                .setMessage("Only one photo can be uploaded at a time.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void setupClickListeners() {
        findViewById(R.id.upload_placeholder).setOnClickListener(v -> imageCaptureHelper.checkPermissionsAndOpenCamera());
        findViewById(R.id.gallery_button).setOnClickListener(v -> imageCaptureHelper.openGallery());
        uploadButton.setOnClickListener(v -> handleImageUpload());
    }

    private void updateProgressText(int currentCount) {
        String progressFormat;
        if (currentCount < REQUIRED_PHOTO_COUNT) {
            progressFormat = "No photo selected";
            // Hide the photo container when no images
            selectedPhotoContainer.setVisibility(View.GONE);
        } else {
            progressFormat = "Photo selected (Ready to upload)";
            // Show the photo container when images exist
            selectedPhotoContainer.setVisibility(View.VISIBLE);
        }
        progressText.setText(progressFormat);
    }

    @SuppressLint("SetTextI18n")
    private void updateUploadButtonState() {
        boolean isEnabled = selectedImagesAdapter.getItemCount() >= REQUIRED_PHOTO_COUNT;
        uploadButton.setEnabled(isEnabled);

        if (isEnabled) {
            uploadButton.setText("Upload Photo");
        } else {
            uploadButton.setText("Upload Photo");
        }
    }

    @Override
    public void onImageRemove(int position) {
        selectedImagesAdapter.removeImage(position);
        updateProgressText(selectedImagesAdapter.getItemCount());
        updateUploadButtonState();

        // Hide container if no images left
        if (selectedImagesAdapter.getItemCount() == 0) {
            selectedPhotoContainer.setVisibility(View.GONE);
        }
    }

    private void handleImageUpload() {
        if (selectedImagesAdapter.getItemCount() < REQUIRED_PHOTO_COUNT) {
            return;
        }

        showUploadDialog();
        uploadImagesSequentially();
    }

    private void showUploadDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_progress, null);
        uploadProgress = dialogView.findViewById(R.id.upload_progress);
        TextView uploadCountText = dialogView.findViewById(R.id.upload_count);

        int totalPhotos = selectedImagesAdapter.getItemCount();
        uploadProgress.setMax(totalPhotos);

        uploadDialog = new AlertDialog.Builder(this)
                .setTitle("Uploading Photo")
                .setView(dialogView)
                .setCancelable(false)
                .create();

        uploadDialog.show();

        uploadCountText.setText("Uploading photo...");
    }

    private void uploadImagesSequentially() {
        List<Uri> imageUris = selectedImagesAdapter.getImageUris();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger currentIndex = new AtomicInteger(0);

        uploadNextImage(imageUris, successCount, currentIndex);
    }

    private void uploadNextImage(List<Uri> imageUris, AtomicInteger successCount, AtomicInteger currentIndex) {
        if (currentIndex.get() >= imageUris.size()) {
            handleUploadCompletion(successCount.get());
            return;
        }

        Uri imageUri = imageUris.get(currentIndex.get());
        imageUploadHelper.uploadImage(imageUri, null, new ImageUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(String message) {
                successCount.incrementAndGet();
                uploadProgress.setProgress(currentIndex.get() + 1);

                TextView uploadCountText = uploadDialog.findViewById(R.id.upload_count);
                if (uploadCountText != null) {
                    uploadCountText.setText("Upload complete");
                }

                currentIndex.incrementAndGet();
                uploadNextImage(imageUris, successCount, currentIndex);
            }

            @Override
            public void onError(String error) {
                currentIndex.incrementAndGet();
                uploadNextImage(imageUris, successCount, currentIndex);
            }
        });
    }

    private void handleUploadCompletion(int successCount) {
        if (uploadDialog != null && uploadDialog.isShowing()) {
            uploadDialog.dismiss();
        }

        String message = "Photo upload complete";
        new AlertDialog.Builder(this)
                .setTitle("Upload Complete")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (successCount == selectedImagesAdapter.getItemCount()) {
                        finish();
                    }
                })
                .show();
    }

    @Override
    public void onNetworkChanged(boolean isConnected) {
        if (isConnected) {
            // When network comes back, verify server configuration
            ServerCheckUtility.checkServerConfigured(this);
        } else {
            // Show a message to the user about the network disconnection
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "Network connection lost. Upload may not work.",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uploadDialog != null && uploadDialog.isShowing()) {
            uploadDialog.dismiss();
        }

        // Clean up network receiver
        try {
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(networkChangeReceiver);
        } catch (NoClassDefFoundError e) {
            // Manually unregister if we used the fallback method
            if (networkChangeReceiver != null) {
                networkChangeReceiver.unregister();
            }
        }

        // The ImageCaptureHelper doesn't have a cleanup method, but we can set it to null
        // to help with garbage collection
        imageCaptureHelper = null;

        // Clean up any resources from ImageUploadHelper
        if (imageUploadHelper != null) {
            // If there's no cancelPendingUploads method, we just null the reference
            imageUploadHelper = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // The ActivityResultLauncher approach used in ImageCaptureHelper makes this not needed,
        // but we'll keep it for backward compatibility if needed
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // The permissionLauncher used in ImageCaptureHelper handles this automatically,
        // so we don't need to forward anything
    }

    @Override
    public void onBackPressed() {
        // Check if there are uploaded images and confirm before exiting
        if (selectedImagesAdapter != null && selectedImagesAdapter.getItemCount() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Discard Photo")
                    .setMessage("Are you sure you want to exit? Your selected photo will be discarded.")
                    .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}