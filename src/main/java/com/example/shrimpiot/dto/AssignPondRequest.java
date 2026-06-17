package com.example.shrimpiot.dto;

public class AssignPondRequest {
    private String username;
    private Long pondId;
    private String accessType; // optional

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getPondId() { return pondId; }
    public void setPondId(Long pondId) { this.pondId = pondId; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
}
