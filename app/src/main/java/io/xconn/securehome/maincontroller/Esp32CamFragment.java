package io.xconn.securehome.maincontroller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.xconn.securehome.R;
import io.xconn.securehome.activities.SettingsActivity;
import io.xconn.securehome.adapters.CaptureAdapter;
import io.xconn.securehome.models.CaptureModel;

public class Esp32CamFragment extends Fragment {

    private static final String TAG = "Esp32CamFragment";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int CONNECTION_TIMEOUT = 10000;  // 10 seconds
    private static final int READ_TIMEOUT = 15000;        // 15 seconds
    private static final String[] POSSIBLE_STREAM_PATHS = {
            "/", "/capture", "/video", "/mjpg", "/cam", "/camera","/stream"
    };

    private MaterialButton connectButton, cameraButton, settingsButton, viewAllButton;
    private Chip chipCam1, chipCam2;
    private ImageView cameraView;
    private TextView cameraStatusText, cam1Status, cam2Status;
    private LinearProgressIndicator connectionProgress;
    private FloatingActionButton captureFloatingButton;
    private RecyclerView recentCapturesRecyclerView;
    private CaptureAdapter captureAdapter;

    private String currentCamUrl = "";
    private boolean isStreaming = false;
    private StreamTask streamTask;
    private int currentStreamPathIndex = 0;

    public Esp32CamFragment() {
        // Required empty public constructor
    }

    public static Esp32CamFragment newInstance() {
        return new Esp32CamFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_esp32_cam, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        requestStoragePermission();
        setupButtonListeners();
        setupChipListeners();
        loadRecentCaptures();
        updateCameraStatus();
    }

    private void initializeViews(View view) {
        connectButton = view.findViewById(R.id.connectButton);
        cameraButton = view.findViewById(R.id.cameraButton);
        settingsButton = view.findViewById(R.id.settingsButton);
        viewAllButton = view.findViewById(R.id.viewAllButton);
        chipCam1 = view.findViewById(R.id.chipCam1);
        chipCam2 = view.findViewById(R.id.chipCam2);
        cameraView = view.findViewById(R.id.cameraView);
        cameraStatusText = view.findViewById(R.id.cameraStatusText);
        connectionProgress = view.findViewById(R.id.connectionProgress);
        captureFloatingButton = view.findViewById(R.id.captureFloatingButton);
        cam1Status = view.findViewById(R.id.cam1Status);
        cam2Status = view.findViewById(R.id.cam2Status);
        recentCapturesRecyclerView = view.findViewById(R.id.recentCapturesRecyclerView);

        // Set up RecyclerView
        recentCapturesRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        captureAdapter = new CaptureAdapter();
        recentCapturesRecyclerView.setAdapter(captureAdapter);
    }

    private void setupButtonListeners() {
        connectButton.setOnClickListener(v -> {
            if (!isStreaming) {
                startStreaming();
            } else {
                stopStreaming();
            }
        });

        cameraButton.setOnClickListener(v -> capturePhoto());

        captureFloatingButton.setOnClickListener(v -> capturePhoto());

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        viewAllButton.setOnClickListener(v ->
                Toast.makeText(getActivity(), "View all captures - feature coming soon",
                        Toast.LENGTH_SHORT).show());
    }

    private void setupChipListeners() {
        chipCam1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipCam2.setChecked(false);
                loadCameraUrl(1);
                if (isStreaming) {
                    stopStreaming();
                    startStreaming();
                }
            }
        });

        chipCam2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipCam1.setChecked(false);
                loadCameraUrl(2);
                if (isStreaming) {
                    stopStreaming();
                    startStreaming();
                }
            }
        });
    }

    private void loadCameraUrl(int cameraNumber) {
        if (getActivity() == null) return;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String key = "camera" + cameraNumber + "_url";
        currentCamUrl = sharedPreferences.getString(key, "");

        if (currentCamUrl.isEmpty()) {
            Toast.makeText(getActivity(), "Camera " + cameraNumber + " URL not set. Please check settings.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        if (getActivity() == null) return false;

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private void startStreaming() {
        if (!isNetworkAvailable()) {
            diagnoseNetworkConnection();
            Toast.makeText(getActivity(), "No network connection available", Toast.LENGTH_LONG).show();
            return;
        }

        if (currentCamUrl.isEmpty()) {
            chipCam1.setChecked(true);
            loadCameraUrl(1);

            if (currentCamUrl.isEmpty()) {
                Toast.makeText(getActivity(), "Please set camera URL in settings", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Reset stream path index
        currentStreamPathIndex = 0;
        attemptStreamConnection();
    }

    private void attemptStreamConnection() {
        // Additional safety checks
        if (getActivity() == null) return;

        if (currentCamUrl.isEmpty()) {
            Toast.makeText(getActivity(),
                    "Camera URL is not configured. Please check settings.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Validate URL format
        if (!currentCamUrl.startsWith("http://") && !currentCamUrl.startsWith("https://")) {
            currentCamUrl = "http://" + currentCamUrl;
        }

        // Try different stream paths with full URL validation
        if (currentStreamPathIndex < POSSIBLE_STREAM_PATHS.length) {
            String testUrl = currentCamUrl.replaceAll("/$", "") +
                    POSSIBLE_STREAM_PATHS[currentStreamPathIndex];

            Log.d(TAG, "Attempting connection with URL: " + testUrl);

            // Show progress indicator
            connectionProgress.setVisibility(View.VISIBLE);

            streamTask = new StreamTask();
            streamTask.execute(testUrl);

            isStreaming = true;
            updateConnectionUI(true);
        } else {
            // All stream paths failed
            Toast.makeText(getActivity(),
                    "Could not connect to camera. Check URL and network settings.",
                    Toast.LENGTH_LONG).show();
            stopStreaming();
        }
    }

    private void diagnoseNetworkConnection() {
        if (getActivity() == null) return;

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            Toast.makeText(getActivity(),
                    "Unable to access connectivity manager",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            Toast.makeText(getActivity(),
                    "No active network connection",
                    Toast.LENGTH_LONG).show();
            return;
        }

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);

        if (capabilities == null) {
            Toast.makeText(getActivity(),
                    "Network capabilities could not be determined",
                    Toast.LENGTH_LONG).show();
            return;
        }

        StringBuilder diagnosticInfo = new StringBuilder("Network Diagnostics:\n");

        diagnosticInfo.append("WiFi Connected: ")
                .append(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                .append("\n");

        diagnosticInfo.append("Cellular Connected: ")
                .append(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .append("\n");

        diagnosticInfo.append("Internet Available: ")
                .append(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .append("\n");

        // Optional: Log the diagnostic information
        Log.d(TAG, diagnosticInfo.toString());

        // Display as toast or in a dialog
        Toast.makeText(getActivity(), diagnosticInfo.toString(), Toast.LENGTH_LONG).show();
    }
    private void updateConnectionUI(boolean connecting) {
        if (getActivity() == null) return;

        connectButton.setText(connecting ? "Disconnect" : "Connect");
        connectButton.setIcon(ContextCompat.getDrawable(getActivity(),
                connecting ? R.drawable.ic_disconnect : R.drawable.ic_connect));

        connectButton.setBackgroundTintList(ContextCompat.getColorStateList(getActivity(),
                connecting ? android.R.color.holo_red_light : R.color.material_blue));

        cameraStatusText.setText(connecting ?
                "Connecting to camera..." : "Live View - Disconnected");
    }

    private void stopStreaming() {
        if (streamTask != null) {
            streamTask.cancel(true);
        }
        isStreaming = false;
        updateConnectionUI(false);
        connectionProgress.setVisibility(View.GONE);
    }

    private void capturePhoto() {
        if (!isStreaming) {
            Toast.makeText(getActivity(), "Please connect to camera first", Toast.LENGTH_SHORT).show();
            return;
        }

        new CapturePhotoTask().execute(currentCamUrl);
    }

    private void updateCameraStatus() {
        if (getActivity() == null) return;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String cam1Url = sharedPreferences.getString("camera1_url", "");
        if (!cam1Url.isEmpty()) {
            cam1Status.setText("Ready");
            cam1Status.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.holo_green_dark));
        } else {
            cam1Status.setText("Not Configured");
            cam1Status.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.darker_gray));
        }

        String cam2Url = sharedPreferences.getString("camera2_url", "");
        if (!cam2Url.isEmpty()) {
            cam2Status.setText("Ready");
            cam2Status.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.holo_green_dark));
        } else {
            cam2Status.setText("Not Configured");
            cam2Status.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.darker_gray));
        }
    }

    private void loadRecentCaptures() {
        if (getActivity() == null) return;

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SecureHome");

        if (!storageDir.exists()) return;

        // Get the most recent captures (max 10)
        File[] files = storageDir.listFiles((dir, name) -> name.startsWith("ESP32CAM_") && name.endsWith(".jpg"));

        if (files == null || files.length == 0) return;

        // Sort by date (newest first)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        List<CaptureModel> captures = new ArrayList<>();
        int maxItems = Math.min(files.length, 10);

        for (int i = 0; i < maxItems; i++) {
            File file = files[i];
            String timestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new Date(file.lastModified()));
            captures.add(new CaptureModel(file.getAbsolutePath(), timestamp));
        }

        captureAdapter.setCaptures(captures);
    }

    private void requestStoragePermission() {
        if (getActivity() != null && ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Storage permission granted", Toast.LENGTH_SHORT).show();
                // Load captures after permission is granted
                loadRecentCaptures();
            } else {
                Toast.makeText(getActivity(),
                        "Storage permission denied. You won't be able to save photos.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if camera URLs have changed in settings
        if (chipCam1.isChecked()) {
            loadCameraUrl(1);
        } else if (chipCam2.isChecked()) {
            loadCameraUrl(2);
        }

        // Update camera status
        updateCameraStatus();

        // Refresh recent captures
        loadRecentCaptures();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopStreaming();
    }

    private class StreamTask extends AsyncTask<String, Bitmap, Boolean> {
        private String attemptedUrl;
        private String errorMessage = "";

        @Override
        protected Boolean doInBackground(String... params) {
            attemptedUrl = params[0];

            try {
                URL url = new URL(attemptedUrl);
                HttpURLConnection connection = null;

                try {
                    connection = (HttpURLConnection) url.openConnection();

                    // Enhanced connection configuration
                    connection.setConnectTimeout(CONNECTION_TIMEOUT);
                    connection.setReadTimeout(READ_TIMEOUT);
                    connection.setDoInput(true);
                    connection.setRequestMethod("GET");

                    // More comprehensive User-Agent
                    connection.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Linux; Android) SecureHome Camera App");

                    // Additional headers to improve compatibility
                    connection.setRequestProperty("Accept", "image/jpeg,image/png,image/*;q=0.8,*/*;q=0.5");
                    connection.setRequestProperty("Accept-Encoding", "identity");
                    connection.setInstanceFollowRedirects(true);

                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "Connection URL: " + attemptedUrl + ", Response Code: " + responseCode);

                    // Handle redirect responses
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                        String redirectUrl = connection.getHeaderField("Location");
                        if (redirectUrl != null) {
                            Log.d(TAG, "Redirected to: " + redirectUrl);
                            // Optionally update the URL
                            url = new URL(redirectUrl);
                            connection = (HttpURLConnection) url.openConnection();
                            responseCode = connection.getResponseCode();
                        }
                    }

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        errorMessage = "Server returned non-OK status: " + responseCode;
                        Log.w(TAG, errorMessage);
                        return false;
                    }

                    String contentType = connection.getContentType();
                    Log.d(TAG, "Content Type: " + contentType);

                    // More flexible content type checking
                    if (contentType == null ||
                            (!contentType.contains("image") &&
                                    !contentType.contains("stream") &&
                                    !contentType.contains("multipart"))) {
                        errorMessage = "Unexpected content type: " + contentType;
                        Log.w(TAG, errorMessage);
                        return false;
                    }

                    InputStream inputStream = connection.getInputStream();

                    // Hide progress indicator after first frame
                    publishProgress(null);

                    byte[] buffer = new byte[4096];
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

                    while (!isCancelled()) {
                        try {
                            int bytesRead = inputStream.read(buffer);
                            if (bytesRead == -1) break;

                            byteBuffer.write(buffer, 0, bytesRead);
                            byte[] imageBytes = byteBuffer.toByteArray();

                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                            if (bitmap != null) {
                                publishProgress(bitmap);

                                // Reset buffer
                                byteBuffer.reset();

                                // Slight delay to prevent overwhelming the UI
                                TimeUnit.MILLISECONDS.sleep(100);
                            } else {
                                Log.w(TAG, "Decoded bitmap is null");
                                break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Stream decoding error", e);
                            errorMessage = "Stream decoding failed: " + e.getMessage();
                            break;
                        }
                    }

                    inputStream.close();
                    byteBuffer.close();

                    return true;

                } catch (IOException e) {
                    errorMessage = "Connection error: " + e.getMessage();
                    Log.e(TAG, errorMessage, e);
                    return false;
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

            } catch (Exception e) {
                errorMessage = "Unexpected error: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                // Provide more detailed error feedback
                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            "Connection failed: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                }

                // Try next stream path
                currentStreamPathIndex++;
                attemptStreamConnection();
            } else {
                // Successfully connected
                connectionProgress.setVisibility(View.GONE);
                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            "Connected to camera: " + attemptedUrl,
                            Toast.LENGTH_SHORT).show();
                }
            }
            if (!isNetworkAvailable()) {
                diagnoseNetworkConnection();
            }
        }
    }


    private class CapturePhotoTask extends AsyncTask<String, Void, File> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Capturing photo...", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected File doInBackground(String... params) {
            String streamUrl = params[0];
            File photoFile = null;

            try {
                URL url = new URL(streamUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap != null) {
                    // Create directory if it doesn't exist
                    File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "SecureHome");
                    if (!storageDir.exists()) {
                        storageDir.mkdirs();
                    }

                    // Create filename
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    photoFile = new File(storageDir, "ESP32CAM_" + timeStamp + ".jpg");

                    // Save the image
                    FileOutputStream fos = new FileOutputStream(photoFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.close();

                    // Make the photo available in the gallery
                    if (getActivity() != null) {
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(android.net.Uri.fromFile(photoFile));
                        getActivity().sendBroadcast(mediaScanIntent);
                    }
                }

                inputStream.close();
                connection.disconnect();

            } catch (IOException e) {
                Log.e(TAG, "Error capturing photo: " + e.getMessage());
                return null;
            }

            return photoFile;
        }

        @Override
        protected void onPostExecute(File photoFile) {
            if (getActivity() == null) return;

            if (photoFile != null && photoFile.exists()) {
                Toast.makeText(getActivity(), "Photo saved: " + photoFile.getName(),
                        Toast.LENGTH_SHORT).show();
                // Refresh the recent captures
                loadRecentCaptures();
            } else {
                Toast.makeText(getActivity(), "Failed to save photo", Toast.LENGTH_SHORT).show();
            }
        }
    }
}