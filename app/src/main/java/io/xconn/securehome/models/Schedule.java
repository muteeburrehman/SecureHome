package io.xconn.securehome.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Schedule {
    @SerializedName("id")
    private int id;

    @SerializedName("time")
    private String time;

    @SerializedName("operation")
    private boolean isOn; // Renamed for clarity

    @SerializedName("days")
    private List<Integer> days;

    public Schedule(String time, boolean isOn, List<Integer> days) {
        this.time = time;
        this.isOn = isOn; // Constructor updated to use `isOn`
        this.days = days;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isOn() { // Renamed method
        return isOn;
    }

    public void setOn(boolean isOn) { // Renamed setter
        this.isOn = isOn;
    }

    public List<Integer> getDays() {
        return days;
    }

    public void setDays(List<Integer> days) {
        this.days = days;
    }
}
