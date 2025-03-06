package io.xconn.securehome.maincontroller;

public class Device {
    private String name;
    private boolean isOn;
    private String startTime; // Start time for scheduling
    private String endTime;   // End time for scheduling

    public Device(String name, boolean isOn) {
        this.name = name;
        this.isOn = isOn;
        this.startTime = "Not set"; // Default value
        this.endTime = "Not set";   // Default value
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
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
}