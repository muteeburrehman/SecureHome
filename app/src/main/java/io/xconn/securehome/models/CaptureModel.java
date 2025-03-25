package io.xconn.securehome.models;

public class CaptureModel {
    private String imagePath;
    private String timestamp;

    public CaptureModel(String imagePath, String timestamp) {
        this.imagePath = imagePath;
        this.timestamp = timestamp;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}