package io.xconn.securehome.models;

public class DeviceEnergy {
    private int deviceId;
    private String deviceName;
    private String deviceType;
    private double wattage;      // Power in watts
    private double voltage;      // Voltage in volts
    private double current;      // Current in amperes
    private double units;        // Energy in kWh

    public DeviceEnergy(int deviceId, String deviceName, String deviceType,
                        double wattage, double voltage, double current, double units) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.wattage = wattage;
        this.voltage = voltage;
        this.current = current;
        this.units = units;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public double getWattage() {
        return wattage;
    }

    public void setWattage(double wattage) {
        this.wattage = wattage;
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getUnits() {
        return units;
    }

    public void setUnits(double units) {
        this.units = units;
    }
}