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
import io.xconn.securehome.api.response.RecognitionResponse;
import androidx.annotation.NonNull;

public class FaceRecognitionHelper {
    private final Context context;

    public FaceRecognitionHelper(Context context) {
        this.context = context;
    }

    public void recognizeFace(Uri imageUri, String photoPath, RecognitionCallback callback) {
        try {
            File imageFile;
            if (photoPath != null) {
                imageFile = new File(photoPath);
            } else {
                imageFile = createTempFileFromUri(imageUri);
            }

            String mimeType = getMimeType(imageUri);
            // Read file bytes directly and create request body
            RequestBody requestFile = RequestBody.Companion.create(imageFile, mimeType != null
                    ?
                    MediaType.parse(mimeType) : MediaType.parse("image/*"));


            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image",
                    imageFile.getName(),
                    requestFile
            );

            RetrofitClient.getInstance(context)
                    .getApi()
                    .recognizeFace(imagePart)
                    .enqueue(new Callback<RecognitionResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<RecognitionResponse> call, @NonNull Response<RecognitionResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                callback.onSuccess(response.body());
                            } else {
                                callback.onError("Recognition failed: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<RecognitionResponse> call, @NonNull Throwable t) {
                            callback.onError("Recognition failed: " + t.getMessage());
                        }
                    });

        } catch (IOException e) {
            callback.onError("Error preparing image: " + e.getMessage());
        }
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Failed to open input stream from Uri: " + uri);
        }

        File tempFile = File.createTempFile("recognize_", ".jpg", context.getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } finally {
            inputStream.close();
        }

        return tempFile;
    }

    private String getMimeType(Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return "image/*"; // Default mime type
    }

    public interface RecognitionCallback {
        void onSuccess(RecognitionResponse response);
        void onError(String error);
    }
}
