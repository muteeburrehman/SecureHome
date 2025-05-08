package io.xconn.securehome.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import io.xconn.securehome.api.ApiService;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.api.request.DeviceStatusUpdateRequest;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.utils.DeviceControlUtility;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceRepository {
    private static final String TAG = "DeviceRepository";

    private final Context context;
    private final ApiService apiService;

    private final MutableLiveData<List<Device>> devices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> statusUpdateMessage = new MutableLiveData<>();

    public DeviceRepository(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApi();
    }

    public LiveData<List<Device>> getDevices() {
        return devices;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getStatusUpdateMessage() {
        return statusUpdateMessage;
    }

    /**
     * Fetch devices for a specific home from the API
     * @param homeId ID of the home
     */
    public void fetchDevices(int homeId) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        apiService.getDevices(homeId).enqueue(new Callback<List<Device>>() {
            @Override
            public void onResponse(Call<List<Device>> call, Response<List<Device>> response) {
                isLoading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    devices.postValue(response.body());
                    Log.d(TAG, "Fetched " + response.body().size() + " devices");

                    // Log device details for debugging
                    for (Device device : response.body()) {
                        Log.d(TAG, "Device: " + device.toString());
                    }
                } else {
                    errorMessage.postValue("Failed to fetch devices: " +
                            (response.code() == 404 ? "No devices found" :
                                    "Error " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Device>> call, Throwable t) {
                isLoading.postValue(false);
                errorMessage.postValue("Network error: " + t.getMessage());
                Log.e(TAG, "Error fetching devices", t);
            }
        });
    }

    /**
     * Add a new device to the home
     * @param homeId ID of the home
     * @param device Device to add
     * @param callback Callback for operation result
     */
    public void addDevice(int homeId, Device device, final OnOperationListener callback) {
        isLoading.setValue(true);

        apiService.createDevice(homeId, device).enqueue(new Callback<Device>() {
            @Override
            public void onResponse(Call<Device> call, Response<Device> response) {
                isLoading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    // Automatically refresh the device list
                    fetchDevices(homeId);

                    if (callback != null) {
                        callback.onSuccess("Device added successfully");
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Failed to add device: " +
                                (response.code() == 400 ? "Invalid data" : "Error " + response.code()));
                    }
                }
            }

            @Override
            public void onFailure(Call<Device> call, Throwable t) {
                isLoading.postValue(false);

                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }

                Log.e(TAG, "Error adding device", t);
            }
        });
    }

    /**
     * Update device status both in the backend and control the physical device via ESP32
     * @param homeId ID of the home
     * @param deviceId ID of the device
     * @param newStatus New status to set
     * @param listener Listener for status update result
     */
    public void updateDeviceStatus(int homeId, int deviceId, boolean newStatus,
                                   final OnStatusUpdateListener listener) {
        // First find the device from our list to get its endpoint index
        List<Device> deviceList = devices.getValue();
        Device targetDevice = null;

        if (deviceList != null) {
            for (Device device : deviceList) {
                if (device.getId() == deviceId) {
                    targetDevice = device;
                    break;
                }
            }
        }

        if (targetDevice == null) {
            if (listener != null) {
                listener.onError("Device not found in the list");
            }
            return;
        }

        // Store a final reference to targetDevice to use in the inner class
        final Device finalTargetDevice = targetDevice;

        // Log the operation
        Log.d(TAG, "Updating device status: " + finalTargetDevice.getName() +
                " (ID: " + deviceId + ", Index: " + finalTargetDevice.getEndpointIndex() +
                ") to " + (newStatus ? "ON" : "OFF"));

        // First control the physical device via ESP32
        DeviceControlUtility.controlDevice(context, finalTargetDevice, newStatus,
                new DeviceControlUtility.DeviceControlCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // After successful device control, update the backend
                        updateBackendDeviceStatus(homeId, deviceId, newStatus, finalTargetDevice, listener);
                    }

                    @Override
                    public void onError(String errorMsg) {
                        if (listener != null) {
                            listener.onError("ESP32 control failed: " + errorMsg);
                        }
                    }
                });
    }

    /**
     * Update device status in the backend after controlling the physical device
     */
    private void updateBackendDeviceStatus(int homeId, int deviceId, boolean newStatus,
                                           Device targetDevice, OnStatusUpdateListener listener) {
        DeviceStatusUpdateRequest request = new DeviceStatusUpdateRequest(newStatus);

        apiService.updateDeviceStatus(homeId, deviceId, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Update the local device list
                    updateLocalDeviceStatus(deviceId, newStatus);

                    String message = targetDevice.getName() + " is now " + (newStatus ? "ON" : "OFF");
                    statusUpdateMessage.postValue(message);

                    if (listener != null) {
                        listener.onStatusUpdated(message);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("Server update failed: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (listener != null) {
                    listener.onError("Network error: " + t.getMessage());
                }
                Log.e(TAG, "Error updating device status in backend", t);
            }
        });
    }

    /**
     * Update the local device list with the new status
     */
    private void updateLocalDeviceStatus(int deviceId, boolean newStatus) {
        List<Device> currentDevices = devices.getValue();
        if (currentDevices != null) {
            List<Device> updatedDevices = new ArrayList<>(currentDevices);

            for (int i = 0; i < updatedDevices.size(); i++) {
                Device device = updatedDevices.get(i);
                if (device.getId() == deviceId) {
                    device.setStatus(newStatus);
                    break;
                }
            }

            devices.postValue(updatedDevices);
        }
    }

    // Interfaces for callbacks
    public interface OnStatusUpdateListener {
        void onStatusUpdated(String message);
        void onError(String message);
    }

    public interface OnOperationListener {
        void onSuccess(String message);
        void onError(String message);
    }
}