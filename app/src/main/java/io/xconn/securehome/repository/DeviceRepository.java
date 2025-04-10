// app/src/main/java/io/xconn/securehome/repository/DeviceRepository.java
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
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceRepository {
    private static final String TAG = "DeviceRepository";
    private final ApiService apiService;
    private final Context context;
    private final MutableLiveData<List<Device>> devicesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Device> currentDeviceLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> statusUpdateMessageLiveData = new MutableLiveData<>();

    public DeviceRepository(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApi();
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

        apiService.createDevice(homeId, device).enqueue(new Callback<Device>() {
            @Override
            public void onResponse(Call<Device> call, Response<Device> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    Device newDevice = response.body();
                    List<Device> currentDevices = devicesLiveData.getValue();
                    if (currentDevices != null) {
                        currentDevices.add(newDevice);
                        devicesLiveData.setValue(currentDevices);
                    }
                    currentDeviceLiveData.setValue(newDevice);
                    listener.onDeviceAdded(newDevice);
                    Log.d(TAG, "Device added successfully: " + newDevice.getName());
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

        DeviceStatusUpdateRequest request = new DeviceStatusUpdateRequest(status);
        apiService.updateDeviceStatus(homeId, deviceId, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isLoadingLiveData.setValue(false);
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

                    String statusMessage = "Device " + (status ? "turned ON" : "turned OFF") + " successfully";
                    statusUpdateMessageLiveData.setValue(statusMessage);
                    listener.onStatusUpdated(statusMessage);
                    Log.d(TAG, statusMessage);
                } else {
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

    public interface OnDeviceAddedListener {
        void onDeviceAdded(Device device);
        void onError(String message);
    }

    public interface OnStatusUpdateListener {
        void onStatusUpdated(String message);
        void onError(String message);
    }
}