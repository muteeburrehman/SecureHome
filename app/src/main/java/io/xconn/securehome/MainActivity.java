package io.xconn.securehome;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private Uri imageUri;
    private String currentPhotoPath;

    // Gallery ActivityResultLauncher
    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    imageView.setImageURI(imageUri);
                }
            });

    // Camera ActivityResultLauncher
    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        processAndDisplayCapturedImage();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error processing camera image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Permission Launcher
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    allGranted = allGranted && isGranted;
                }
                if (allGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permissions required to use camera", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button galleryBtn = findViewById(R.id.button);
        Button cameraBtn = findViewById(R.id.button2);

        galleryBtn.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryActivityResultLauncher.launch(galleryIntent);
        });

        cameraBtn.setOnClickListener(v -> checkPermissionsAndOpenCamera());
    }

    private void checkPermissionsAndOpenCamera() {
        String[] permissions = {Manifest.permission.CAMERA};

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }

        boolean cameraPermissionDenied = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED;
        boolean storagePermissionDenied = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED;

        if (cameraPermissionDenied || storagePermissionDenied) {
            permissionLauncher.launch(permissions);
        } else {
            openCamera();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;

        File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SecureHome");
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
            imageUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraActivityResultLauncher.launch(cameraIntent);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndDisplayCapturedImage() throws IOException {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW / targetW, photoH / targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        if (bitmap == null) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read EXIF orientation
        ExifInterface ei = new ExifInterface(currentPhotoPath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateImage(bitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateImage(bitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateImage(bitmap, 270);
                break;
            default:
                rotatedBitmap = bitmap;
        }

        imageView.setImageBitmap(rotatedBitmap);

        // Cleanup
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}