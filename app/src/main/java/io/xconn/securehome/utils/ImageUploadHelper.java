package io.xconn.securehome.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import io.xconn.securehome.api.RetrofitClient;

import androidx.annotation.NonNull;

public class ImageUploadHelper {
    private static final String TAG = "ImageUploadHelper";
    private static final int MAX_IMAGE_DIMENSION = 1920; // Maximum width or height
    private static final int COMPRESSION_QUALITY = 85; // JPEG compression quality
    private final Context context;

    public ImageUploadHelper(Context context) {
        this.context = context;
    }

    public void uploadImage(Uri imageUri, String photoPath, UploadCallback callback) {
        try {
            File imageFile;
            if (photoPath != null) {
                // Camera photo
                imageFile = optimizeImage(photoPath, null);
            } else {
                // Gallery photo
                imageFile = optimizeImage(null, imageUri);
            }

            String mimeType = "image/jpeg"; // We're always converting to JPEG

            RequestBody requestFile = RequestBody.create(
                    imageFile,
                    MediaType.parse(mimeType)
            );

            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "file",
                    imageFile.getName(),
                    requestFile
            );

            RetrofitClient.getInstance()
                    .getApi()
                    .upload(imagePart)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            // Attempt to delete the temporary file
                            if (!imageFile.delete()) {
                                Log.e(TAG, "Failed to delete temporary file: " + imageFile.getAbsolutePath());
                            }

                            if (response.isSuccessful()) {
                                callback.onSuccess("Image uploaded successfully");
                            } else {
                                String errorBody = "Unknown error";
                                if (response.errorBody() != null) {
                                    try {
                                        errorBody = response.errorBody().string();
                                    } catch (IOException e) {
                                        Log.e(TAG, "Error reading error body", e);
                                    }
                                }
                                callback.onError("Upload failed: " + errorBody);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            // Attempt to delete the temporary file
                            if (!imageFile.delete()) {
                                Log.e(TAG, "Failed to delete temporary file: " + imageFile.getAbsolutePath());
                            }
                            callback.onError("Upload failed: " + t.getMessage());
                        }
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error preparing image for upload", e);
            callback.onError("Error preparing image: " + e.getMessage());
        }
    }

    private File optimizeImage(String photoPath, Uri imageUri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Get image dimensions
        if (photoPath != null) {
            BitmapFactory.decodeFile(photoPath, options);
        } else {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();
        }

        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;

        // Decode bitmap with inSampleSize
        Bitmap bitmap;
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;

        if (photoPath != null) {
            // Handle camera photo
            bitmap = BitmapFactory.decodeFile(photoPath, options);
            ExifInterface exif = new ExifInterface(photoPath);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        } else {
            // Handle gallery photo
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            // Get orientation for gallery image
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                inputStream.close();
            }
        }

        // Rotate bitmap if needed
        bitmap = rotateBitmapIfNeeded(bitmap, orientation);

        // Create output file
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        File outputFile = new File(context.getCacheDir(), fileName);

        // Save optimized image
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out);
            out.flush();
        } finally {
            bitmap.recycle();
        }

        return outputFile;
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, int orientation) {
        if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError while rotating bitmap", e);
            return bitmap;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > ImageUploadHelper.MAX_IMAGE_DIMENSION || width > ImageUploadHelper.MAX_IMAGE_DIMENSION) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= ImageUploadHelper.MAX_IMAGE_DIMENSION && (halfWidth / inSampleSize) >= ImageUploadHelper.MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public interface UploadCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}