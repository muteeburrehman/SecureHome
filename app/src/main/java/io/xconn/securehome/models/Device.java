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

    @SerializedName("type")
    private String type;  // Device type (light, outlet, hvac, etc.)

    @SerializedName("home_id")
    private int homeId;

    @SerializedName("endpoint_index")
    private int endpointIndex;

    // Default constructor
    public Device() {
        this.endpointIndex = -1;
        this.type = "outlet";  // Default type
    }

    // Constructor with name and status
    public Device(String name, boolean status, int homeId) {
        this.name = name;
        this.status = status;
        this.homeId = homeId;
        this.endpointIndex = -1;
        this.type = "outlet";  // Default type
    }

    // Constructor for energy monitoring
    public Device(int id, String name, String type, int homeId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.homeId = homeId;
        this.status = false;
        this.endpointIndex = -1;
    }

    // Constructor with all fields
    public Device(int id, String name, boolean status, String type, int homeId, int endpointIndex) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.type = type;
        this.homeId = homeId;
        this.endpointIndex = endpointIndex;
    }

    // Legacy constructor without type
    public Device(int id, String name, boolean status, int homeId, int endpointIndex) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.homeId = homeId;
        this.endpointIndex = endpointIndex;
        this.type = "outlet";  // Default type
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", type='" + type + '\'' +
                ", homeId=" + homeId +
                ", endpointIndex=" + endpointIndex +
                '}';
    }
}