package io.xconn.securehome.models;

public class UserModel {
    private String userId;
    private String email;
    private String displayName;
    private String role;
    private String approvalStatus;
    private long createdAt;

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    // Default constructor required for Firebase
    public UserModel() {
    }

    public UserModel(String userId, String email, String displayName, String role, String approvalStatus, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.approvalStatus = approvalStatus;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(approvalStatus);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(approvalStatus);
    }
}