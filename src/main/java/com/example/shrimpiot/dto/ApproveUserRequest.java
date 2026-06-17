package com.example.shrimpiot.dto;

import java.util.List;

public class ApproveUserRequest {
    private String role;
    private List<Long> pondIds;
    private List<String> deviceIds;
    private String accessType;

    public String getRole() { return role; }
    public List<Long> getPondIds() { return pondIds; }
    public List<String> getDeviceIds() { return deviceIds; }
    public String getAccessType() { return accessType; }

    public void setRole(String role) { this.role = role; }
    public void setPondIds(List<Long> pondIds) { this.pondIds = pondIds; }
    public void setDeviceIds(List<String> deviceIds) { this.deviceIds = deviceIds; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
}
