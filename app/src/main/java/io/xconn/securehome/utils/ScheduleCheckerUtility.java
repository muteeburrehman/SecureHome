package io.xconn.securehome.utils;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.xconn.securehome.api.ApiService;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.models.DeviceSchedule;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleCheckerUtility {
    private static final String TAG = "ScheduleCheckerUtility";

    public interface ScheduleCheckListener {
        void onAllDevicesChecked(Map<Integer, Boolean> deviceScheduleMap);
        void onError(String message);
    }

    public static void checkDevicesWithSchedules(Context context, int homeId, List<Device> devices, ScheduleCheckListener listener) {
        if (devices == null || devices.isEmpty()) {
            listener.onAllDevicesChecked(new HashMap<>());
            return;
        }

        ApiService apiService = RetrofitClient.getInstance(context).getApi();
        Map<Integer, Boolean> deviceScheduleMap = new HashMap<>();
        final int[] checkedDevices = {0};

        for (Device device : devices) {
            deviceScheduleMap.put(device.getId(), false);

            apiService.getDeviceSchedules(homeId, device.getId()).enqueue(new Callback<DeviceSchedule>() {
                @Override
                public void onResponse(Call<DeviceSchedule> call, Response<DeviceSchedule> response) {
                    checkedDevices[0]++;

                    if (response.isSuccessful() && response.body() != null &&
                            response.body().getSchedules() != null &&
                            !response.body().getSchedules().isEmpty()) {

                        deviceScheduleMap.put(device.getId(), true);
                    }

                    // Check if this is the last device
                    if (checkedDevices[0] == devices.size()) {
                        listener.onAllDevicesChecked(deviceScheduleMap);
                    }
                }

                @Override
                public void onFailure(Call<DeviceSchedule> call, Throwable t) {
                    checkedDevices[0]++;
                    Log.e(TAG, "Failed to check schedules for device: " + device.getId(), t);

                    // Check if this is the last device
                    if (checkedDevices[0] == devices.size()) {
                        listener.onAllDevicesChecked(deviceScheduleMap);
                    }
                }
            });
        }
    }
}