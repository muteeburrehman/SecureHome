package io.xconn.securehome.models;

import androidx.annotation.DrawableRes;
import io.xconn.securehome.R;

public class Device {
    // Device Info
    private String id;
    private String name;
    private DeviceType type;
    private boolean isOn;

    // Connection Info
    private String ipAddress;
    private int port;
    private String username;
    private String password;
    private boolean isConnected;

    // Schedule Info
    private String startTime;
    private String endTime;

    // Constructors
    public Device(String name, DeviceType type) {
        this.id = generateUniqueId();
        this.name = name;
        this.type = type;
        this.isOn = false;
        this.isConnected = false;
        this.startTime = "Not set";
        this.endTime = "Not set";
    }

    public Device(String name, DeviceType type, String ipAddress, int port,
                  String username, String password) {
        this(name, type);
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    // Helper methods
    @DrawableRes
    public int getIconResource() {
        switch (type) {
            case LIGHT:
                return R.drawable.ic_lightbulb;
            case OUTLET:
                return R.drawable.ic_outlets;
            case THERMOSTAT:
                return R.drawable.ic_thermostat;
            case CAMERA:
                return R.drawable.ic_camera;
            default:
                return R.drawable.ic_home;
        }
    }

    public String getStatusText() {
        if (!isConnected) {
            return "Disconnected";
        }
        return isOn ? "On" : "Off";
    }

    private String generateUniqueId() {
        return "device_" + System.currentTimeMillis() + "_" + Math.random() * 1000;
    }

    // Device Type Enum
    public enum DeviceType {
        LIGHT("Light"),
        OUTLET("Outlet"),
        THERMOSTAT("Thermostat"),
        CAMERA("Camera"),
        OTHER("Other");

        private final String displayName;

        DeviceType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static DeviceType fromDisplayName(String displayName) {
            for (DeviceType type : values()) {
                if (type.getDisplayName().equals(displayName)) {
                    return type;
                }
            }
            return OTHER;
        }
    }
}