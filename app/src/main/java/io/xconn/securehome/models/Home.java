package io.xconn.securehome.models;

import com.google.gson.annotations.SerializedName;

public class Home {
    @SerializedName("id")
    private int id;

    @SerializedName("owner")
    private String owner;

    @SerializedName("ip_address")
    private String ipAddress;

    @SerializedName("port")
    private int port;

    public Home(String owner, String ipAddress, int port) {
        this.owner = owner;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}