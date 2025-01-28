package io.xconn.securehome.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import io.xconn.securehome.R;
import io.xconn.securehome.adapters.SelectedImagesAdapter;
import io.xconn.securehome.utils.ImageCaptureHelper;
import io.xconn.securehome.utils.ImageUploadHelper;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class RegisterActivity extends AppCompatActivity implements SelectedImagesAdapter.OnImageRemoveListener {
    private static final int REQUIRED_PHOTO_COUNT = 10;

    private ImageCaptureHelper imageCaptureHelper;
    private ImageUploadHelper imageUploadHelper;
    private SelectedImagesAdapter selectedImagesAdapter;
    private TextView progressText;
    private ExtendedFloatingActionButton uploadButton;
    private AlertDialog uploadDialog;
    private LinearProgressIndicator uploadProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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
        RecyclerView recyclerView = findViewById(R.id.recycler_selected_images);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        selectedImagesAdapter = new SelectedImagesAdapter(this, this);
        recyclerView.setAdapter(selectedImagesAdapter);
    }

    private void setupImageHelpers() {
        imageCaptureHelper = new ImageCaptureHelper(
                this,
                null,
                imageUris -> {
                    if (imageUris != null && !imageUris.isEmpty()) {
                        for (Uri uri : imageUris) {
                            selectedImagesAdapter.addImage(uri);
                        }
                        updateProgressText(selectedImagesAdapter.getItemCount());
                        uploadButton.setEnabled(selectedImagesAdapter.getItemCount() >= REQUIRED_PHOTO_COUNT);
                    }
                }
        );
        imageUploadHelper = new ImageUploadHelper(this);
    }

    private void setupClickListeners() {
        findViewById(R.id.card_gallery).setOnClickListener(v -> imageCaptureHelper.openGallery());
        findViewById(R.id.card_camera).setOnClickListener(v -> imageCaptureHelper.checkPermissionsAndOpenCamera());
        uploadButton.setOnClickListener(v -> handleImageUpload());
    }

    private void updateProgressText(int currentCount) {
        progressText.setText(String.format(Locale.US, "Selected Photos: %d/%d", currentCount, REQUIRED_PHOTO_COUNT));
    }

    @Override
    public void onImageRemove(int position) {
        selectedImagesAdapter.removeImage(position);
        updateProgressText(selectedImagesAdapter.getItemCount());
        uploadButton.setEnabled(selectedImagesAdapter.getItemCount() >= REQUIRED_PHOTO_COUNT);
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
        uploadProgress.setMax(selectedImagesAdapter.getItemCount());

        uploadDialog = new AlertDialog.Builder(this)
                .setTitle("Uploading Photos")
                .setView(dialogView)
                .setCancelable(false)
                .create();

        uploadDialog.show();
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

        String message = String.format(Locale.US, "Successfully uploaded %d/%d photos",
                successCount, selectedImagesAdapter.getItemCount());
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