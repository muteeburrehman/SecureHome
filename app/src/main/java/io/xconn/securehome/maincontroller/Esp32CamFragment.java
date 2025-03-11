package io.xconn.securehome.maincontroller;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.xconn.securehome.R;
import io.xconn.securehome.activities.SettingsActivity;

public class Esp32CamFragment extends Fragment {

    private static final String TAG = "Esp32CamFragment";
    private static final int REQUEST_WRITE_STORAGE = 112;

    private Button connectButton, cameraButton, settingsButton;
    private Switch switchCam1, switchCam2;
    private ImageView cameraView;

    private String currentCamUrl = "";
    private boolean isStreaming = false;
    private StreamTask streamTask;

    public Esp32CamFragment() {
        // Required empty public constructor
    }

    public static Esp32CamFragment newInstance() {
        return new Esp32CamFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_esp32_cam, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        connectButton = view.findViewById(R.id.connectButton);
        cameraButton = view.findViewById(R.id.cameraButton);
        settingsButton = view.findViewById(R.id.settingsButton);
        switchCam1 = view.findViewById(R.id.switchCam1);
        switchCam2 = view.findViewById(R.id.switchCam2);
        cameraView = view.findViewById(R.id.cameraView);

        // Request storage permissions for saving photos
        requestStoragePermission();

        // Setup button click listeners
        setupButtonListeners();

        // Setup switch listeners
        setupSwitchListeners();
    }

    private void setupButtonListeners() {
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStreaming) {
                    startStreaming();
                } else {
                    stopStreaming();
                }
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupSwitchListeners() {
        switchCam1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    switchCam2.setChecked(false);
                    loadCameraUrl(1);
                    if (isStreaming) {
                        stopStreaming();
                        startStreaming();
                    }
                }
            }
        });

        switchCam2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    switchCam1.setChecked(false);
                    loadCameraUrl(2);
                    if (isStreaming) {
                        stopStreaming();
                        startStreaming();
                    }
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

    private void startStreaming() {
        if (currentCamUrl.isEmpty()) {
            // If no camera is selected, default to camera 1
            switchCam1.setChecked(true);
            loadCameraUrl(1);

            if (currentCamUrl.isEmpty()) {
                Toast.makeText(getActivity(), "Please set camera URL in settings",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        streamTask = new StreamTask();
        streamTask.execute(currentCamUrl);
        isStreaming = true;
        connectButton.setText("Disconnect");
        Toast.makeText(getActivity(), "Connecting to: " + currentCamUrl, Toast.LENGTH_SHORT).show();
    }

    private void stopStreaming() {
        if (streamTask != null) {
            streamTask.cancel(true);
        }
        isStreaming = false;
        connectButton.setText("Connect");
    }

    private void capturePhoto() {
        if (!isStreaming) {
            Toast.makeText(getActivity(), "Please connect to camera first", Toast.LENGTH_SHORT).show();
            return;
        }

        new CapturePhotoTask().execute(currentCamUrl);
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
        if (switchCam1.isChecked()) {
            loadCameraUrl(1);
        } else if (switchCam2.isChecked()) {
            loadCameraUrl(2);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopStreaming();
    }

    private class StreamTask extends AsyncTask<String, Bitmap, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String streamUrl = params[0];

            try {
                URL url = new URL(streamUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream inputStream = connection.getInputStream();

                while (!isCancelled()) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            publishProgress(bitmap);
                        }
                        // Slight delay to prevent overwhelming the UI
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding stream: " + e.getMessage());
                        break;
                    }
                }

                inputStream.close();
                connection.disconnect();

            } catch (IOException e) {
                Log.e(TAG, "Error connecting to stream: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(),
                                    "Failed to connect to camera. Check URL and network.",
                                    Toast.LENGTH_LONG).show();
                            stopStreaming();
                        }
                    });
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            cameraView.setImageBitmap(values[0]);
        }
    }

    private class CapturePhotoTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            String streamUrl = params[0];

            try {
                URL url = new URL(streamUrl + "/capture");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                inputStream.close();
                connection.disconnect();

                return bitmap;

            } catch (IOException e) {
                Log.e(TAG, "Error capturing photo: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && getActivity() != null) {
                saveImageToStorage(bitmap);
            } else if (getActivity() != null) {
                Toast.makeText(getActivity(),
                        "Failed to capture photo",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToStorage(Bitmap bitmap) {
        if (getActivity() == null) return;

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getActivity(), "Storage permission required to save photos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "ESP32CAM_" + timeStamp + ".jpg";

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SecureHome");

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File imageFile = new File(storageDir, imageFileName);

        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Toast.makeText(getActivity(),
                    "Photo saved to: " + imageFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            Toast.makeText(getActivity(), "Failed to save photo", Toast.LENGTH_SHORT).show();
        }
    }
}