package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.ApprovalStatus;
import com.example.shrimpiot.model.UserAccount;
import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String role;
    private boolean active;
    private String phone;
    private String email;
    private String approvalStatus;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(UserAccount user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.role = user.getRole().name();
        this.active = user.isActive();
        this.phone = user.getPhone();
        this.email = user.getEmail();
        ApprovalStatus status = user.getApprovalStatus();
        this.approvalStatus = status == null ? (user.isActive() ? "APPROVED" : "PENDING") : status.name();
        this.approvedBy = user.getApprovedBy();
        this.approvedAt = user.getApprovedAt();
        this.createdAt = user.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getApprovalStatus() { return approvalStatus; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
