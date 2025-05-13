package io.xconn.securehome.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class for security alerts
 */
public class AlertModel {
    private String alertId;
    private String title;
    private String description;
    private String userId;
    private String userName;
    private long timestamp;
    private String type;
    private String priority;
    private boolean resolved;

    // Constants for alert types
    public static final String TYPE_EMERGENCY = "emergency";
    public static final String TYPE_SECURITY = "security";
    public static final String TYPE_SYSTEM = "system";

    // Constants for alert priorities
    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_HIGH = "high";

    // Default constructor required for Firestore
    public AlertModel() {
    }

    public AlertModel(String alertId, String title, String description, String userId,
                      String userName, long timestamp, String type, String priority, boolean resolved) {
        this.alertId = alertId;
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.userName = userName;
        this.timestamp = timestamp;
        this.type = type;
        this.priority = priority;
        this.resolved = resolved;
    }

    // Convert AlertModel to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("alertId", alertId);
        map.put("title", title);
        map.put("description", description);
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("timestamp", timestamp);
        map.put("type", type);
        map.put("priority", priority);
        map.put("resolved", resolved);
        return map;
    }

    // Getters and setters

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    // Format timestamp to readable date string
    public String getFormattedTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    // Get color for priority
    public int getPriorityColor() {
        switch (priority) {
            case PRIORITY_HIGH:
                return android.graphics.Color.parseColor("#F44336"); // Red
            case PRIORITY_MEDIUM:
                return android.graphics.Color.parseColor("#FF9800"); // Orange
            case PRIORITY_LOW:
                return android.graphics.Color.parseColor("#4CAF50"); // Green
            default:
                return android.graphics.Color.parseColor("#2196F3"); // Blue
        }
    }

    // Get icon resource for alert type
    public int getTypeIconResource() {
        switch (type) {
            case TYPE_EMERGENCY:
                return android.R.drawable.ic_dialog_alert; // Replace with your own icons
            case TYPE_SECURITY:
                return android.R.drawable.ic_lock_idle_lock;
            case TYPE_SYSTEM:
                return android.R.drawable.ic_dialog_info;
            default:
                return android.R.drawable.ic_dialog_info;
        }
    }
}