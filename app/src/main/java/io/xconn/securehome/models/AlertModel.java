package io.xconn.securehome.models;

import java.util.Date;

/**
 * Model class for Security Alerts
 */
public class AlertModel {

    // Alert type constants
    public static final String TYPE_SECURITY = "security";
    public static final String TYPE_FIRE_GAS = "fire_gas";
    public static final String TYPE_SYSTEM = "system";

    private String id;
    private String type;
    private String title;
    private String description;
    private String imageUrl;
    private long timestamp;
    private boolean read;
    private boolean dismissed;
    private String userId;

    // Empty constructor for Firebase
    public AlertModel() {
    }

    public AlertModel(String id, String type, String title, String description, long timestamp) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.read = false;
        this.dismissed = false;
    }

    // With additional parameters for full initialization
    public AlertModel(String id, String type, String title, String description, String imageUrl,
                      long timestamp, boolean read, boolean dismissed, String userId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.read = read;
        this.dismissed = dismissed;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Get formatted time ago from alert timestamp
     */
    public String getTimeAgo() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - timestamp;

        // Convert to seconds
        long seconds = timeDiff / 1000;

        if (seconds < 60) {
            return "Just now";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            long days = seconds / 86400;
            return days + (days == 1 ? " day ago" : " days ago");
        }
    }

    /**
     * Get formatted type name for display
     */
    public String getTypeDisplayName() {
        switch (type) {
            case TYPE_SECURITY:
                return "Security Alert";
            case TYPE_FIRE_GAS:
                return "Fire/Gas Alert";
            case TYPE_SYSTEM:
                return "System Alert";
            default:
                return "Alert";
        }
    }

    /**
     * Get resource ID for icon based on alert type
     */
    public int getIconResourceId() {
        switch (type) {
            case TYPE_SECURITY:
                return io.xconn.securehome.R.drawable.alerts;
            case TYPE_FIRE_GAS:
                return io.xconn.securehome.R.mipmap.danger_alert;
            case TYPE_SYSTEM:
                return io.xconn.securehome.R.drawable.electricity;
            default:
                return io.xconn.securehome.R.drawable.ic_notification;
        }
    }
}