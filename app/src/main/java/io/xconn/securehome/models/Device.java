package io.xconn.securehome.models;

import com.google.gson.annotations.SerializedName;
import android.util.Log;

public class Device {
    private static final String TAG = "Device";

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("status")
    private boolean status;

    @SerializedName("home_id")
    private int homeId;

    @SerializedName("endpoint_index")
    private int endpointIndex; // New field for endpoint index

    // Default constructor
    public Device() {
        this.endpointIndex = -1;
    }

    // Constructor with name and status
    public Device(String name, boolean status, int homeId) {
        this.name = name;
        this.status = status;
        this.homeId = homeId;
        this.endpointIndex = -1; // Default to -1, will be set properly when device is added
    }

    // Constructor with all fields
    public Device(int id, String name, boolean status, int homeId, int endpointIndex) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.homeId = homeId;
        this.endpointIndex = endpointIndex;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public int getHomeId() {
        return homeId;
    }

    public void setHomeId(int homeId) {
        this.homeId = homeId;
    }

    public int getEndpointIndex() {
        return endpointIndex;
    }

    public void setEndpointIndex(int endpointIndex) {
        Log.d(TAG, "Setting endpoint index for device " + name + " to: " + endpointIndex);
        this.endpointIndex = endpointIndex;
    }

    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", homeId=" + homeId +
                ", endpointIndex=" + endpointIndex +
                '}';
    }
}