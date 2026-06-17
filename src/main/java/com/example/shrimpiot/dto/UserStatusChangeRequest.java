package com.example.shrimpiot.dto;

public class UserStatusChangeRequest {

    private String reason;

    public UserStatusChangeRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
