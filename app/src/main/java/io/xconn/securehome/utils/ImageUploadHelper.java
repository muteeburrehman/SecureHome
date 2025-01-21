package io.xconn.securehome.utils;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;
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
    private final Context context;

    public ImageUploadHelper(Context context) {
        this.context = context;
    }

    public void uploadImage(Uri imageUri, String photoPath, UploadCallback callback) {
        try {
            File imageFile;
            if (photoPath != null) {
                // Use directly captured photo
                imageFile = new File(photoPath);
            } else {
                // Convert Uri to File for gallery images
                imageFile = createTempFileFromUri(imageUri);
            }

            String mimeType = getMimeType(imageUri);
            // Updated: Using RequestBody.Companion.create instead of deprecated RequestBody.create
            RequestBody requestFile = RequestBody.Companion.create(imageFile, mimeType != null ? MediaType.parse(mimeType) : MediaType.parse("image/*"));

            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image",
                    imageFile.getName(),
                    requestFile
            );

            RetrofitClient.getInstance()
                    .getApi()
                    .uploadImage(imagePart)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (response.isSuccessful()) {
                                callback.onSuccess("Image uploaded successfully");
                            } else {
                                callback.onError("Upload failed: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            callback.onError("Upload failed: " + t.getMessage());
                        }
                    });

        } catch (IOException e) {
            callback.onError("Error preparing image: " + e.getMessage());
        }
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        // Handle case if inputStream is null
        if (inputStream == null) {
            throw new IOException("Unable to open input stream for the URI: " + uri);
        }

        File tempFile = File.createTempFile("upload_", ".jpg", context.getCacheDir());

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;

            // Read the data from the input stream into the buffer and write it to the temp file
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } finally {
            // Ensure the inputStream is closed to avoid memory leaks
            inputStream.close();
        }

        return tempFile;
    }

    private String getMimeType(Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return null;
    }

    public interface UploadCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
