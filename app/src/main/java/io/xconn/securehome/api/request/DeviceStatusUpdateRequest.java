package io.xconn.securehome.api.request;

import com.google.gson.annotations.SerializedName;

/**
 * Request class for updating device status
 */
public class DeviceStatusUpdateRequest {
    @SerializedName("status")
    private boolean status;

    public DeviceStatusUpdateRequest(boolean status) {
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}