package io.xconn.securehome.utils;

import android.content.Context;
import android.util.Log;

import io.xconn.securehome.models.Device;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class DeviceControlUtility {
    private static final String TAG = "DeviceControlUtility";
    private static final String ESP32_BASE_URL = "http://esp32.local";

    /**
     * Controls a device by sending a command to the ESP32
     * @param context Application context
     * @param device Device to control
     * @param turnOn Whether to turn the device on or off
     * @param callback Callback for operation result
     */
    public static void controlDevice(Context context, Device device, boolean turnOn, DeviceControlCallback callback) {
        // Log the control attempt
        Log.d(TAG, "Controlling device: " + device.getName() +
                " (ID: " + device.getId() +
                ", Index: " + device.getEndpointIndex() +
                ", Action: " + (turnOn ? "ON" : "OFF") + ")");

        // Use device ID to determine the correct endpoint if endpointIndex is not set
        String endpoint;
        if (device.getEndpointIndex() > 0) {
            // Use the provided endpoint index
            endpoint = "/D" + device.getEndpointIndex() + (turnOn ? "ON" : "OFF");
        } else {
            // Map device ID to the appropriate endpoint (D1, D2, etc.)
            endpoint = "/D" + device.getId() + (turnOn ? "ON" : "OFF");
        }

        // Log the ESP32 endpoint being used
        Log.d(TAG, "Using ESP32 endpoint: " + endpoint);

        // Create the full URL
        String url = ESP32_BASE_URL + endpoint;

        // Execute the network request
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "ESP32 control failed for device: " + device.getName(), e);
                if (callback != null) {
                    callback.onError("Communication failed: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    Log.d(TAG, "ESP32 control successful for device: " + device.getName());
                    Log.d(TAG, "ESP32 response: " + responseBody);

                    // Verify response matches expected operation
                    boolean responseMatchesAction = verifyResponse(responseBody, device, turnOn);

                    if (responseMatchesAction) {
                        if (callback != null) {
                            callback.onSuccess("Device " + device.getName() + " is now " +
                                    (turnOn ? "ON" : "OFF"));
                        }
                    } else {
                        Log.w(TAG, "ESP32 response doesn't match expected action. Response: " + responseBody);
                        // Still consider it successful but log the mismatch
                        if (callback != null) {
                            callback.onSuccess("Command sent to " + device.getName() +
                                    ", but response was unexpected: " + responseBody);
                        }
                    }
                } else {
                    Log.e(TAG, "ESP32 control failed with code: " + response.code() + ", response: " + responseBody);
                    if (callback != null) {
                        callback.onError("Control failed: HTTP " + response.code());
                    }
                }
            }
        });
    }

    /**
     * Verify that the ESP32 response matches the expected action
     */
    private static boolean verifyResponse(String response, Device device, boolean turnOn) {
        response = response.toLowerCase().trim();

        // Expected responses based on device name since there's no type field
        String deviceName = device.getName().toLowerCase();

        if (deviceName.contains("light") || deviceName.contains("lamp") || deviceName.contains("bulb")) {
            return turnOn ?
                    response.contains("light on") || response.contains("turned on") :
                    response.contains("light off") || response.contains("turned off");
        } else if (deviceName.contains("door") || deviceName.contains("lock") || deviceName.contains("gate")) {
            return turnOn ?
                    response.contains("door open") || response.contains("unlocked") :
                    response.contains("door closed") || response.contains("locked");
        } else {
            // Generic device check
            return turnOn ?
                    response.contains("on") || response.contains("enabled") :
                    response.contains("off") || response.contains("disabled");
        }
    }

    /**
     * Interface for callback
     */
    public interface DeviceControlCallback {
        void onSuccess(String message);
        void onError(String errorMsg);
    }
}