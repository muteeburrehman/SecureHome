package io.xconn.securehome.models;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("status")
    private boolean status;

    @SerializedName("home_id")
    private int homeId;

    @SerializedName("has_schedules")
    private boolean hasSchedules;

    public Device(String name, boolean status, int homeId) {
        this.name = name;
        this.status = status;
        this.homeId = homeId;
        this.hasSchedules = false; // Default value
    }

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

    public boolean hasSchedules() {
        return hasSchedules;
    }

    public void setHasSchedules(boolean hasSchedules) {
        this.hasSchedules = hasSchedules;
    }
}