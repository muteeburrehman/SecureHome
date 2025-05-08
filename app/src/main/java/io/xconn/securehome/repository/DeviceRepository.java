package io.xconn.securehome.repository;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.xconn.securehome.api.ApiService;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.api.request.DeviceStatusUpdateRequest;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.utils.Esp32EndpointManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceRepository {
    private static final String TAG = "DeviceRepository";
    private final ApiService apiService;
    private final ApiService esp32ApiService; // Separate API service for ESP32
    private final Context context;
    private final MutableLiveData<List<Device>> devicesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Device> currentDeviceLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> statusUpdateMessageLiveData = new MutableLiveData<>();

    public DeviceRepository(Context context) {
        this.context = context;
        RetrofitClient retrofitClient = RetrofitClient.getInstance(context);
        this.apiService = retrofitClient.getApi();
        this.esp32ApiService = retrofitClient.getEsp32Api(); // Get the ESP32-specific API service
        devicesLiveData.setValue(new ArrayList<>());
        isLoadingLiveData.setValue(false);
    }

    public LiveData<List<Device>> getDevices() {
        return devicesLiveData;
    }

    public LiveData<Device> getCurrentDevice() {
        return currentDeviceLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public LiveData<String> getStatusUpdateMessage() {
        return statusUpdateMessageLiveData;
    }

    public void setCurrentDevice(Device device) {
        currentDeviceLiveData.setValue(device);
    }

    public void fetchDevices(int homeId) {
        isLoadingLiveData.setValue(true);

        apiService.getDevices(homeId).enqueue(new Callback<List<Device>>() {
            @Override
            public void onResponse(Call<List<Device>> call, Response<List<Device>> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    devicesLiveData.setValue(response.body());
                    Log.d(TAG, "Devices fetched successfully: " + response.body().size());
                } else {
                    errorMessageLiveData.setValue("Failed to fetch devices: " + response.message());
                    Log.e(TAG, "Error fetching devices: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Device>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error when fetching devices", t);
            }
        });
    }

    public void addDevice(int homeId, Device device, OnDeviceAddedListener listener) {
        isLoadingLiveData.setValue(true);

        // First, get the existing devices to determine the new device's endpoint index
        apiService.getDevices(homeId).enqueue(new Callback<List<Device>>() {
            @Override
            public void onResponse(Call<List<Device>> call, Response<List<Device>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Device> existingDevices = response.body();

                    // Assign the next available endpoint index (or default to 0 if empty)
                    int newEndpointIndex = existingDevices.size();

                    // Make sure we don't exceed our maximum number of endpoints
                    if (newEndpointIndex >= 23) {
                        isLoadingLiveData.setValue(false);
                        String errorMessage = "Maximum number of devices reached (23). Cannot add more devices with unique endpoints.";
                        errorMessageLiveData.setValue(errorMessage);
                        listener.onError(errorMessage);
                        return;
                    }

                    // Set the endpoint index for the new device
                    device.setEndpointIndex(newEndpointIndex);

                    Log.d(TAG, "Creating new device with endpoint index: " + newEndpointIndex);
                    String onEndpoint = Esp32EndpointManager.getOnEndpoint(newEndpointIndex);
                    String offEndpoint = Esp32EndpointManager.getOffEndpoint(newEndpointIndex);
                    Log.d(TAG, "This device will use endpoints: " + onEndpoint + " / " + offEndpoint);

                    // Now create the device with the endpoint index
                    createDeviceWithEndpointIndex(homeId, device, listener);
                } else {
                    isLoadingLiveData.setValue(false);
                    String errorMessage = "Failed to fetch existing devices: " + response.message();
                    errorMessageLiveData.setValue(errorMessage);
                    listener.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<List<Device>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                String errorMessage = "Network error: " + t.getMessage();
                errorMessageLiveData.setValue(errorMessage);
                listener.onError(errorMessage);
            }
        });
    }

    private void createDeviceWithEndpointIndex(int homeId, Device device, OnDeviceAddedListener listener) {
        apiService.createDevice(homeId, device).enqueue(new Callback<Device>() {
            @Override
            public void onResponse(Call<Device> call, Response<Device> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    Device newDevice = response.body();

                    // Ensure the endpoint index is preserved from our local device object
                    // This is crucial if the server doesn't return the endpoint_index field
                    if (newDevice.getEndpointIndex() <= 0) {
                        newDevice.setEndpointIndex(device.getEndpointIndex());
                    }

                    List<Device> currentDevices = devicesLiveData.getValue();
                    if (currentDevices != null) {
                        currentDevices.add(newDevice);
                        devicesLiveData.setValue(currentDevices);
                    }
                    currentDeviceLiveData.setValue(newDevice);
                    listener.onDeviceAdded(newDevice);
                    Log.d(TAG, "Device added successfully: " + newDevice.getName() +
                            " with endpoint index: " + newDevice.getEndpointIndex());
                } else {
                    String errorMessage = "Failed to add device: " +
                            (response.errorBody() != null ? response.errorBody().toString() : response.message());
                    errorMessageLiveData.setValue(errorMessage);
                    listener.onError(errorMessage);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<Device> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                String errorMessage = "Network error: " + t.getMessage();
                errorMessageLiveData.setValue(errorMessage);
                listener.onError(errorMessage);
                Log.e(TAG, "Network error when adding device", t);
            }
        });
    }

    public void updateDeviceStatus(int homeId, int deviceId, boolean status, OnStatusUpdateListener listener) {
        isLoadingLiveData.setValue(true);

        // Find the device to get its endpoint index
        Device targetDevice = null;
        List<Device> devices = devicesLiveData.getValue();
        if (devices != null) {
            for (Device device : devices) {
                if (device.getId() == deviceId) {
                    targetDevice = device;
                    break;
                }
            }
        }

        if (targetDevice == null) {
            isLoadingLiveData.setValue(false);
            String errorMessage = "Device not found";
            errorMessageLiveData.setValue(errorMessage);
            listener.onError(errorMessage);
            return;
        }

        // Store device reference for use in callbacks
        final Device finalTargetDevice = targetDevice;

        // First, make the request to update the device status in the backend
        DeviceStatusUpdateRequest request = new DeviceStatusUpdateRequest(status);
        apiService.updateDeviceStatus(homeId, deviceId, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Update the device in the list
                    List<Device> currentDevices = devicesLiveData.getValue();
                    if (currentDevices != null) {
                        for (Device device : currentDevices) {
                            if (device.getId() == deviceId) {
                                device.setStatus(status);
                                break;
                            }
                        }
                        devicesLiveData.setValue(currentDevices);
                    }

                    int endpointIndex = finalTargetDevice.getEndpointIndex();
                    Log.d(TAG, "Using endpoint index: " + endpointIndex + " for device: " + finalTargetDevice.getName());

                    // Now, send the corresponding ESP32 request based on the device's endpoint index
                    sendEsp32Request(endpointIndex, status, listener);
                } else {
                    isLoadingLiveData.setValue(false);
                    String errorMessage = "Failed to update device status: " + response.message();
                    errorMessageLiveData.setValue(errorMessage);
                    listener.onError(errorMessage);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                String errorMessage = "Network error: " + t.getMessage();
                errorMessageLiveData.setValue(errorMessage);
                listener.onError(errorMessage);
                Log.e(TAG, "Network error when updating device status", t);
            }
        });
    }

    private void sendEsp32Request(int endpointIndex, boolean status, OnStatusUpdateListener listener) {
        // Determine the endpoint based on the status and device's endpoint index
        String endpoint = status ?
                Esp32EndpointManager.getOnEndpoint(endpointIndex) :
                Esp32EndpointManager.getOffEndpoint(endpointIndex);

        Log.d(TAG, "Using ESP32 endpoint: " + endpoint + " for device with endpoint index: " + endpointIndex);

        // Make the request to the ESP32 endpoint using the ESP32-specific API service
        Call<ResponseBody> call = esp32ApiService.callEsp32Endpoint(endpoint);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful()) {
                    String statusMessage = "Device " + (status ? "turned ON" : "turned OFF") + " successfully";
                    statusUpdateMessageLiveData.setValue(statusMessage);
                    listener.onStatusUpdated(statusMessage);
                    Log.d(TAG, statusMessage + " (ESP32 notified using endpoint: " + endpoint + ")");
                } else {
                    String errorMessage = "Device status changed but failed to notify ESP32: " + response.message();
                    errorMessageLiveData.setValue(errorMessage);
                    listener.onError(errorMessage);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                String errorMessage = "Device status changed but failed to connect to ESP32: " + t.getMessage();
                errorMessageLiveData.setValue(errorMessage);
                listener.onError(errorMessage);
                Log.e(TAG, "Network error when connecting to ESP32", t);
            }
        });
    }

    public interface OnDeviceAddedListener {
        void onDeviceAdded(Device device);
        void onError(String message);
    }

    public interface OnStatusUpdateListener {
        void onStatusUpdated(String message);
        void onError(String message);
    }
}