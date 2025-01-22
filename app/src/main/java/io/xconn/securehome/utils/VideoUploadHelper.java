package io.xconn.securehome.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
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

public class VideoUploadHelper {
    private static final String TAG = "VideoUploadHelper";
    private final Context context;

    public VideoUploadHelper(Context context) {
        this.context = context;
    }

    public void uploadVideo(Uri videoUri, String videoPath, UploadCallback callback) {
        try {
            File videoFile;
            if (videoPath != null) {
                videoFile = new File(videoPath);
            } else {
                videoFile = createTempFileFromUri(videoUri);
            }

            String mimeType = getMimeType(videoUri);
            if (mimeType == null) mimeType = "video/mp4";

            RequestBody requestFile = RequestBody.create(
                    videoFile,
                    MediaType.parse(mimeType)
            );

            MultipartBody.Part videoPart = MultipartBody.Part.createFormData(
                    "file",
                    videoFile.getName(),
                    requestFile
            );

            RetrofitClient.getInstance()
                    .getApi()
                    .upload(videoPart)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (videoPath == null) {
                                if (!videoFile.delete()) {
                                    Log.e(TAG, "Failed to delete temporary file");
                                }
                            }

                            if (response.isSuccessful()) {
                                callback.onSuccess("Video uploaded successfully");
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
                            if (videoPath == null) {
                                if (!videoFile.delete()) {
                                    Log.e(TAG, "Failed to delete temporary file");
                                }
                            }
                            callback.onError("Upload failed: " + t.getMessage());
                        }
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error preparing video for upload", e);
            callback.onError("Error preparing video: " + e.getMessage());
        }
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        String fileName = "VID_" + System.currentTimeMillis() + ".mp4";
        File tempFile = new File(context.getCacheDir(), fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream == null) throw new IOException("Failed to open input stream");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
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