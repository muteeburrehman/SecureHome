// ImageCaptureHelper.java
package io.xconn.securehome.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageCaptureHelper {
    private final AppCompatActivity activity;
    private final ImageView imageView;
    private Uri imageUri;
    private String currentPhotoPath;
    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher;
    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher;
    private final ActivityResultLauncher<String[]> permissionLauncher;
    private ImageCaptureCallback callback;

    public interface ImageCaptureCallback {
        void onImageCaptured(Uri imageUri);
    }

    public ImageCaptureHelper(AppCompatActivity activity, ImageView imageView) {
        this(activity, imageView, null);
    }

    public ImageCaptureHelper(AppCompatActivity activity, ImageView imageView, ImageCaptureCallback callback) {
        this.activity = activity;
        this.imageView = imageView;
        this.callback = callback;

        galleryActivityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imageView != null) {
                            imageView.setImageURI(imageUri);
                        }
                        if (callback != null) {
                            callback.onImageCaptured(imageUri);
                        }
                    }
                });

        cameraActivityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        try {
                            if (imageView != null) {
                                processAndDisplayCapturedImage();
                            }
                            if (callback != null) {
                                callback.onImageCaptured(imageUri);
                            }
                        } catch (IOException e) {
                            showToast("Error processing camera image");
                        }
                    }
                });

        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = permissions.values().stream().allMatch(Boolean::booleanValue);
                    if (allGranted) {
                        openCamera();
                    } else {
                        showToast("Permissions are required to use the camera");
                    }
                });
    }

    public void setImageCaptureCallback(ImageCaptureCallback callback) {
        this.callback = callback;
    }

    public void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryActivityResultLauncher.launch(galleryIntent);
    }

    public void checkPermissionsAndOpenCamera() {
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
            openCamera();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SecureHome");

        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + storageDir.getAbsolutePath());
        }

        File imageFile = new File(storageDir, imageFileName + ".jpg");
        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            imageUri = FileProvider.getUriForFile(activity,
                    activity.getApplicationContext().getPackageName() + ".provider",
                    photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraActivityResultLauncher.launch(cameraIntent);
        } catch (IOException e) {
            showToast("Error creating image file");
        }
    }

    private void processAndDisplayCapturedImage() throws IOException {
        if (imageView == null) return;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = calculateInSampleSize(bmOptions, imageView.getWidth(), imageView.getHeight());

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        if (bitmap == null) {
            showToast("Error loading image");
            return;
        }

        ExifInterface exif = new ExifInterface(currentPhotoPath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Bitmap rotatedBitmap = rotateBitmap(bitmap, orientation);

        imageView.setImageBitmap(rotatedBitmap);
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        float angle;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                angle = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                angle = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                angle = 270;
                break;
            default:
                return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    public Uri handleActivityResult(int requestCode, int resultCode, Intent data) {
        return imageUri;
    }
}