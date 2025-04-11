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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.SelectedImagesAdapter;
import io.xconn.securehome.utils.ImageCaptureHelper;
import io.xconn.securehome.utils.ImageUploadHelper;
import io.xconn.securehome.utils.ServerCheckUtility;

public class RegisterActivity extends AppCompatActivity implements SelectedImagesAdapter.OnImageRemoveListener {
    private static final int REQUIRED_PHOTO_COUNT = 10;
    private static final int MAX_PHOTO_COUNT = 20;

    private ImageCaptureHelper imageCaptureHelper;
    private ImageUploadHelper imageUploadHelper;
    private SelectedImagesAdapter selectedImagesAdapter;
    private TextView progressText;
    private ExtendedFloatingActionButton uploadButton;
    private AlertDialog uploadDialog;
    private LinearProgressIndicator uploadProgress;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Check if server is configured first, before initializing anything else
        if (!ServerCheckUtility.checkServerConfigured(this)) {
            // Return early - the user will be redirected to ServerDiscoveryActivity
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupImageHelpers();
        setupClickListeners();
    }

    private void initializeViews() {
        progressText = findViewById(R.id.tv_progress);
        uploadButton = findViewById(R.id.fab_upload);
        uploadButton.setEnabled(false);
        updateProgressText(0);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_selected_images);

        // Set fixed size for better performance
        recyclerView.setHasFixedSize(true);

        // Increase view cache
        recyclerView.setItemViewCacheSize(20);

        // Configure layout manager with spacing
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(12);

        // Add item decoration for grid spacing
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);

                outRect.left = spacing;
                outRect.right = spacing;
                outRect.top = spacing;
                outRect.bottom = spacing;
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        selectedImagesAdapter = new SelectedImagesAdapter(this, this);
        recyclerView.setAdapter(selectedImagesAdapter);

        // Disable nested scrolling on RecyclerView
        recyclerView.setNestedScrollingEnabled(false);
    }

    // Rest of the code remains unchanged
    private void setupImageHelpers() {
        imageCaptureHelper = new ImageCaptureHelper(
                this,
                null,
                imageUris -> {
                    if (imageUris != null && !imageUris.isEmpty()) {
                        int remainingSlots = MAX_PHOTO_COUNT - selectedImagesAdapter.getItemCount();

                        // Batch add images instead of one by one
                        List<Uri> urisToAdd = new ArrayList<>();
                        for (int i = 0; i < Math.min(imageUris.size(), remainingSlots); i++) {
                            urisToAdd.add(imageUris.get(i));
                        }

                        // Add all images at once
                        if (!urisToAdd.isEmpty()) {
                            selectedImagesAdapter.addImages(urisToAdd);
                            // Use post to smooth out the scroll
                            recyclerView.post(() ->
                                    recyclerView.scrollToPosition(selectedImagesAdapter.getItemCount() - 1)
                            );
                        }

                        if (imageUris.size() > remainingSlots) {
                            showMaxLimitWarning(imageUris.size() - remainingSlots);
                        }

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
                .setMessage(String.format(Locale.US,
                        "Only added %d of your selected photos. Maximum limit is %d photos.",
                        Math.min(skippedCount, MAX_PHOTO_COUNT),
                        MAX_PHOTO_COUNT))
                .setPositiveButton("OK", null)
                .show();
    }

    private void setupClickListeners() {
        findViewById(R.id.card_gallery).setOnClickListener(v -> imageCaptureHelper.openGallery());
        findViewById(R.id.card_camera).setOnClickListener(v -> imageCaptureHelper.checkPermissionsAndOpenCamera());
        uploadButton.setOnClickListener(v -> handleImageUpload());
    }

    private void updateProgressText(int currentCount) {
        String progressFormat;
        if (currentCount < REQUIRED_PHOTO_COUNT) {
            progressFormat = "Selected Photos: %d (Minimum %d required)";
        } else {
            progressFormat = "Selected Photos: %d (Ready to upload)";
        }
        progressText.setText(String.format(Locale.US, progressFormat, currentCount, REQUIRED_PHOTO_COUNT));
    }

    @SuppressLint("SetTextI18n")
    private void updateUploadButtonState() {
        boolean isEnabled = selectedImagesAdapter.getItemCount() >= REQUIRED_PHOTO_COUNT;
        uploadButton.setEnabled(isEnabled);

        if (isEnabled) {
            uploadButton.setText(String.format(Locale.US, "Upload %d Photos",
                    selectedImagesAdapter.getItemCount()));
        } else {
            uploadButton.setText("Upload Photos");
        }
    }

    @Override
    public void onImageRemove(int position) {
        selectedImagesAdapter.removeImage(position);
        updateProgressText(selectedImagesAdapter.getItemCount());
        updateUploadButtonState();
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
                .setTitle("Uploading Photos")
                .setView(dialogView)
                .setCancelable(false)
                .create();

        uploadDialog.show();

        uploadCountText.setText(String.format(Locale.US, "Uploaded 0/%d photos", totalPhotos));
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
                    uploadCountText.setText(String.format(Locale.US, "Uploaded %d/%d photos",
                            currentIndex.get() + 1, imageUris.size()));
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

        String message = String.format(Locale.US, "Successfully uploaded %d photos", successCount);
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
    protected void onDestroy() {
        super.onDestroy();
        if (uploadDialog != null && uploadDialog.isShowing()) {
            uploadDialog.dismiss();
        }
    }
}