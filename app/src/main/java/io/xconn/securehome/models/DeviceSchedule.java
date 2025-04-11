package io.xconn.securehome.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeviceSchedule {
    @SerializedName("device_id")
    private int deviceId;

    @SerializedName("schedules")
    private List<Schedule> schedules;

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }
}