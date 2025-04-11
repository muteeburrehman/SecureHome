// app/src/main/java/io/xconn/securehome/models/Device.java
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

    public Device(String name, boolean status, int homeId) {
        this.name = name;
        this.status = status;
        this.homeId = homeId;
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
}